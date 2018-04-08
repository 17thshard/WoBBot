package wiresegal.wob

import com.overzealous.remark.Options
import com.overzealous.remark.Remark
import com.overzealous.remark.convert.InlineStyle
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.wikipedia.Wiki
import java.awt.Color

/**
 * @author WireSegal
 * Created at 10:12 PM on 4/7/18.
 */

val wiki = Coppermind()
val wikiMarkup = Remark(Options.github().apply { inlineLinks = true; preserveRelativeLinks }).apply {
    converter.addInlineNode(InlineStyle(), "blockquote")
}

val coppermindColor = Color(0xCB6D51)
const val coppermindIcon = "https://cdn.discordapp.com/emojis/432391749550342145.png?v=1"

fun Remark.convert(node: Node, base: String): String = convertFragment(node.toString(), base)

class Coppermind : Wiki("coppermind.net") {
    fun getSectionHTML(title: String): String {
        return parse(wiki.getSectionText(title, 0))
    }

    fun getDocument(title: String): Document {
        return Jsoup.parse(getSectionHTML(title), "https://coppermind.net")
    }
}

fun fetchPreview(searchInfo: String): Pair<List<String>, String> {
    val x = wiki.getDocument(searchInfo)
    x.getElementsByClass("infobox").forEach(Element::remove)
    x.getElementsByClass("reference").forEach(Element::remove)
    x.getElementsByClass("mw-references-wrap").forEach(Element::remove)

    val allNotices = x.getElementsByClass("notice")
    allNotices.forEach(Element::remove)

    val notices = allNotices.filter { it.childNodeSize() > 0 }.map { wikiMarkup.convert(it.child(0), "https://coppermind.net") }
    val md = wikiMarkup.convert(x).split("(?<!\\*\\*”\\*\\*)\n{2,}".toRegex()).take(2).joinToString("\n\n")

    return notices to md
}

fun searchResults(searchInfo: String): List<String> {
    return if (wiki.getPageInfo(searchInfo)["exists"] as Boolean)
        listOf(wiki.resolveRedirect(searchInfo) ?: searchInfo)
    else {
        val search = searchInfo.split("\\s+".toRegex())
        val articles = wiki.search(searchInfo)
                .map { it.first() }
        val redirected = wiki.resolveRedirects(articles.toTypedArray())
        redirected.mapIndexed { idx, it -> it ?: articles[idx] }
                .toSet()
                .sortedBy { search.count { term -> term in it } }
    }

}

fun embedFromWiki(titlePrefix: String, name: String, entry: Pair<List<String>, String>): EmbedBuilder {
    val (notices, body) = entry

    val title = titlePrefix + name

    val embed = EmbedBuilder()
            .setColor(coppermindColor)
            .setTitle(title)
            .setUrl("https://coppermind.net/wiki/" + name.replace(" ", "_"))
            .setThumbnail(coppermindIcon)

    val description = notices.toMutableList()

    description.add(body)

    embed.setDescription(description.joinToString("\n\n"))

    if (embed.toJsonNode().toString().length > 2000)
        return backupEmbed(title, name)

    return embed
}

fun backupEmbed(title: String, name: String): EmbedBuilder {
    return EmbedBuilder().setColor(coppermindColor).setTitle(title)
            .setUrl("https://coppermind.net/wiki/" + name.replace(" ", "_"))
            .setThumbnail(coppermindIcon)
}

fun harvestFromWiki(terms: List<String>): List<EmbedBuilder> {
    val allArticles = searchResults(terms.joinToString("+")).map { it to fetchPreview(it) }
    val allEmbeds = mutableListOf<EmbedBuilder>()
    val large = allArticles.size > 20

    val size = if (large) "... (250)" else allArticles.size.toString()

    for ((idx, article) in allArticles.withIndex()) {
        val (name, body) = article
        val titleText = "Search: \"${terms.joinToString()}\" (${idx+1}/$size) \n"
        allEmbeds.add(embedFromWiki(titleText, name, body))
    }

    return allEmbeds
}

fun searchCoppermind(message: Message, terms: List<String>) {
    val waiting = message.channel.sendMessage("Searching for \"${terms.joinToString()}\"...").get()
    val type = message.channel.typeContinuously()
    try {
        val allEmbeds = harvestFromWiki(terms)

        type.close()

        when {
            allEmbeds.isEmpty() -> message.channel.sendMessage("Couldn't find any articles for \"${terms.joinToString()}\".")
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
    } catch (e: Exception) {
        type.close()
        message.channel.sendMessage("An error occurred trying to look up the article.")
        e.printStackTrace()
    }
}
