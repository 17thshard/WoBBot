package wiresegal.wob

import de.btobastian.javacord.entities.DiscordEntity
import de.btobastian.javacord.entities.channels.TextChannel
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.events.message.MessageCreateEvent
import org.jsoup.Jsoup
import java.awt.Color
import java.util.*

/**
 * @author WireSegal
 * Created at 11:43 PM on 2/15/18.
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

    sendMessage(embed).get().setupDeletable(requester).setupControls(requester, index, embeds)
}

fun TextChannel.sendRandomEmbed(requester: DiscordEntity, title: String, messages: Array<Pair<String, Pair<String, Color>>>) {
    val embeds = messages.embeds(title)

    val index = (Math.random() * embeds.size).toInt()
    val embed = embeds[index]

    sendMessage(embed).get().setupDeletable(requester).setupControls(requester, index, embeds)
}

fun actOnCreation(it: MessageCreateEvent) {
    val message = it.message
    if (!message.userAuthor.orElseGet { api.yourself }.isBot) {
        message.content.split("\n").forEach { handleContent(message, it) }
    }
}

fun handleContent(message: Message, line: String) {
    val content = line.toLowerCase(Locale.ROOT).trim()
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
            val allWobs = "#e(\\d+)".toRegex().findAll(content)

            for (wob in allWobs) async {
                val theWob = entryFromId(wob.groupValues[1].toInt())
                message.channel.sendMessage(embedFromContent("", theWob)).get().setupDeletable(message.author)
            }
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
    } else if (trimmed == "!wobabout")
        about(message)
    else if (trimmed == "!wobrandom") async {
        message.channel.sendMessage(embedFromContent("", randomEntry())).get().setupDeletable(message.author)
    } else if (trimmed.startsWith("!wobrank") && message.checkPermissions(BotRanks.ADMIN)) {
        val serverHolder = message.server
        if (serverHolder.isPresent) {
            val server = serverHolder.get()
            val add = trimmed.startsWith("!wobrankadd")
            val remove = trimmed.startsWith("!wobrankremove")

            if (add || remove) {
                val roleName = if (add)
                    trimmed.removePrefix("!wobrankadd")
                else
                    trimmed.removePrefix("!wobrankremove")
                val role = server.roles.firstOrNull { it.name.toLowerCase().replace("\\s+".toRegex(), "") == roleName }
                if (role != null) {
                    val list = permissions.getOrElse(server.id) { listOf() }
                    if (add) {
                        if (role.id in list)
                            message.channel.sendMessage("Role ${role.name} already had reaction control.")
                        else {
                            val newList = list.toMutableSet().apply { add(role.id) }.toList()
                            permissions[server.id] = newList
                            message.channel.sendMessage("Gave the role ${role.name} reaction control.")
                        }
                    } else {
                        if (role.id !in list)
                            message.channel.sendMessage("Role ${role.name} didn't have reaction control.")
                        else {
                            val newList = list.toMutableSet().apply { remove(role.id) }.toList()
                            if (newList.isEmpty())
                                permissions.remove(server.id)
                            else
                                permissions[server.id] = newList
                            message.channel.sendMessage("Removed the role ${role.name} from reaction control.")
                        }
                    }
                }
            } else {
                message.channel.sendMessage(EmbedBuilder().apply {
                    setTitle("WoB Rank Details _(Admin Only)_")
                    setColor(arcanumColor)

                    var roles = ""

                    val allRoles = permissions[server.id]
                    if (allRoles != null) {
                        val rolesInServer = server.roles.filter { it.id in allRoles }
                        if (rolesInServer.isNotEmpty()) {
                            roles += "\n\nAllowed: \n"
                            roles += rolesInServer.joinToString("\n") { it.mentionTag }
                        }
                    }
                    setDescription("Anyone with one of the whitelisted roles may use reactions as though they were the one who sent the message.\n\n" +
                            "Usage:\n" +
                            "* !wobrank add <Role Name>\n" +
                            "* !wobrank remove <Role Name>" + roles)
                })
            }
        }
    } else if (noChrTrimmed.startsWith("saythewords"))
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
    else if (content == "express my opinion, wobbot" && message.content == line) {
        if (message.checkPermissions(BotRanks.MANAGE_MESSAGES)) {
            message.delete()
            message.channel.sendMessage("ಠ_ಠ")
        }
    } else if (content == "thank you, wobbot")
        message.channel.sendMessage(Jsoup.connect("https://cdn.discordapp.com/emojis/396521772691881987.png?v=1").ignoreContentType(true).execute().bodyStream(), "blush.png")
}
