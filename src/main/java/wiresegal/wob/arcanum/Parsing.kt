package wiresegal.wob.arcanum

import de.btobastian.javacord.entities.User
import de.btobastian.javacord.entities.channels.PrivateChannel
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.Messageable
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.entities.permissions.PermissionState
import de.btobastian.javacord.entities.permissions.PermissionType
import de.btobastian.javacord.entities.permissions.PermissionsBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.jsoup.select.Elements
import wiresegal.wob.*
import wiresegal.wob.misc.catch
import wiresegal.wob.misc.setupControls
import wiresegal.wob.misc.setupDeletable
import wiresegal.wob.misc.then
import wiresegal.wob.misc.util.FakeEmbedBuilder
import wiresegal.wob.plugin.sendError
import wiresegal.wob.plugin.visibleCommands
import java.time.Instant

/**
 * @author WireSegal
 * Created at 11:40 PM on 2/15/18.
 */

const val EMBED_LIMIT = 6000
const val TITLE_LIMIT = 256
const val DESCRIPTION_LIMIT = 2048
const val FIELDS_LIMIT = 25
const val FIELD_NAME_LIMIT = 256
const val FIELD_TEXT_LIMIT = 1024
const val FOOTER_LIMIT = 2048
const val MESSAGE_LIMIT = 2000

fun String.capWithSuffix(len: Int, suffix: String): String {
    if (length <= len)
        return this
    return substring(0, len - suffix.length).replace("\\w+$".toRegex(), "").trim() + suffix
}

fun embedFromContent(titlePrefix: String, entry: Entry): EmbedBuilder {
    val date = entry.date.split("-")
    val dateStr = if (date.size >= 3) {
        "(${months[date[1].toInt() - 1]} ${date[2].removePrefix("0")}, ${date[0]})"
    } else
        entry.date

    val title = (titlePrefix + entry.eventName + " " + dateStr).take(TITLE_LIMIT)

    val embed = EmbedBuilder()
            .setColor(embedColor)
            .setTitle(title)
            .setUrl(entry.toString())
            .setThumbnail(iconUrl)

    val flags = mutableListOf<String>()

    if (entry.eventState == ReviewState.PENDING) flags.add("__Pending Review__")
    if (entry.paraphrased) flags.add("__Paraphrased__")
    if (entry.eventState == ReviewState.APPROVED) flags.add("_Approved_")

    if (flags.isNotEmpty())
        embed.setDescription("**" + flags.joinToString().take(DESCRIPTION_LIMIT - 4) + "**")

    val arcanumSuffix = "*… (Check Arcanum for more.)*"

    if (entry.note != null && entry.note.isNotBlank())
        embed.setFooter(("Footnote: " + entry.getFooterText())
                .capWithSuffix(FOOTER_LIMIT, arcanumSuffix))

    for ((speaker, comment) in entry.lines.take(FIELDS_LIMIT)
            .map {
                it.getTrueSpeaker().run { if (isEmpty()) "Context" else this }.take(FIELD_NAME_LIMIT) to
                        it.getTrueText().capWithSuffix(FIELD_TEXT_LIMIT, arcanumSuffix)
            }) {
        val oldJson = embed.toJsonNode()
        embed.addField(speaker, comment, false)
        val newJson = embed.toJsonNode()
        val footer = newJson.objectNode()
        footer.put("text", "(Too long to display. Check the original for more.)")
        val oldFooter = newJson.get("footer")?.toString() ?: ""
        val size = footer.toString().length - oldFooter.length
        if (newJson.toString().length > EMBED_LIMIT - size) {
            oldJson.set("footer", footer)
            return FakeEmbedBuilder(oldJson)
        }
    }

    if (embed.toJsonNode().toString().length > EMBED_LIMIT)
        return backupEmbed(title, entry)

    return embed
}

fun backupEmbed(title: String, entry: Entry): EmbedBuilder {
    val backup = EmbedBuilder().setColor(embedColor).setTitle(title)
            .setUrl(entry.toString())
            .setThumbnail(iconUrl)
    backup.setDescription("This entry is too long. Click on the link above to see the original.")
    return backup
}

val months = listOf("Jan.", "Feb.", "March", "April",
        "May", "June", "July", "Aug.",
        "Sept.", "Oct.", "Nov.", "Dec.")

fun harvestFromSearch(terms: List<String>): List<EmbedBuilder> {
    val (allArticles, large) = entriesFromSearch(terms)
    val allEmbeds = mutableListOf<EmbedBuilder>()

    val size = if (large) "... (250)" else allArticles.size.toString()

    for ((idx, article) in allArticles.withIndex()) {
        val titleText = "Search: \"${terms.joinToString()}\" (${idx + 1}/$size) \n"
        allEmbeds.add(embedFromContent(titleText, article))
    }

    return allEmbeds
}

