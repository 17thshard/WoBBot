package wiresegal.wob

import com.fasterxml.jackson.databind.node.ObjectNode
import de.btobastian.javacord.DiscordApi
import de.btobastian.javacord.DiscordApiBuilder
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.exceptions.DiscordException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import org.jsoup.select.Evaluator.*
import java.awt.Color
import java.net.URLEncoder

/**
 * @author WireSegal
 * Created at 9:12 PM on 1/25/18.
 */


// https://discordapp.com/oauth2/authorize?client_id=406271036913483796&scope=bot&permissions=8192

val arcanumColor = Color(0x003A52)
const val iconUrl = "https://cdn.discordapp.com/emojis/373082865073913859.png?v=1"

val api: DiscordApi = DiscordApiBuilder().setToken(token).login().join()


fun Message.startsWith(string: String) = content.startsWith(string, ignoreCase = true)

fun Element.find(vararg evaluators: Evaluator) = allElements.find(*evaluators)

fun Elements.find(vararg evaluators: Evaluator): Elements {
    var nodes: List<Element> = this
    for (evaluator in evaluators)
        nodes = nodes.filter { it.`is`(evaluator) }
    return Elements(nodes)
}

fun embedFromContent(title: String, url: String, article: Element): EmbedBuilder {
    val embed = EmbedBuilder().setColor(arcanumColor).setTitle(title).setUrl(url).setThumbnail(iconUrl)

    var pending = false

    val lines = mutableListOf<String>()
    var lastSpeaker = "Brandon Sanderson" // Assumed

    val content = article.find(Tag("div"), Class("entry-content")).first()
    val footnote = article.find(Tag("small"), Class("footnote")).first()
    if (footnote != null)
        embed.setFooter(footnote.text())

    val fields = mutableListOf<Pair<String, String>>()

    var lastLine = false

    for (child in content.childNodes()) {
        var text: String? = null
        if (child is Element && child.hasClass("entry-speaker")) {
            if (lines.isNotEmpty()) {
                val str = lines.joinToString("\n").replace("\\s{2,}".toRegex(), " ")
                fields.add(lastSpeaker to str)
                lines.clear()
            }
            lastLine = false
            lastSpeaker = child.text()
            if (lastSpeaker.contains("[PENDING REVIEW]")) {
                pending = true
                lastSpeaker = lastSpeaker.replace("[PENDING REVIEW]", "").trim()
            }
        } else if (child is Element && child.tag().isInline) {
            var line = child.text()

            if (child.tagName() == "p" || lines.isEmpty()) text = line
            else {
                when {
                    child.tagName() == "i" -> line = "_${line}_"
                    child.tagName() == "b" -> line = "**$line**"
                    child.tagName() == "u" -> line = "__${line}__"
                }
                lines[lines.size - 1] = lines.last() + line
                lastLine = true
            }
        } else if (child is Element) text = child.text()
        else if (child is TextNode) text = child.text()

        if (text != null) {
            if (lastLine)
                lines[lines.size - 1] = lines.last() + text
            else
                lines.add(text)
            lastLine = false
        }
    }

    if (pending)
        embed.setDescription("__**Pending Review**__")

    if (lines.isNotEmpty()) {
        val str = lines.joinToString("\n").replace("\\s{2,}".toRegex(), " ")
        fields.add(lastSpeaker to str)
    }

    var lastJson = embed.toJsonNode()
    for ((author, comment) in fields.filter { it.second.isNotBlank() }
            .map { (author, comment) ->
        if (comment.length > 1024) author to comment.substring(0, 1000)
                .replace("\\w+$".toRegex(), "").trim() + " *… (Check Arcanum for more.)*"
        else author to comment
    }) {
        embed.addField(author, comment, false)
        val newJson = embed.toJsonNode()
        if (newJson.toString().length > 1950) {
            val footer = lastJson.putObject("footer")
            footer.put("text", "(Too long to display. Check Arcanum for more.)")
            return object : EmbedBuilder() {
                override fun toJsonNode(`object`: ObjectNode): ObjectNode {
                    return `object`.setAll(lastJson) as ObjectNode
                }
            }
        }
        lastJson = newJson
    }

    if (embed.toJsonNode().toString().length > 2000)
        return backupEmbed(title, url)

    return embed
}

