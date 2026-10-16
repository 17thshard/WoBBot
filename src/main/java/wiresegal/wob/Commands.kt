package wiresegal.wob

import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import dev.kord.rest.builder.interaction.GuildMultiApplicationCommandBuilder
import dev.kord.rest.builder.interaction.MessageCommandCreateBuilder
import dev.kord.rest.builder.interaction.UserCommandCreateBuilder

typealias EventHandler<T> = suspend T.() -> Unit
typealias ChatInputCommandHandler = EventHandler<ChatInputCommandInteractionCreateEvent>
typealias UserCommandHandler = EventHandler<UserCommandInteractionCreateEvent>
typealias MessageCommandHandler = EventHandler<MessageCommandInteractionCreateEvent>

private val chatCommands = mutableMapOf<String, ChatInputCommandHandler>()
private val userCommands = mutableMapOf<String, UserCommandHandler>()
private val messageCommands = mutableMapOf<String, MessageCommandHandler>()

suspend fun ActionInteractionCreateEvent.handleCommandEvents() {
	when (val event = this) {
		is ChatInputCommandInteractionCreateEvent -> chatCommands[interaction.invokedCommandName]?.invoke(event)
			?: throw IllegalArgumentException("Unrecognized `${interaction.invokedCommandType}` command `${interaction.invokedCommandName}.`")
		is UserCommandInteractionCreateEvent      -> userCommands[interaction.invokedCommandName]?.invoke(event)
			?: throw IllegalArgumentException("Unrecognized `${interaction.invokedCommandType}` command `${interaction.invokedCommandName}.`")
		is MessageCommandInteractionCreateEvent   -> messageCommands[interaction.invokedCommandName]?.invoke(event)
			?: throw IllegalArgumentException("Unrecognized `${interaction.invokedCommandType}` command `${interaction.invokedCommandName}.`")
		else                                      -> return
	}
}

sealed interface CommandData {
	fun addTo(commandsBuilder: GlobalMultiApplicationCommandBuilder)
	fun addTo(commandsBuilder: GuildMultiApplicationCommandBuilder)
}

class InputCommandData(
	val name: String,
	val description: String,
	val builder: ChatInputCreateBuilder.() -> Unit = {},
) : CommandData {
	override fun addTo(commandsBuilder: GlobalMultiApplicationCommandBuilder) {
		commandsBuilder.input(name, description, builder)
	}
	
	override fun addTo(commandsBuilder: GuildMultiApplicationCommandBuilder) {
		commandsBuilder.input(name, description, builder)
	}
}

fun ChatInputCreateBuilder.handle(handler: ChatInputCommandHandler) {
	chatCommands[this.name] = handler
}

class UserCommandData(
	val name: String,
	val builder: UserCommandCreateBuilder.() -> Unit = {},
) : CommandData {
	override fun addTo(commandsBuilder: GlobalMultiApplicationCommandBuilder) {
		commandsBuilder.user(name, builder)
	}
	
	override fun addTo(commandsBuilder: GuildMultiApplicationCommandBuilder) {
		commandsBuilder.user(name, builder)
	}
}

fun UserCommandCreateBuilder.handle(handler: UserCommandHandler) {
	userCommands[this.name] = handler
}

class MessageCommandData(
	val name: String,
	val builder: MessageCommandCreateBuilder.() -> Unit = {},
) : CommandData {
	override fun addTo(commandsBuilder: GlobalMultiApplicationCommandBuilder) {
		commandsBuilder.message(name, builder)
	}
	
	override fun addTo(commandsBuilder: GuildMultiApplicationCommandBuilder) {
		commandsBuilder.message(name, builder)
	}
}

fun MessageCommandCreateBuilder.handle(handler: MessageCommandHandler) {
	messageCommands[this.name] = handler
}
