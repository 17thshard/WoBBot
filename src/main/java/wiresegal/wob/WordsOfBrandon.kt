package wiresegal.wob

import com.fasterxml.jackson.databind.node.ObjectNode
import de.btobastian.javacord.DiscordApi
import de.btobastian.javacord.DiscordApiBuilder
import de.btobastian.javacord.entities.DiscordEntity
import de.btobastian.javacord.entities.channels.TextChannel
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.entities.permissions.PermissionState
import de.btobastian.javacord.entities.permissions.PermissionType
import de.btobastian.javacord.entities.permissions.PermissionsBuilder
import de.btobastian.javacord.exceptions.DiscordException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import org.jsoup.select.Evaluator.*
import java.awt.Color
import java.net.URLEncoder
import java.util.*

/**
 * @author WireSegal
 * Created at 9:12 PM on 1/25/18.
 */

val arcanumColor = Color(0x003A52)
const val iconUrl = "https://cdn.discordapp.com/emojis/373082865073913859.png?v=1"

val api: DiscordApi = DiscordApiBuilder().setToken(token).login().join()

fun Array<Pair<String, String>>.embeds(title: String, color: Color): List<EmbedBuilder> {
    return this.mapIndexed { idx, (rattle, comment) -> EmbedBuilder().apply {
        setTitle("(${idx + 1}/$size) \n$title")
        setColor(color)
        setDescription(rattle)
        setFooter(comment)
    } }
}

fun Array<Pair<String, Pair<String, Color>>>.embeds(title: String): List<EmbedBuilder> {
    return this.mapIndexed { idx, (rattle, pair) -> EmbedBuilder().apply {
        val (comment, color) = pair
        setTitle("(${idx + 1}/$size) \n$title")
        setColor(color)
        setDescription(rattle)
        setFooter(comment)
    } }
}

fun TextChannel.sendRandomEmbed(requester: DiscordEntity, title: String, color: Color, messages: Array<Pair<String, String>>) {
    val embeds = messages.embeds(title, color)

    val index = (Math.random() * embeds.size).toInt()
    val embed = embeds[index]

    sendMessage(embed).get().setupDeletable(requester).setupControls(requester, index, embeds)
}

fun TextChannel.sendRandomEmbed(requester: DiscordEntity, title: String, messages: Array<Pair<String, Pair<String, Color>>>) {
    val embeds = messages.embeds(title)

    val index = (Math.random() * embeds.size).toInt()
    val embed = embeds[index]

    sendMessage(embed).get().setupDeletable(requester).setupControls(requester, index, embeds)
}

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
    var lastSpeaker = "Context"

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
            if (lastSpeaker.isBlank())
                lastSpeaker = "Context"
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
                    child.tagName() == "a" -> {
                        var href = child.attr("href")
                        if (href.startsWith("/"))
                            href = "https://wob.coppermind.net" + href
                        line = "[$line]($href)"
                    }
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
        val str = lines
                .joinToString("\n") { it.replace("\\s{2,}".toRegex(), " ") }
                .replace("\n{3,}".toRegex(), "\n\n")
        fields.add(lastSpeaker to str)
    }

    var lastJson = embed.toJsonNode()
    val arcanumSuffix = "*… (Check Arcanum for more.)*"
    for ((author, comment) in fields.filter { it.second.isNotBlank() }
            .map { (author, comment) ->
                if (comment.length > 1024) author to comment.substring(0, 1024 - arcanumSuffix.length)
                        .replace("\\w+$".toRegex(), "").trim() + arcanumSuffix
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
const val no = "❌"

val validReactions = listOf(arrowLeft, arrowRight, done, last, first, jumpLeft, jumpRight, no)

val messagesWithEmbedLists = mutableMapOf<Long, Triple<Long, Int, List<EmbedBuilder>>>()
val messageToAuthor = mutableMapOf<Long, Long>()

fun updateMessageWithJump(jump: Int, message: Message, entry: Triple<Long, Int, List<EmbedBuilder>>) {
    val (uid, index, embeds) = entry
    val newIndex = Math.min(Math.max(index + jump, 0), embeds.size - 1)
    if (index != newIndex) {
        val newEmbed = embeds[newIndex]
        message.edit(newEmbed)
        messagesWithEmbedLists[message.id] = Triple(uid, newIndex, embeds)
    }
}

fun Message.setupDeletable(author: DiscordEntity) = setupDeletable(author.id)

fun Message.setupDeletable(id: Long): Message {
    messageToAuthor[this.id] = id
    addReaction(no)
    return this
}

fun Message.setupControls(requester: DiscordEntity, index: Int, embeds: List<EmbedBuilder>): Message {
    if (embeds.size > 2)
        addReaction(first)
    if (embeds.size > 10)
        addReaction(jumpLeft)
    addReaction(arrowLeft)
    addReaction(done)
    addReaction(arrowRight)
    if (embeds.size > 10)
        addReaction(jumpRight)
    if (embeds.size > 2)
        addReaction(last)

    messagesWithEmbedLists[id] = Triple(requester.id, index, embeds)
    return this
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
                message.channel.sendMessage(finalEmbed).get().setupDeletable(message.author)
            }
            else ->
                message.channel.sendMessage(allEmbeds.first()).get()
                        .setupDeletable(message.author).setupControls(message.author, 0, allEmbeds)
        }
        waiting.delete()
    } catch (e: DiscordException) {
        message.channel.sendMessage("An error occurred trying to look up the WoB.")
    }
}