fun backupEmbed(title: String, url: String): EmbedBuilder {
    val backup = EmbedBuilder().setColor(arcanumColor).setTitle(title).setUrl(url).setThumbnail(iconUrl)
    backup.setDescription("This WoB is too long. Click on the link above to see it on Arcanum.")
    return backup
}

const val masterUrl = "https://wob.coppermind.net/adv_search/?ordering=rank&query="

fun harvestFromSearch(terms: List<String>): List<EmbedBuilder> {
    val baseUrl = masterUrl + terms.joinToString("+") { URLEncoder.encode(it, "UTF-8") } + "&page="
    val allArticles = mutableListOf<Element>()
    val allEmbeds = mutableListOf<EmbedBuilder>()

    (1..5).all { harvestFromSearchPage(baseUrl, it, allArticles) }

    for ((idx, article) in allArticles.withIndex()) {
        val title = article.find(Tag("header"), Class("entry-options")).first().find(Tag("a")).first()
        val titleText = "Search: \"${terms.joinToString()}\" (${idx+1}/${allArticles.size}) \n" + title.text()
        allEmbeds.add(embedFromContent(titleText, "https://wob.coppermind.net" + title.attr("href"), article))
    }

    return allEmbeds
}

fun harvestFromSearchPage(url: String, page: Int, list: MutableList<Element>): Boolean {
    val data = Jsoup.connect(url + page).get()
    list += data.find(Tag("article"), Class("entry-article"))

    return data.find(Class("fa-chevron-right")).isNotEmpty()
}

const val arrowLeft = "⬅"
const val arrowRight = "➡"
const val done = "\u23F9"
const val last = "⏭"
const val first = "⏮"
const val jumpLeft = "⏪"
const val jumpRight = "⏩"

val validReactions = listOf(arrowLeft, arrowRight, done, last, first, jumpLeft, jumpRight)

val messagesWithEmbedLists = mutableMapOf<Long, Triple<Long, Int, List<EmbedBuilder>>>()

fun updateMessageWithJump(jump: Int, message: Message, entry: Triple<Long, Int, List<EmbedBuilder>>) {
    val (uid, index, embeds) = entry
    val newIndex = Math.min(Math.max(index + jump, 0), embeds.size - 1)
    if (index != newIndex) {
        val newEmbed = embeds[newIndex]
        message.edit(newEmbed)
        messagesWithEmbedLists.put(message.id, Triple(uid, newIndex, embeds))
    }
}

fun search(message: Message, terms: List<String>) {
    val waiting = message.channel.sendMessage("Searching for \"${terms.joinToString().replace("&!", "!")}\"...").get()
    val type = message.channel.typeContinuously()
    val allEmbeds = harvestFromSearch(terms)
    type.close()

    try {
        when {
            allEmbeds.isEmpty() -> message.channel.sendMessage("Couldn't find any WoBs for \"${terms.joinToString().replace("&!", "!")}\".")
            allEmbeds.size == 1 -> {
                val finalEmbed = allEmbeds.first()
                finalEmbed.setTitle(finalEmbed.toJsonNode()["title"].asText().replace(".*\n".toRegex(), ""))
                message.channel.sendMessage("", finalEmbed)
            }
            else -> {
                val search = message.channel.sendMessage("", allEmbeds.first()).get()
                if (allEmbeds.size > 2)
                    search.addReaction(first)
                if (allEmbeds.size > 10)
                    search.addReaction(jumpLeft)
                search.addReaction(arrowLeft)
                search.addReaction(done)
                search.addReaction(arrowRight)
                if (allEmbeds.size > 10)
                    search.addReaction(jumpRight)
                if (allEmbeds.size > 2)
                    search.addReaction(last)

                messagesWithEmbedLists.put(search.id, Triple(message.author.id, 0, allEmbeds))
            }
        }
        waiting.delete()
    } catch (e: DiscordException) {
        message.channel.sendMessage("An error occurred trying to look up the WoB.")
    }
}

