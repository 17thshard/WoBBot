package wiresegal.wob

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.request.KtorRequestException
import wiresegal.wob.palanaeum.ARCANUM_NAME
import wiresegal.wob.palanaeum.WOB_COMMAND
import wiresegal.wob.palanaeum.searchWoB
import wiresegal.wob.palanaeum.showWoB
import wiresegal.wob.utils.embed
import wiresegal.wob.utils.handleSearchEvents
import java.lang.System.getenv
import kotlin.system.exitProcess

suspend fun main() {
	val discordToken = getenv("DISCORD_TOKEN")
	if (discordToken == null) {
		println("The bot can't run without a token! Please provide it through the environment variable DISCORD_TOKEN.")
		exitProcess(1)
	}
	val testGuildId = getenv("TEST_GUILD")?.let { Snowflake(it) }
	
	val client = Kord(discordToken)
	
	val commandData = [
		InputCommandData(WOB_COMMAND, "Show a specific $ARCANUM_NAME entry.") {
			string("entry", "URL or ID of entry to show.") { required = true }
			
			handle {
				showWoB(interaction)
			}
		},
		
		InputCommandData("${WOB_COMMAND}s", "Search through a list of $ARCANUM_NAME entries.") {
			string("query", "Query to search for. (See site help page for advanced options.)") { required = true }
			// todo: implement other parameters like tags
			
			handle {
				searchWoB(interaction)
			}
		},
	]
	
	if (testGuildId != null) {
		println("Registering commands on test guild")
		client.createGuildApplicationCommands(testGuildId) {
			commandData.forEach { cmd -> cmd.addTo(this) }
		}
	} else {
		println("Registering commands globally")
		client.createGlobalApplicationCommands {
			commandData.forEach { cmd -> cmd.addTo(this) }
		}
	}
	
	client.on<ActionInteractionCreateEvent> {
		try {
			handleCommandEvents()
			handleSearchEvents()
		} catch (exception: Exception) {
			val messageContent = "Encountered an unexpected problem, please send the information below to the developer."
			val messageEmbed = EmbedBuilder().apply {
				color = Color(0xFF0000)
				description = "```" + exception.stackTraceToString().take(EmbedBuilder.Limits.description - 6) + "```"
			}
			try {
				interaction.respondEphemeral {
					content = messageContent
					embed(messageEmbed)
				}
			} catch (_: KtorRequestException) {
				// might throw if the interaction has already been replied to,
				// but the reason we're deferring is to buy time TO check that,
				// so here we just ignore the exception and hope that was it
				kord.rest.interaction.createFollowupMessage(client.selfId, interaction.token, ephemeral = true) {
					content = messageContent
					embed(messageEmbed)
				}
			}
		}
	}
	
	println("Logging in")
	client.login()
}
