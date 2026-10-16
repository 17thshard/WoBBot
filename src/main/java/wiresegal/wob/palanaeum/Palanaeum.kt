package wiresegal.wob.palanaeum

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.EmbedBuilder.Field
import dev.kord.rest.builder.message.EmbedBuilder.Footer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import wiresegal.wob.utils.Search
import wiresegal.wob.utils.capWithSuffix
import wiresegal.wob.utils.clamp
import wiresegal.wob.utils.deleteButton
import wiresegal.wob.utils.embed
import wiresegal.wob.utils.htmlToDiscordMarkdown
import wiresegal.wob.utils.htmlToPlainText
import wiresegal.wob.utils.searchPanel
import java.lang.System.getenv
import java.net.URI
import java.net.URLEncoder
import kotlin.math.min
import kotlin.text.Charsets.UTF_8
import java.awt.Color as JavaColor

val WOB_COMMAND = getenv("WOB_COMMAND") ?: "wob"
val ARCANUM_URL = getenv("ARCANUM_URL") ?: "https://wob.coppermind.net"
val ARCANUM_ICON = getenv("ARCANUM_ICON") ?: "https://cdn.discordapp.com/emojis/373082865073913859.png?v=1"
val ARCANUM_COLOR = Color(JavaColor.decode(getenv("ARCANUM_COLOR") ?: "#003A52").rgb)
val ARCANUM_NAME = getenv("ARCANUM_NAME") ?: "Arcanum"
val ARCANUM_TOKEN = getenv("ARCANUM_TOKEN")

private val arcanumSuffix = "… (Check $ARCANUM_NAME for more.)"

private val idRegex = Regex(""".+#e(\d+)|(\d+)""")
private val dateFormat = LocalDate.Format {
	monthName(ENGLISH_ABBREVIATED)
	chars(". ")
	day()
	chars(", ")
	year()
}

suspend fun showWoB(interaction: ChatInputCommandInteraction) {
	val param = interaction.command.strings["entry"]!!
	val entryIdMatch = idRegex.matchEntire(param)
	if (entryIdMatch == null) {
		interaction.respondEphemeral {
			content = "Please provide a valid link!"
		}
		return
	}
	val entryId = entryIdMatch.groups[1]?.value ?: entryIdMatch.groups[2]?.value!!
	
	val deferral = interaction.deferPublicResponse()
	val response = apiRequest("entry/$entryId")
	val entry = Json.decodeFromString<Entry>(response)
	deferral.respond {
		embed(entry.toEmbed())
		actionRow {
			deleteButton("wiresegal.wob.${interaction.id}", interaction.user.id)
		}
	}
}

suspend fun searchWoB(interaction: ChatInputCommandInteraction) {
	val deferral = interaction.deferPublicResponse()
	
	val query = interaction.command.strings["query"]!!
	val search = WoBSearch(query)
	if (search.count == 0) {
		deferral.respond {
			content = "No results found!"
		}
		return
	}
	
	deferral.respond {
		searchPanel(
			customId = "wiresegal.wob.${interaction.id}",
			search = search,
			allowedUser = interaction.user.id,
			buildSearchEmbed = { entry ->
				entry.toEmbed().apply {
					author {
						name = "Search: ${search.query}"
					}
				}
			},
			buildResultEmbed = { entry ->
				entry.toEmbed()
			}
		)
		println()
	}
}

fun Entry.toEmbed(): EmbedBuilder {
	val entry = this
	val embed = EmbedBuilder()
	
	embed.apply {
		var charCount = 0
		title = "${entry.eventName} (${dateFormat.format(entry.date)})"
			.also { charCount += it.length }
		url = "$ARCANUM_URL/entry/${entry.id}"
		color = ARCANUM_COLOR
		thumbnail {
			url = ARCANUM_ICON
		}
		
		description = buildString {
			if (entry.eventState.note != null) {
				append("__**${entry.eventState.note}**__\n\n")
			}
			if (entry.paraphrased) {
				append("__**Paraphrased**__\n\n")
			}
		}.also { charCount += it.length }
		
		var sizeExceeded = entry.lines.size > EmbedBuilder.Limits.fieldCount
		for ((speaker, text) in entry.lines.take(EmbedBuilder.Limits.fieldCount)) {
			val plainSpeaker = speaker.htmlToPlainText()
			val charCountWithSpeaker = charCount + plainSpeaker.length
			val textCap = clamp(Field.Limits.value, min = 0, max = EmbedBuilder.Limits.total - charCountWithSpeaker)
			val markdownText = text.htmlToDiscordMarkdown().capWithSuffix(textCap, "*$arcanumSuffix*")
			val charCountWithText = charCountWithSpeaker + markdownText.length
			
			if (charCountWithText > EmbedBuilder.Limits.total) {
				sizeExceeded = true
				break
			}
			
			charCount = charCountWithText
			field {
				name = plainSpeaker
				value = markdownText
			}
		}
		
		if (sizeExceeded) {
			footer {
				text = "(Too long to display. Check $ARCANUM_NAME for more.)"
			}
		} else if (entry.note.isNotBlank()) {
			val cap = min(Footer.Limits.text, EmbedBuilder.Limits.total - charCount)
			val plainNote = ("Footnote: " + entry.note.htmlToPlainText()).capWithSuffix(cap, arcanumSuffix)
			val potentialCount = charCount + plainNote.length
			if (potentialCount <= EmbedBuilder.Limits.total) {
				footer {
					text = plainNote
				}
			}
		}
	}
	
	return embed
}

private class WoBSearch(
	val query: String,
) : Search<Entry> {
	private val apiBase = "search_entry?query=${URLEncoder.encode(query, UTF_8)}"
	private val firstPage = Json.decodeFromString<SearchPage>(apiRequest(apiBase))
	private val maxPerPage = firstPage.results.size
	private val pages = mutableMapOf(1 to firstPage)
	
	override val count = firstPage.count
	override var current = 0
		set(value) {
			field = clamp(value, min = 0, max = count - 1)
		}
	
	override fun getCurrentValue(): Entry {
		val pageIndex = (current / maxPerPage) + 1
		val page = pages.getOrPut(pageIndex) {
			Json.decodeFromString(apiRequest("$apiBase&page=$pageIndex"))
		}
		return page.results[current % maxPerPage]
	}
	
}

private fun apiRequest(endpoint: String): String {
	val url = URI("$ARCANUM_URL/api/$endpoint").toURL()
	val connection = url.openConnection().apply {
		setRequestProperty("Accept", "application/json; charset=utf-8")
		if (ARCANUM_TOKEN != null) setRequestProperty("Authorization", "Token $ARCANUM_TOKEN")
	}
	return connection.getInputStream()
		.use { it.readBytes() }
		.toString(UTF_8)
		// sloppy way to fix relative links by assuming they're all ultimately going back to the homepage
		// a better solution might be to resolve them relative to the API URL during Markdown conversion,
		// but right now I just need to get the bot working again
		.replace(Regex("""href=\\"(\.\./)+"""), """href=\\"$ARCANUM_URL/""")
		// Discord doesn't allow links where the link text contains a URL, so we leave those for the autolinker
		// another thing that'll work better on the Markdown conversion level
		.replace(Regex("""<a href=\\"(.+?)\\">\1</a>"""), $$"$1")
}
