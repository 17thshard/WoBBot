use std::time::Duration;

use ::serenity::builder::{CreateInteractionResponse, CreateMessage};
use poise::futures_util::StreamExt;
use poise::Context;
use serenity::{
    all::ButtonStyle,
    builder::{CreateActionRow, CreateButton},
};

use poise::serenity_prelude::{self as serenity};

pub type Result<T> = std::result::Result<T, Box<dyn std::error::Error + Send + Sync>>;

// send a message with an interaction button to delete it
// it can be deleted by either an admin or the author taken from context
pub async fn send_deletable<U: Send + Sync, E>(
    ctx: &Context<'_, U, E>,
    msg: CreateMessage,
) -> Result<()> {
    let m = ctx
        .channel_id()
        .send_message(
            ctx,
            msg.components(vec![CreateActionRow::Buttons(vec![CreateButton::new(
                "delete",
            )
            .style(ButtonStyle::Danger)
            .label("Delete")])]),
        )
        .await?;

    let inner_context: &serenity::Context = ctx.as_ref();

    let mut interaction_stream = m
        .await_component_interaction(&inner_context.shard)
        .timeout(Duration::from_secs(180))
        .stream();

    while let Some(interaction) = interaction_stream.next().await {
        if interaction.user.id != ctx.author().id {
            let is_admin = match interaction.member {
                Some(ref member) => member.permissions(ctx)?.administrator(),
                None => false,
            };

            if !is_admin {
                interaction
                    .create_response(&ctx, CreateInteractionResponse::Acknowledge)
                    .await?;
                continue;
            }
        }

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
