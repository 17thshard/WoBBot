package wiresegal.wob

import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.events.message.MessageCreateEvent
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import org.jsoup.Jsoup
import wiresegal.wob.arcanum.*
import wiresegal.wob.coppermind.searchCoppermind
import wiresegal.wob.misc.setupDeletable
import wiresegal.wob.misc.util.BotRanks
import wiresegal.wob.misc.util.async
import wiresegal.wob.misc.util.checkPermissions
import wiresegal.wob.plugin.*
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.jvm.internal.components.ReflectKotlinClass
import kotlin.reflect.jvm.internal.impl.load.kotlin.header.KotlinClassHeader

/**
 * @author WireSegal
 * Created at 11:43 PM on 2/15/18.
 */

fun isKotlinObjectOrFile(clazz: Class<*>): Boolean {
    val type = ReflectKotlinClass.create(clazz)?.classHeader?.kind
    if (type == KotlinClassHeader.Kind.FILE_FACADE || type == KotlinClassHeader.Kind.MULTIFILE_CLASS || type == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART)
        return true

    return clazz.kotlin.objectInstance != null
}

fun getKotlinObjectOrNull(clazz: Class<*>): Any? {
    val type = ReflectKotlinClass.create(clazz)?.classHeader?.kind
    if (type == KotlinClassHeader.Kind.FILE_FACADE || type == KotlinClassHeader.Kind.MULTIFILE_CLASS || type == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART)
        return null

    return clazz.kotlin.objectInstance
}

fun registerAll() {
    val classes = FastClasspathScanner()
            .matchClassesWithMethodAnnotation(RegisterHandlers::class.java) { clazz, executable ->
                (Modifier.isStatic(executable.modifiers) || isKotlinObjectOrFile(clazz)) &&
                        executable.parameterCount == 0
            }
            .initializeLoadedClasses(true)
            .scan()

    for (clazz in classes.classNamesToClassRefs(classes
            .getNamesOfClassesWithMethodAnnotation(RegisterHandlers::class.java))) {
        val obj = getKotlinObjectOrNull(clazz)
        val methods = clazz.methods.filter {
            it.getAnnotation(RegisterHandlers::class.java) != null &&
                    (Modifier.isStatic(it.modifiers) || obj != null)
        }
        for (method in methods)
            method.invoke(obj)
    }


}

fun actOnCreation(it: MessageCreateEvent) {
    val message = it.message
    if (!message.userAuthor.orElseGet { api.yourself }.isBot) {
        message.content.split("(?<=.|\n)(?=!)".toRegex()).forEach { handleContent(message, it) }
    }
}

fun handleContent(message: Message, line: String) {
    val content = line.toLowerCase(Locale.ROOT).trim()
    val trimmed = content.replace("\\s".toRegex(), "")
    val noChrTrimmed = trimmed.replace("\\W".toRegex(), "")

    if (message.mentionedUsers.any { it.isYourself } && !content.startsWith("!")) {
        if (trimmed == api.yourself.mentionTag) // About
            about(message)
        else
            handleContent(message, "!wob $line")
    }

    for (handler in textHandlers)
        if (handler.matches(content, trimmed, noChrTrimmed, message)) {
            handler.handle(content, trimmed, noChrTrimmed, message)
            break
        }
}

@RegisterHandlers
fun registerBuiltinHandlers() {
    addCommand("wob") { content, trimmed, _, message ->
        if (trimmed == "!wob" || trimmed.startsWith("!wob?"))
            message.channel.sendMessage("Use `!wob \"term\"` to search, or put a WoB link in to get its text directly.")
        else {
            val allWobs = "#e(\\d+)".toRegex().findAll(content)

            for (wob in allWobs) async {
                val theWob = entryFromId(wob.groupValues[1].toInt())
                message.channel.sendMessage(embedFromContent("", theWob)).get().setupDeletable(message.author)
            }

            val terms = "[\"“]([\\w\\s,+!|&]+)[\"”]".toRegex().findAll(content).toList()
                    .flatMap {
                        it.groupValues[1]
                                .replace("([^&])!".toRegex(), "$1&!")
                                .split("[\\s,]+".toRegex())
                    }.filter { it.matches("[!+|&\\w]+".toRegex()) }
            if (terms.any()) async {
                searchWoB(message, terms)
            }
        }
    }

    addCommand("coppermind", listOf("cm")) { content, trimmed, _, message ->
        if (trimmed == "!cm" || trimmed == "!coppermind")
            message.channel.sendMessage("Use `$trimmed \"term\"` to search.")
        else {
            val terms = "[\"“]([/\\w\\s,]+)[\"”]".toRegex().findAll(content).toList()
                    .flatMap {
                        it.groupValues[1].split("[\\s,]+".toRegex())
                    }.filter { it.matches("[/\\w]+".toRegex()) }
                    .map { it.toLowerCase().capitalize() }

            if (terms.any()) async {
                searchCoppermind(message, terms)
            }
        }
    }

    addAdminCommand("wobrank", listOf("wobrankadd", "wobrankremove")) { content, trimmed, _, message ->
        val server = message.server.get()
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

    addCommand("wobhelp") { _, _, _, message ->
        message.channel.sendMessage("Use `!wob \"term\"` to search, or put a WoB link in to get its text directly.")
    }

    addCommand("wobabout") { _, _, _, message ->
        about(message)
    }

    addCommand("wobrandom") { _, _, _, message ->
        message.channel.sendMessage(embedFromContent("", randomEntry())).get().setupDeletable(message.author)
    }

    addHiddenCalloutHandler("saythewords") { _, _, _, message ->
        message.channel.sendMessage("**`Life before death.`**\n" +
                "**`Strength before weakness.`**\n" +
                "**`Journey before destination.`**")
    }

    addMultiCalloutHandler(listOf("lifebeforedeath", "strengthbeforeweakness", "journeybeforedestination")) { _, _, _, message ->
        message.channel.sendMessage("**`These Words are Accepted.`**")
    }

    addExactCalloutHandler("express my opinion, wobbot") { content, _, _, message ->
        if (message.content.toLowerCase(Locale.ROOT).trim() == content && message.checkPermissions(BotRanks.MANAGE_MESSAGES)) {
            message.delete()
            message.channel.sendMessage("ಠ_ಠ")
        }
    }

    addExactCalloutHandler("thank you, wobbot") { _, _, _, message ->
        message.channel.sendMessage(Jsoup.connect("https://cdn.discordapp.com/emojis/396521772691881987.png?v=1").ignoreContentType(true).execute().bodyStream(), "blush.png")
    }
}
