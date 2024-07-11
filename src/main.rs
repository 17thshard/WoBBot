use std::time::Duration;

use arcanum::Arcanum;
use poise::futures_util::StreamExt;
use poise::{
    serenity_prelude::{self as serenity},
    CreateReply,
};

mod arcanum;
mod util;

use ::serenity::all::ButtonStyle;
use ::serenity::builder::{CreateActionRow, CreateButton};
use ::serenity::builder::{CreateInteractionResponse, CreateMessage};
use tokio::time::timeout;
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
pub async fn wob(_ctx: Context<'_>) -> Result<()> {
    Ok(())
}

#[poise::command(prefix_command, slash_command)]
pub async fn search(ctx: Context<'_>, terms: Vec<String>) -> Result<()> {
    let reply = ctx
        .send(
            CreateReply::default()
                .ephemeral(true)
                .content(format!("Searching for '{}' ...", terms.join(", "))),
        )
        .await?;

    let mut entries = ctx.data().arcanum.search_entries(terms).await?;
    let count = entries.count as usize;

    if entries.count == 0 {
        reply
            .edit(ctx, CreateReply::default().content("No results found!"))
            .await?;
        return Ok(());
    }

    let mut cur_idx = 0;

    let get_edit_components = |idx: usize, count: usize| {
        CreateReply::default().content("").components(vec![
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
                    .label(format!("{}/{}", idx + 1, count)),
                CreateButton::new("next")
                    .disabled(idx + 1 >= count)
                    .style(ButtonStyle::Secondary)
                    .label("Next"),
                CreateButton::new("next_10")
                    .disabled(idx + 10 >= count)
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

    reply
        .edit(
            ctx,
            get_edit_components(cur_idx, count).embed(
                ctx.data()
                    .arcanum
                    .embed_entry(&entries.get_entry(cur_idx).await?),
            ),
        )
        .await?;

    let mut interaction_stream = reply
        .message()
        .await?
        .await_component_interaction(ctx)
        .stream();

    loop {
        match timeout(Duration::from_secs(300), interaction_stream.next()).await {
            Ok(Some(interaction)) => {
                let kind = interaction.data.custom_id.as_str();

                match kind {
                    "next" | "back" | "next_10" | "back_10" => {
                        let delta = match kind {
                            "next" => 1,
                            "back" => -1,
                            "next_10" => 10,
                            "back_10" => -10,
                            &_ => unreachable!(),
                        };

                        cur_idx = cur_idx.checked_add_signed(delta).unwrap();

                        reply
                            .edit(
                                ctx,
                                get_edit_components(cur_idx, count).embed(
                                    ctx.data()
                                        .arcanum
                                        .embed_entry(&entries.get_entry(cur_idx).await?),
                                ),
                            )
                            .await?;

                        interaction
                            .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                            .await?;
                    }

                    "delete" => {
                        interaction
                            .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                            .await?;

                        reply.delete(ctx).await?;
                        break;
                    }
                    "done" => {
                        interaction
                            .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                            .await?;

                        reply.delete(ctx).await?;

                        ctx.channel_id()
                            .send_message(
                                ctx,
                                CreateMessage::default().embed(
                                    ctx.data()
                                        .arcanum
                                        .embed_entry(&entries.get_entry(cur_idx).await?),
                                ),
                            )
                            .await?;

                        break;
                    }
                    _ => {
                        println!("Unhandled interaction: {}", kind);
                    }
                }
            }
            _ => {
                reply.delete(ctx).await?;
                break;
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
