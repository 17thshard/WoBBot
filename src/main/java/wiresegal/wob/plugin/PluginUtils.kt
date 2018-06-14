package wiresegal.wob.plugin

import de.btobastian.javacord.entities.DiscordEntity
import de.btobastian.javacord.entities.Mentionable
import de.btobastian.javacord.entities.channels.GroupChannel
import de.btobastian.javacord.entities.channels.PrivateChannel
import de.btobastian.javacord.entities.channels.TextChannel
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import wiresegal.wob.arcanum.notifyOwners
import wiresegal.wob.misc.setupControls
import wiresegal.wob.misc.setupDeletable
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter

/**
 * @author WireSegal
 * Created at 10:03 AM on 4/26/18.
 */

val gemColors = mapOf(
        "sapphire" to Color(0x0000FF),
        "smokestone" to Color(0x303030),
        "ruby" to Color(0xFF0000),
        "diamond" to Color(0xBBBBBB),
        "emerald" to Color(0x00FF00),
        "garnet" to Color(0x7B0C0B),
        "zircon" to Color(0x21B4E1),
        "amethyst" to Color(0xAA20FF),
        "topaz" to Color(0xCE7427),
        "heliodor" to Color(0xCEBF2E)
)

fun gemColorFor(last: String): Color {
    for ((gem, color) in gemColors)
        if (gem in last)
            return color
    return Color.WHITE
}

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

fun TextChannel.sendError(message: String, error: Exception) {
    val trace = StringWriter().apply { PrintWriter(this).apply { error.printStackTrace(this) } }.toString()
            .split("\n").take(5).joinToString("\n")
    sendMessage(EmbedBuilder().apply {
        setTitle("ERROR")
        setColor(Color.RED)
        setFooter(message)
        setDescription("`$trace`")
    })
    error.printStackTrace()

    val location = when {
        this is Mentionable -> "in " + this.mentionTag + "\n"
        this is PrivateChannel -> "in " + this.recipient.mentionTag + "\n"
        this is GroupChannel -> "in " + (this.name.orElse(null)?.plus("\n") ?: "") +
                this.members.joinToString { it.mentionTag } + "\n"
        else -> ""
    }

    val wireID = 77084495118868480L
    val ownerID = api.ownerId

    if (this !is PrivateChannel || (this.recipient.id != wireID || this.recipient.id != ownerID))
        notifyOwners {
            setTitle("ERROR")
            setColor(Color.RED)
            setFooter(message)
            setDescription("$location`$trace`")
        }
}