fun main(args: Array<String>) {
    api.addMessageCreateListener {
        val message = it.message
        if (!message.userAuthor.orElseGet { api.yourself }.isBot) {
            if (message.content == "!wob" || message.startsWith("!wob ") || message.mentionedUsers.any { it.isYourself }) {
                val trimmed = message.content.replace("\\s".toRegex(), "")
                if (trimmed == "!wobhelp" || trimmed == api.yourself.mentionTag + "help" ||
                        trimmed == "!wob" || trimmed == api.yourself.mentionTag) {
                    message.channel.sendMessage("Use `!wob \"term\"` to search, or put a WoB link in to get its text directly.")
                } else {
                    val allWobs = "wob\\.coppermind\\.net/events/[\\w-]+/#(e\\d+)".toRegex().findAll(message.content)

                    for (wob in allWobs) async {
                        val url = "https://" + wob.value
                        val document = Jsoup.connect(url).get()
                        val article = document.find(Tag("article"), Id(wob.groupValues[1])).first()
                        val details = document.find(Tag("div"), Class("eventDetails")).first()
                        if (article != null && details != null) {
                            val title = details.find(ContainsOwnText("Name")).last().parent().text().removePrefix("Name ")
                            val date = details.find(ContainsOwnText("Date")).last().parent().text().removePrefix("Date ")
                            message.channel.sendMessage(embedFromContent("$title ($date)", url, article))
                        }
                    }

                    if (allWobs.none()) {
                        val terms = "[\"“]([\\w\\s,+!|&]+)[\"”]".toRegex().findAll(message.content).toList()
                                .flatMap { it.groupValues[1]
                                        .replace("([^&])!".toRegex(), "$1&!")
                                        .split("[\\s,]+".toRegex())
                                }.filter { it.matches("[!+|&\\w]+".toRegex()) }
                        if (terms.any()) async {
                            search(message, terms)
                        }
                    }
                }
            } else if (message.content.trim() == "Say the Words!")
                message.channel.sendMessage("Life before death.\nStrength before weakness.\nJourney before destination.")
        }
    }
    api.addReactionAddListener {
        val message = it.message.get()
        val reaction = it.reaction.get()
        if (reaction.emoji.isUnicodeEmoji) {
            val unicode = reaction.emoji.asUnicodeEmoji().get()
            if (!it.user.isBot && unicode in validReactions) {
                val messageValue = messagesWithEmbedLists[message.id]
                if (messageValue != null) {
                    val (uid, index, embeds) = messageValue
                    if (uid == it.user.id) {
                        when (unicode) {
                            arrowLeft -> updateMessageWithJump(-1, message, messageValue)
                            jumpLeft -> updateMessageWithJump(-10, message, messageValue)
                            first -> updateMessageWithJump(-embeds.size, message, messageValue)
                            arrowRight -> updateMessageWithJump(1, message, messageValue)
                            jumpRight -> updateMessageWithJump(10, message, messageValue)
                            last -> updateMessageWithJump(embeds.size, message, messageValue)
                            done -> {
                                val finalEmbed = embeds[index]
                                finalEmbed.setTitle(finalEmbed.toJsonNode()["title"].asText().replace(".*\n".toRegex(), ""))
                                message.edit(finalEmbed)
                                messagesWithEmbedLists.remove(message.id)
                                message.removeAllReactions()
                            }
                        }
                    }
                    it.removeReaction()
                }
            }
        }
    }
}