fun searchWoB(message: Message, terms: List<String>) {
    var type = AutoCloseable {}
    message.channel.sendMessage("Searching for \"${terms.joinToString(" ")}\"...").then {
        type = message.channel.typeContinuously()
        val allEmbeds = harvestFromSearch(terms)

        type.close()

        when {
            allEmbeds.isEmpty() -> message.channel.sendMessage("Couldn't find any entries for \"${terms.joinToString().replace("&!", "!")}\".")
            allEmbeds.size == 1 -> {
                val finalEmbed = allEmbeds.first()
                finalEmbed.setTitle(finalEmbed.toJsonNode()["title"].asText().replace(".*\n".toRegex(), ""))
                message.channel.sendMessage(finalEmbed).setupDeletable(message.author)
            }
            else ->
                message.channel.sendMessage(allEmbeds.first())
                        .then { type.close() }
                        .setupDeletable(message.author).setupControls(message.author, 0, allEmbeds)
        }
        if (it.channel !is PrivateChannel)
            it.delete()
    }.catch {
        type.close()
        message.sendError("An error occurred trying to look up the entry.", it)
    }
}

fun about(message: Message) {
    val invite = api.createBotInvite(PermissionsBuilder().setState(PermissionType.MANAGE_MESSAGES, PermissionState.ALLOWED).build())
    val wireID = 77084495118868480L
    val wire = api.getUserById(wireID)
    val wireStr = if (wire.isPresent) wire.get().mentionTag else "@wiresegal#1522"
    val host = api.owner
    val hostStr = if (host.isPresent) host.get().mentionTag else wireStr

    val add = if (hostStr != wireStr) "\nHosted by: $hostStr" else ""

    message.channel.sendMessage(EmbedBuilder().apply {
        setTitle("About WoBBot")
        setColor(embedColor)

        setDescription("**Commands:** \n" +
                (visibleCommands.filter { it.matches(message) }.joinToString("\n") { " * " + it.name }) + "\n" +
                "Author: $wireStr$add\n" +
                "[Invite Link]($invite) | " +
                "[Github Source](https://github.com/Palanaeum/WoBBot) | " +
                "[Arcanum]($urlTarget)")
    })
}

data class Progress(val title: String, val percentage: String, val link: String?)

val progressCache = mutableListOf<Progress>()
var progressCacheTimeStamp: Long? = null

fun showProgressBar(message: Message) {
    val progresses = extractProgresses()
    val embed = EmbedBuilder ()
            .setColor(embedColor)
            .setTitle("Progress Bars")
            .setUrl(homepageTarget)

    val full = "█"
    val empty = "░"
    val sensitivity = 20

    var progressInformation = ""
    for ((name, percent, link) in progresses) {
        val percentNumber = Integer.parseInt(percent)
        val points = (percentNumber/100.0 * sensitivity).toInt()
        val bar = full.repeat(points) + empty.repeat(sensitivity - points)
        val title = if(link != null) "[$name]($link)" else name
        progressInformation += "**$title**\n$bar $percent%\n\n"
    }
    embed.setDescription(progressInformation)

    message.channel.sendMessage(embed)
}

fun extractProgresses(): List<Progress> {
    if (progressCacheTimeStamp != null
            && (System.currentTimeMillis() - progressCacheTimeStamp!! <= progressCachePersistence))
        return progressCache

    val elementClass = "vc_label"

    progressCache.clear()

    val doc = Jsoup.connect(homepageTarget).get()
    val content = doc.getElementsByClass(elementClass)
    for (elem in content) {
        val percentage = elem.getElementsByTag("span").text()
        elem.getElementsByTag("span").remove()
        val title = elem.text()
        val link = elem.getElementsByTag("a").firstOrNull()?.attr("href")

        progressCache.add(Progress(Entities.unescape(title).trim(), percentage.trim().substring(0, percentage.length-1), link))
    }

    progressCacheTimeStamp = System.currentTimeMillis();
    return progressCache
}

fun applyToOwners(toApply: User.() -> Unit) {
    val wireID = 77084495118868480L
    val wire = api.getUserById(wireID)

    if (wobCommand == "wob" && wire.isPresent)
        wire.get().toApply()
    if (api.owner.isPresent && (api.ownerId != wireID || wobCommand != "wob"))
        api.owner.get().toApply()
}

fun notifyOwners(launch: Instant) = notifyOwners {
    setColor(embedColor)
    setTitle("Launch Notification")
    addField("Last Commit", "$commitDesc ($commitId)", false)
    addField("Committer", committer.toString(), false)
    addField("Commit Time", version.toString(), false)
    setTimestamp(launch)
}

fun notifyOwners(embed: EmbedBuilder.() -> Unit) = applyToOwners {
    sendMessage(EmbedBuilder().apply(embed))
}

fun notifyOwners(data: String, name: String) = applyToOwners {
    sendTo(data, name)
}

fun Messageable.sendTo(data: String, name: String) {
    val prefix = "$name: "
    when {
        data.length > MESSAGE_LIMIT - prefix.length ->
            sendMessage(data.byteInputStream(), "$name.txt")
        data.isEmpty() ->
            sendMessage("Nothing found for $name!")
        else ->
            sendMessage(prefix + data)
    }
}
