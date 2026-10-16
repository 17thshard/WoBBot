package wiresegal.wob.utils

import com.mltheuser.khtmlmarkdown.KHtmlToMarkdown
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import org.jsoup.Jsoup
import kotlin.math.max
import kotlin.math.min

fun clamp(value: Int, min: Int, max: Int) = min(max, max(value, min))

fun String.capWithSuffix(maxLength: Int, suffix: String): String {
	return when {
		maxLength < suffix.length -> ""
		length <= maxLength       -> this
		else                      -> substring(0, maxLength - suffix.length) + suffix
	}
}

/**
 * Extracts the text from an HTML document.
 */
fun String.htmlToPlainText() = Jsoup.parse(this).text()

private val discordMarkdownFormat = KHtmlToMarkdown.Builder()
	.apply {
		options {
			bulletCharacter = "-"
			strongDelimiter = "*"
			emDelimiter = "*"
		}
		
		rule("u") { element, context ->
			val content = context.processChildren(element)
			return@rule "__${content}__"
		}
	}
	.build()

/**
 * Converts a limited subset of HTML to Discord-compatible Markdown.
 */
fun String.htmlToDiscordMarkdown() = discordMarkdownFormat.convert(this)

/**
 * User's visible name as best as can be determined from this object,
 * prioritizing [Member.nickname] > [User.globalName] > [User.username].
 */
val User.visibleName get() = (this as? Member)?.nickname ?: globalName ?: username

/**
 * User's visible name as best as can be determined from this object,
 * prioritizing [Member.memberAvatar] > [User.avatar] > [User.defaultAvatar].
 */
val User.visibleAvatar get() = (this as? Member)?.memberAvatar ?: avatar ?: defaultAvatar

/**
 * Adds an existing embed to this message.
 */
fun MessageBuilder.embed(builder: EmbedBuilder) {
	embeds?.add(builder) ?: run { embeds = [builder] }
}
