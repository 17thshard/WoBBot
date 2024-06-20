use std::time::Duration;

use arcanum::Arcanum;
use poise::futures_util::StreamExt;
use poise::{
    serenity_prelude::{self as serenity},
    ChoiceParameter, CreateReply,
};

mod arcanum;
mod util;

use ::serenity::all::ButtonStyle;
use ::serenity::builder::{
    CreateInteractionResponse, CreateInteractionResponseMessage, EditMessage,
};
use ::serenity::{
    all::ReactionType,
    builder::{CreateActionRow, CreateButton},
};
use util::Result;

struct Data {
    arcanum: Arcanum,
}

type Error = Box<dyn std::error::Error + Send + Sync>;
type Context<'a> = poise::Context<'a, Data, Error>;

/// Utility command for quickly registering/unregistering commands
#[poise::command(prefix_command)]
pub async fn register(ctx: Context<'_>) -> Result<()> {
    poise::builtins::register_application_commands_buttons(ctx).await?;
    Ok(())
}

#[poise::command(prefix_command, slash_command, subcommands("search", "random"))]
pub async fn wob(ctx: Context<'_>) -> Result<()> {
    Ok(())
}

#[poise::command(prefix_command, slash_command)]
pub async fn search(ctx: Context<'_>, terms: Vec<String>) -> Result<()> {
    println!("{terms:?}");

    let mut m = ctx
        .send(CreateReply::default().content(format!("Searching for '{}' ...", terms.join(", "))))
        .await?
        .into_message()
        .await?;

    let entries = ctx.data().arcanum.search_entries(&terms).await?;

    if entries.count == 0 {
        m.edit(&ctx, EditMessage::new().content("No results found!"))
            .await?;
        return Ok(());
    }

    let mut cur_idx = 0;

    let get_edit = |idx: usize| {
        EditMessage::new()
            .content("")
            .embed(ctx.data().arcanum.embed_entry(&entries.results[idx]))
            .components(vec![
                CreateActionRow::Buttons(vec![
                    CreateButton::new("back_10")
                        .disabled(idx < 10)
                        .style(ButtonStyle::Secondary)
                        .label("Back 10"),
                    CreateButton::new("back")
                        .disabled(idx == 0)
                        .style(ButtonStyle::Secondary)
                        .label("Back"),
                    CreateButton::new("info")
                        .style(ButtonStyle::Secondary)
                        .disabled(true)
                        .label(format!("{}/{}", idx + 1, entries.results.len())),
                    CreateButton::new("next")
                        .disabled(idx + 1 >= entries.results.len())
                        .style(ButtonStyle::Secondary)
                        .label("Next"),
                    CreateButton::new("next_10")
                        .disabled(idx + 10 >= entries.results.len())
                        .style(ButtonStyle::Secondary)
                        .label("Next 10"),
                ]),
                CreateActionRow::Buttons(vec![
                    CreateButton::new("done")
                        .style(ButtonStyle::Success)
                        .label("Done"),
                    CreateButton::new("delete")
                        .style(ButtonStyle::Danger)
                        .label("Delete"),
                ]),
            ])
    };

    m.edit(&ctx, get_edit(cur_idx)).await?;

    let inner_context: &serenity::Context = ctx.as_ref();

    let mut interaction_stream = m
        .await_component_interaction(&inner_context.shard)
        .timeout(Duration::from_secs(180))
        .stream();

    while let Some(interaction) = interaction_stream.next().await {
        let kind = &interaction.data.custom_id;

        match kind.as_str() {
            "next" => {
                cur_idx += 1;
                m.edit(&ctx, get_edit(cur_idx)).await?;

                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;
            }
            "back" => {
                cur_idx -= 1;
                m.edit(&ctx, get_edit(cur_idx)).await?;

                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;
            }
            "next_10" => {
                cur_idx += 10;
                m.edit(&ctx, get_edit(cur_idx)).await?;

                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;
            }
            "back_10" => {
                cur_idx -= 10;
                m.edit(&ctx, get_edit(cur_idx)).await?;

                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;
            }

            "delete" => {
                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;

                m.delete(&ctx).await?;
                break;
            }
            "done" => {
                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;

                m.edit(&ctx, EditMessage::new().components(vec![])).await?;
            }
            _ => {
                println!("Unhandled interaction: {}", kind);
            }
        }
    }

    Ok(())
}
#[poise::command(prefix_command, slash_command)]
pub async fn random(ctx: Context<'_>) -> Result<()> {
    let entry = ctx.data().arcanum.random_entry().await?;

    let reply = CreateReply::default()
        .embed(ctx.data().arcanum.embed_entry(&entry))
        .components(vec![CreateActionRow::Buttons(vec![CreateButton::new(
            "delete",
        )
        .style(ButtonStyle::Danger)
        .label("Delete")])]);

    let m = ctx.send(reply).await?.into_message().await?;

    let inner_context: &serenity::Context = ctx.as_ref();

    let mut interaction_stream = m
        .await_component_interaction(&inner_context.shard)
        .timeout(Duration::from_secs(180))
        .stream();

    while let Some(interaction) = interaction_stream.next().await {
        let kind = &interaction.data.custom_id;

        match kind.as_str() {
            "delete" => {
                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;

                m.delete(&ctx).await?;
                break;
            }
            _ => {
                println!("Unhandled interaction: {}", kind);
            }
        }
    }

    Ok(())
}

#[tokio::main]
async fn main() {
    let token = std::env::var("DISCORD_TOKEN").expect("missing DISCORD_TOKEN");
    let base_url = std::env::var("ARCANUM_URL").unwrap_or("https://wob.coppermind.net".into());
    let arcanum_icon = std::env::var("ARCANUM_ICON")
        .unwrap_or("https://cdn.discordapp.com/emojis/909180911269081179.png".into());
    let intents = serenity::GatewayIntents::non_privileged();

    let framework = poise::Framework::builder()
        .options(poise::FrameworkOptions {
            commands: vec![wob(), register()],
            ..Default::default()
        })
        .setup(|ctx, _ready, framework| {
            Box::pin(async move {
                poise::builtins::register_globally(ctx, &framework.options().commands).await?;
                Ok(Data {
                    arcanum: Arcanum::new(base_url, arcanum_icon),
                })
            })
        })
        .build();

    let client = serenity::ClientBuilder::new(token, intents)
        .framework(framework)
        .await;
    client.unwrap().start().await.unwrap();
}
