package wiresegal.wob.coppermind

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
import wiresegal.wob.misc.setupControls
import wiresegal.wob.misc.setupDeletable
import wiresegal.wob.plugin.sendError
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

fun Remark.convert(node: List<Node>, base: String): String = convertFragment(node.joinToString(""), base)
fun Remark.convert(node: Node, base: String): String = convertFragment(node.toString(), base)

class Coppermind : Wiki("coppermind.net") {

    fun getSectionHTML(title: String): String {
        return parse(getPageText(title)).replace("API", title.replace("[+_]".toRegex(), " "))
    }

    fun getDocument(title: String): Document {
        return Jsoup.parse(getSectionHTML(title), "https://coppermind.net")
    }

    fun resolveFragmentRedirect(title: String) = resolveFragmentRedirect(arrayOf(title))[0]

    fun resolveFragmentRedirect(titles: Array<String>): Array<String?> {
        val url = StringBuilder(query)
        if (!isResolvingRedirects)
            url.append("redirects&")
        url.append("titles=")
        val ret = arrayOfNulls<String>(titles.size)
        val temp = constructTitleString(titles)
        for (blah in temp) {
            val line = fetch(url.toString() + blah, "resolveRedirects")
            var j = line.indexOf("<r ")
            while (j > 0) {
                val parsedtitle = parseAttribute(line, "from", j)
                for (i in titles.indices)
                    if (normalize(titles[i]) == parsedtitle) {
                        ret[i] = parseAttribute(line, "to", j)
                        val frag = parseAttribute(line, "tofragment", j)
                        if (frag != null)
                            ret[i] += "#$frag"
                    }
                j = line.indexOf("<r ", ++j)
            }
        }
        return ret
    }
}

fun fetchPreview(searchInfo: String): Pair<List<String>, String> {
    val x = wiki.getDocument(searchInfo)
    val body = x.body()
    val allNotices = body.children().takeWhile { it.hasClass("notice") }
    allNotices.forEach(Element::remove)
    x.getElementsByClass("infobox").forEach(Element::remove)
    x.getElementsByClass("reference").forEach(Element::remove)
    x.getElementsByClass("mw-references-wrap").forEach(Element::remove)
    x.getElementsByClass("thumb").forEach(Element::remove)

    val sectionHeader = if ('#' in searchInfo)
        x.getElementById(searchInfo.split("#")[1].replace("[+\\s]".toRegex(), "_")).parent()
    else
        body.child(0)

    val sectionNodes = mutableListOf<Element>()
    var next = sectionHeader.nextElementSibling()
    while (next != null && !next.tagName().startsWith("h")) {
        sectionNodes.add(next)
        next = next.nextElementSibling()
    }

    val notices = allNotices.filter { it.childNodeSize() > 0 }.map { wikiMarkup.convert(it.child(0), "https://coppermind.net") }
    var splits = wikiMarkup.convert(sectionNodes, "https://coppermind.net").split("(?<!\\*\\*”\\*\\*)\n{2,}".toRegex())

    if (splits.none { "http://en.wikipedia.org/wiki/Help:Disambiguation" in it })
        splits = splits.take(2)

    val md = splits.joinToString("\n\n")

    return notices to md
}

fun searchResults(searchInfo: String): Pair<Boolean, List<String>> {
    return if (wiki.getPageInfo(searchInfo.replace("[+\\s]".toRegex(), "_"))["exists"] as Boolean)
        false to listOf(wiki.resolveFragmentRedirect(searchInfo.replace("[+\\s]".toRegex(), "_")) ?: searchInfo.replace("[+\\s]".toRegex(), "_"))
    else {
        val search = searchInfo.split("\\s+".toRegex())
        val allArticles = wiki.search(searchInfo)
                .map { it.first() }
        val articles = allArticles.take(10)
        val redirected = wiki.resolveRedirects(articles.toTypedArray())
        (articles.size != allArticles.size) to redirected.mapIndexed { idx, it -> it ?: articles[idx] }
                .toSet()
                .sortedBy { search.count { term -> term in it } }
    }

}

fun embedFromWiki(titlePrefix: String, name: String, entry: Pair<List<String>, String>): EmbedBuilder {
    val (notices, body) = entry

    val title = titlePrefix + name.replace("+", " ").replace("#", ": ")

    val embed = EmbedBuilder()
            .setColor(coppermindColor)
            .setTitle(title)
            .setUrl("https://coppermind.net/wiki/" + name.replace("[+\\s]".toRegex(), "_"))
            .setThumbnail(coppermindIcon)

    val description = mutableListOf<String>()

    notices.mapTo(description) { "**$it**" }
    description.add(body)

    var desc = description.joinToString("\n\n")
    if (desc.length > 1600)
        desc = desc.substring(0, "\\.[\"”'’]?\\s".toRegex().findAll(desc)
                .last { it.range.start <= 1600 }.range.endInclusive)

    embed.setDescription(desc)

    if (embed.toJsonNode().toString().length > 2000)
        return backupEmbed(titlePrefix, name)

    return embed
}

fun backupEmbed(title: String, name: String): EmbedBuilder {
    return EmbedBuilder().setColor(coppermindColor).setTitle(title + name.replace("+", " ").replace("#", ": "))
            .setUrl("https://coppermind.net/wiki/" + name.replace("[+\\s]".toRegex(), "_"))
            .setThumbnail(coppermindIcon).setDescription("An error occurred in loading the wiki preview.")
}

fun harvestFromWiki(terms: List<String>): List<EmbedBuilder> {
    val (large, rawArticles) = searchResults(terms.joinToString("+"))
    val allArticles = rawArticles.map { it to fetchPreview(it) }
    val allEmbeds = mutableListOf<EmbedBuilder>()

    val size = if (large) "... (10)" else allArticles.size.toString()

    for ((idx, article) in allArticles.withIndex()) {
        val (name, body) = article
        val titleText = "Search: \"${terms.joinToString(" ")}\" (${idx+1}/$size) \n"
        allEmbeds.add(embedFromWiki(titleText, name, body))
    }

    return allEmbeds
}

fun searchCoppermind(message: Message, terms: List<String>) {
    val waiting = message.channel.sendMessage("Searching for \"${terms.joinToString(" ")}\"...").get()
    val type = message.channel.typeContinuously()
    try {
        val allEmbeds = harvestFromWiki(terms)

        type.close()

        when {
            allEmbeds.isEmpty() -> message.channel.sendMessage("Couldn't find any articles for \"${terms.joinToString(" ")}\".")
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
        message.channel.sendError("An error occurred trying to look up the article.", e)
    }
}
