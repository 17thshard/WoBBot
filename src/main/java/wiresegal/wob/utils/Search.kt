package wiresegal.wob.utils

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission.ManageMessages
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.component.ButtonBuilder.InteractionButtonBuilder
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import kotlinx.coroutines.NonCancellable.cancel
import wiresegal.wob.utils.SearchAction.Confirm
import wiresegal.wob.utils.SearchAction.LeftAll
import wiresegal.wob.utils.SearchAction.LeftOne
import wiresegal.wob.utils.SearchAction.LeftTen
import wiresegal.wob.utils.SearchAction.Position
import wiresegal.wob.utils.SearchAction.RightAll
import wiresegal.wob.utils.SearchAction.RightOne
import wiresegal.wob.utils.SearchAction.RightTen

/**
 * A set of search results that can be embedded in a message via a [searchPanel].
 */
interface Search<T> {
	val count: Int
	var current: Int
	fun getCurrentValue(): T
}

/**
 * Adds an embed displaying the current [Search] page and a set of buttons to navigate it.
 */
fun <T> MessageBuilder.searchPanel(
	customId: String,
	
	search: Search<T>,
	
	allowedUser: Snowflake,
	
	/**
	 * Create an embed from the currently-selected [search] result while the search is ongoing.
	 */
	buildSearchEmbed: (T) -> EmbedBuilder,
	
	/**
	 * Create an embed from the currently-selected [search] result when the search is finished.
	 */
	buildResultEmbed: (T) -> EmbedBuilder,
) {
	val config = SearchPanel(customId, search, allowedUser, buildSearchEmbed, buildResultEmbed)
	with(config) {
		searchPanel()
	}
}

context(panel: SearchPanel<T>)
private fun <T> MessageBuilder.searchPanel() {
	embed(panel.searchEmbed())
	searchActionButtons()
}

/**
 * Info needed to navigate search results and convert them to embeds.
 */
class SearchPanel<T>(
	val customId: String,
	val search: Search<T>,
	val allowedUser: Snowflake,
	val buildSearchEmbed: (T) -> EmbedBuilder,
	val buildResultEmbed: (T) -> EmbedBuilder,
) {
	init {
		searchPanels[customId] = this
	}
	
	fun searchEmbed() = buildSearchEmbed(search.getCurrentValue())
	fun resultEmbed() = buildResultEmbed(search.getCurrentValue())
}

private val deleteRegex = Regex("""(?<panelId>.+)\.delete\.(?<allowedUserId>\d+)""")
private val searchRegex = Regex("""(?<panelId>.+)\.search\.(?<actionId>\w+)""")
private val searchPanels = mutableMapOf<String, SearchPanel<*>>()

/**
 * Handles events for [searchPanel]s.
 */
suspend fun ActionInteractionCreateEvent.handleSearchEvents() {
	if (this !is ButtonInteractionCreateEvent) return
	
	deleteRegex.matchEntire(interaction.componentId)?.groups?.let { groups ->
		val panelId = groups["panelId"]!!.value
		val allowedUserId = groups["allowedUserId"]!!.value
		handleDeletionEvent(panelId, allowedUserId)
	}
	
	searchRegex.matchEntire(interaction.componentId)?.groups?.let { groups ->
		val panelId = groups["panelId"]!!.value
		val actionId = groups["actionId"]!!.value
		handleNavigationEvent(panelId, actionId)
	}
}

private suspend fun ButtonInteractionCreateEvent.handleDeletionEvent(panelId: String, allowedUserId: String) {
	if (interaction.user.id != Snowflake(allowedUserId)) {
		val userIsMod = this is GuildButtonInteractionCreateEvent
			&& interaction.user.getPermissions().contains(ManageMessages)
		if (!userIsMod) {
			interaction.respondEphemeral { content = "You don't have permission to do this!" }
			return
		}
	}
	
	if (interaction.message.flags?.values?.contains(Ephemeral) == true) {
		interaction.deferEphemeralMessageUpdate().delete()
	} else {
		interaction.deferPublicMessageUpdate().delete()
	}
	searchPanels.remove(panelId)
}

