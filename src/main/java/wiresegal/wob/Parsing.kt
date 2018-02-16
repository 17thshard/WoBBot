package wiresegal.wob

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
import org.jsoup.select.Evaluator.Class
import org.jsoup.select.Evaluator.Tag
import java.net.URLEncoder

/**
 * @author WireSegal
 * Created at 11:40 PM on 2/15/18.
 */

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
            return FakeEmbedBuilder(newJson)
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

    val bigSize = if ((1..5).all { harvestFromSearchPage(baseUrl, it, allArticles) })
        "... (250)" else allArticles.size.toString()


    for ((idx, article) in allArticles.withIndex()) {
        val title = article.find(Tag("header"), Class("entry-options")).first().find(Tag("a")).first()
        val titleText = "Search: \"${terms.joinToString()}\" (${idx+1}/$bigSize) \n" + title.text()
        allEmbeds.add(embedFromContent(titleText, "https://wob.coppermind.net" + title.attr("href"), article))
    }

    return allEmbeds
}

fun harvestFromSearchPage(url: String, page: Int, list: MutableList<Element>): Boolean {
    val data = Jsoup.connect(url + page).get()
    list += data.find(Tag("article"), Class("entry-article"))

    return data.find(Class("fa-chevron-right")).isNotEmpty()
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