fun about(message: Message) {
    val invite = api.createBotInvite(PermissionsBuilder().setState(PermissionType.MANAGE_MESSAGES, PermissionState.ALLOWED).build())
    val wireID = 77084495118868480L
    val wire = api.getUserById(wireID)
    val wireStr = if (wire.isPresent) wire.get().mentionTag else "@wiresegal#1522"
    val host = api.owner
    val hostStr = if (host.isPresent) host.get().mentionTag else "@wiresegal#1522"

    val add = if (hostStr != wireStr) "\nHosted by: $hostStr" else ""

    message.channel.sendMessage(EmbedBuilder().apply {
        setTitle("About WoBBot")
        setColor(arcanumColor)

        setDescription("Commands: \n" +
                " - `!wob`\n" +
                " - `Ask the Silent Gatherers`\n" +
                " - `Consult the Diagram`\n" +
                " - `Check the Gemstone Archives`\n" +
                "Author: $wireStr$add\n" +
                "[Invite Link]($invite) | " +
                "[Github Source](https://github.com/yrsegal/WoBBot) | " +
                "[Arcanum](https://wob.coppermind.net/)")
    })
}

fun main(args: Array<String>) {
    api.addMessageCreateListener {
        val message = it.message
        if (!message.userAuthor.orElseGet { api.yourself }.isBot) {
            val content = message.content.toLowerCase(Locale.ROOT)
            val trimmed = content.replace("\\s".toRegex(), "")
            val noChrTrimmed = trimmed.replace("\\W".toRegex(), "")
            if (trimmed != "!wobabout" && (message.privateChannel.isPresent ||
                    content == "!wob" || content.startsWith("!wob ") || message.mentionedUsers.any { it.isYourself })) {
                if (trimmed == "!wobhelp" || trimmed == api.yourself.mentionTag + "help" ||
                        trimmed == "!wob") {
                    message.channel.sendMessage("Use `!wob \"term\"` to search, or put a WoB link in to get its text directly.")
                } else if (trimmed == api.yourself.mentionTag)
                    about(message)
                else {
                    val allWobs = "wob\\.coppermind\\.net/events/[\\w-]+/#(e\\d+)".toRegex().findAll(content)

                    for (wob in allWobs) async {
                        val url = "https://" + wob.value
                        val document = Jsoup.connect(url).get()
                        val article = document.find(Tag("article"), Id(wob.groupValues[1])).first()
                        val details = document.find(Tag("div"), Class("eventDetails")).first()
                        if (article != null && details != null) {
                            val title = details.find(ContainsOwnText("Name")).last().parent().text().removePrefix("Name ")
                            val date = details.find(ContainsOwnText("Date")).last().parent().text().removePrefix("Date ")
                            message.channel.sendMessage(embedFromContent("$title ($date)", url, article)).get().setupDeletable(message.author)
                        }
                    }

                    if (allWobs.none()) {
                        val contentModified = if (message.privateChannel.isPresent && "^[\\w\\s,+!|&]+$".toRegex().matches(content))
                            "\"" + content + "\"" else content

                        val terms = "[\"“]([\\w\\s,+!|&]+)[\"”]".toRegex().findAll(contentModified).toList()
                                .flatMap {
                                    it.groupValues[1]
                                            .replace("([^&])!".toRegex(), "$1&!")
                                            .split("[\\s,]+".toRegex())
                                }.filter { it.matches("[!+|&\\w]+".toRegex()) }
                        if (terms.any()) async {
                            search(message, terms)
                        }
                    }
                }
            } else if (trimmed == "!wobabout")
                about(message)
            else if (noChrTrimmed.startsWith("saythewords"))
                message.channel.sendMessage("**`Life before death.`**\n" +
                        "**`Strength before weakness.`**\n" +
                        "**`Journey before destination.`**")
            else if ("lifebeforedeath" in noChrTrimmed &&
                    "strengthbeforeweakness" in noChrTrimmed &&
                    "journeybeforedestination" in noChrTrimmed)
                message.channel.sendMessage("**`These Words are Accepted.`**")
            else if (noChrTrimmed.startsWith("askthesilentgatherers"))
                message.channel.sendRandomEmbed(message.author, "Death Rattles", Color.RED, rattles)
            else if (noChrTrimmed.startsWith("consultthediagram"))
                message.channel.sendRandomEmbed(message.author, "The Diagram", Color.BLUE, diagram)
            else if (noChrTrimmed.startsWith("checkthegemstonearchives"))
                message.channel.sendRandomEmbed(message.author, "Gemstone Archives", archive)
        }
    }
    api.addReactionAddListener {
        if (it.message.isPresent && it.reaction.isPresent) {
            val message = it.message.get()
            val reaction = it.reaction.get()
            if (message.author.isYourself && reaction.emoji.isUnicodeEmoji) {
                val unicode = reaction.emoji.asUnicodeEmoji().get()
                if (!it.user.isBot && unicode in validReactions) {
                    if (unicode == no) {
                        if (messageToAuthor[message.id] == it.user.id)
                            it.deleteMessage()
                        else
                            it.removeReaction()
                    } else {
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
                                        message.removeAllReactions().whenComplete { _, _ -> message.setupDeletable(uid) }
                                    }
                                }
                            }
                            it.removeReaction()
                        }
                    }
                }
            }
        }
    }
}