private suspend fun ButtonInteractionCreateEvent.handleNavigationEvent(panelId: String, actionId: String) {
	val action = SearchAction.entries.firstOrNull { it.id == actionId }
		?: return
	val panel = searchPanels[panelId]
		?: return
	
	if (interaction.user.id != panel.allowedUser) {
		val userIsMod = this is GuildButtonInteractionCreateEvent
			&& interaction.user.getPermissions().contains(ManageMessages)
		if (!userIsMod) {
			interaction.respondEphemeral { content = "You don't have permission to do this!" }
			return
		}
	}
	
	context(panel) {
		when (action) {
			LeftOne  -> {
				val deferral = interaction.deferPublicMessageUpdate()
				panel.search.current -= 1
				deferral.edit {
					searchPanel()
				}
			}
			LeftTen  -> {
				val deferral = interaction.deferPublicMessageUpdate()
				panel.search.current -= 10
				deferral.edit {
					searchPanel()
				}
			}
			LeftAll  -> {
				val deferral = interaction.deferPublicMessageUpdate()
				panel.search.current = 0
				deferral.edit {
					searchPanel()
				}
			}
			RightOne -> {
				val deferral = interaction.deferPublicMessageUpdate()
				panel.search.current += 1
				deferral.edit {
					searchPanel()
				}
			}
			RightTen -> {
				val deferral = interaction.deferPublicMessageUpdate()
				panel.search.current += 10
				deferral.edit {
					searchPanel()
				}
			}
			RightAll -> {
				val deferral = interaction.deferPublicMessageUpdate()
				panel.search.current = panel.search.count
				deferral.edit {
					searchPanel()
				}
			}
			Confirm  -> {
				val deferral = interaction.deferPublicMessageUpdate()
				deferral.edit {
					embed(panel.resultEmbed())
					actionRow {
						deleteButton(panel.customId, panel.allowedUser)
					}
				}
				searchPanels.remove(panelId)
			}
			Position -> {
				interaction.deferPublicMessageUpdate()
				// should we make this do something, like a "jump to" feature maybe?
			}
		}
	}
}

/**
 * Components to navigate a search embed.
 *
 * @see handleSearchEvents
 */
private enum class SearchAction(
	val id: String,
	val labelTemplate: String,
	val style: ButtonStyle,
) {
	LeftOne(id = "leftOne", labelTemplate = "◀", style = Primary),
	LeftTen(id = "leftTen", labelTemplate = "⏪\uFE0E", style = Primary),
	LeftAll(id = "leftAll", labelTemplate = "⏮", style = Primary),
	RightOne(id = "rightOne", labelTemplate = "▶", style = Primary),
	RightTen(id = "rightTen", labelTemplate = "⏩\uFE0E", style = Primary),
	RightAll(id = "rightAll", labelTemplate = "⏭", style = Primary),
	Confirm(id = "confirm", labelTemplate = "✓", style = Success),
	Position(id = "position", labelTemplate = "?", style = Secondary) {
		context(panel: SearchPanel<T>)
		override fun <T> renderButton(): ButtonBuilder {
			return InteractionButtonBuilder(style, "${panel.customId}.search.$id").apply {
				label = "${panel.search.current + 1}/${panel.search.count}"
			}
		}
	},
	;
	
	context(panel: SearchPanel<T>)
	open fun <T> renderButton(): ButtonBuilder {
		return InteractionButtonBuilder(style, "${panel.customId}.search.$id").apply {
			label = labelTemplate
		}
	}
}

context(panel: SearchPanel<T>)
private fun <T> ActionRowBuilder.searchActionButton(action: SearchAction) {
	components.add(action.renderButton())
}

context(panel: SearchPanel<T>)
private fun <T> MessageBuilder.searchActionButtons() {
	actionRow {
		searchActionButton(LeftAll)
		searchActionButton(LeftOne)
		searchActionButton(Position)
		searchActionButton(RightOne)
		searchActionButton(RightAll)
	}
	
	actionRow {
		deleteButton(panel.customId, panel.allowedUser)
		searchActionButton(Confirm)
	}
}

fun ActionRowBuilder.deleteButton(customId: String, allowedUser: Snowflake) {
	interactionButton(Danger, "$customId.delete.$allowedUser") {
		label = "✖"
	}
}
