package wiresegal.wob.plugin

import org.javacord.api.entity.DiscordEntity
import org.javacord.api.entity.Mentionable
import org.javacord.api.entity.user.User
import org.javacord.api.entity.channel.GroupChannel
import org.javacord.api.entity.channel.PrivateChannel
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.Messageable
import org.javacord.api.entity.message.embed.EmbedBuilder
import wiresegal.wob.*
import wiresegal.wob.arcanum.DESCRIPTION_LIMIT
import wiresegal.wob.arcanum.notifyOwners
import wiresegal.wob.arcanum.sendTo
import wiresegal.wob.misc.setupControls
import wiresegal.wob.misc.setupDeletable
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter

/**
 * @author WireSegal
 * Created at 10:03 AM on 4/26/18.
 */

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

    sendMessage(embed).setupDeletable(requester).setupControls(requester, index, false, embeds)
}

fun TextChannel.sendRandomEmbed(requester: DiscordEntity, title: String, messages: Array<Pair<String, Pair<String, Color>>>) {
    val embeds = messages.embeds(title)

    val index = (Math.random() * embeds.size).toInt()
    val embed = embeds[index]

    sendMessage(embed).setupDeletable(requester).setupControls(requester, index, false, embeds)
}

fun Message.sendMinorError(message: String) {
    channel.sendMinorError(message)
}

fun Message.sendError(message: String, error: Throwable) {
    channel.sendError(this.content, message, error)
}

fun String.getClassMarkdown(clazzName: String, realName: String, line: Int = -1): String {
    if (!origin.endsWith(".git") || fullCommit == null)
        return this

    val me = EmbeddedInfo::class.java
    try {
        val clazz = Class.forName(clazzName, true, me.classLoader)

        if (clazz.protectionDomain.codeSource != me.protectionDomain.codeSource)
            return this

        val split = clazzName.split('.').dropLast(1).joinToString("/")

        val baseUrl = origin.removeSuffix(".git")
        var url = "$baseUrl/blob/$fullCommit/src/main/java/$split/$realName"
        if (line >= 0)
            url += "#L$line"

        return "[$this]($url)"

    } catch (e: ClassNotFoundException) {
        return this
    }
}

fun Messageable.sendMinorError(message: String) {
    sendMessage(EmbedBuilder().apply {
        setTitle("Failed")
        setColor(Color.RED)
        setDescription(message)
    })
}

fun Messageable.sendError(replyingTo: String, message: String, error: Throwable) {
    val fullTrace = StringWriter().apply { error.printStackTrace(PrintWriter(this)) }.toString()

    val location = when (this) {
        is Mentionable -> "in " + this.mentionTag + "\n"
        is PrivateChannel -> "in " + (this.recipient.orElse(null)?.mentionTag?.plus("\n") ?: "")
        is GroupChannel -> "in " + (this.name.orElse(null)?.plus("\n") ?: "") +
                this.members.joinToString { it.mentionTag } + "\n"
        else -> ""
    }

    var trace = "`$error`"

    for (line in error.stackTrace) {
        val lineString = line.run {
            val name = fileName
            "\n\u2003`at $className.$methodName`(" + (if (isNativeMethod)
                "Native Method"
            else if (name != null && lineNumber >= 0)
                "$name:$lineNumber".getClassMarkdown(className, name, lineNumber)
            else
                name?.getClassMarkdown(className, name) ?: "Unknown Source") + ")"
        }

        if (location.length + lineString.length + trace.length < DESCRIPTION_LIMIT)
            trace += lineString
        else break
    }

    sendMessage(EmbedBuilder().apply {
        setTitle("ERROR")
        setColor(Color.RED)
        setFooter(message)
        setDescription(trace)
    })

    error.printStackTrace()

    val wireID = 77084495118868480L
    val ownerID = api.ownerId

    val myId = (this as? User)?.id ?: (this as? PrivateChannel)?.recipient?.orElse(null)?.id

    if ((myId == wireID && wobCommand == "wob") || myId == ownerID) {
        sendTo(replyingTo, "message")
        sendTo(fullTrace, "error")
    } else {
        notifyOwners {
            setTitle("ERROR")
            setColor(Color.RED)
            setFooter(message)
            setDescription(location + trace)
        }

        notifyOwners(replyingTo, "message")
        notifyOwners(fullTrace, "error")
    }
}
