package wiresegal.wob

import de.btobastian.javacord.DiscordApi
import de.btobastian.javacord.DiscordApiBuilder
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.utils.logging.LoggerUtil
import wiresegal.wob.arcanum.applyToOwners
import wiresegal.wob.arcanum.notifyOwners
import wiresegal.wob.misc.actOnReaction
import wiresegal.wob.misc.finalizeMessage
import wiresegal.wob.misc.util.SavedTypedMap
import wiresegal.wob.misc.util.async
import wiresegal.wob.misc.util.fileInHome
import wiresegal.wob.plugin.sendError
import java.time.Instant

/**
 * @author WireSegal
 * Created at 9:12 PM on 1/25/18.
 */

private val apiProvider = DiscordApiBuilder().setToken(token).login()

val api: DiscordApi by lazy { apiProvider.join() }

data class EmbeddedInfo(val user: Long, val channel: Long, val index: Int, val embeds: List<EmbedBuilder>)

val messagesWithEmbedLists by lazy {
    SavedTypedMap<Long, EmbeddedInfo>(fileInHome("wob_bot_messages"),
            { it.toString() }, { it.toLongOrNull() ?: 0L },
            { _, value -> "${value.user}:${value.channel}" }, { key, value ->
        val values = value.split(":")
        if (values.size > 1) {
            val uid = values[0].toLongOrNull()
            val channel = values[1].toLongOrNull()
            if (uid != null && channel != null) async {
                api.servers.forEach {
                    it.textChannels.filter { it.id == channel }.forEach {
                        it.getMessageById(key).whenComplete { message, _ ->
                            message.finalizeMessage(uid)
                        }
                    }
                }
            }
        }
        null
    }, false, loadLimit = 50)
}
val messageToAuthor = SavedTypedMap(fileInHome("wob_bot_deletable"),
        { it.toString() },
        { it.toLongOrNull() ?: 0L },
        { _, value -> value.toString() },
        { _, value -> value.toLongOrNull() ?: 0L })
val permissions = SavedTypedMap(fileInHome("wob_bot_permissions"),
        { it.toString() },
        { it.toLongOrNull() ?: 0L },
        { _, value -> value.joinToString(",") },
        { _, value -> value.split(",").mapNotNull { it.toLongOrNull() } })

val launch: Instant = Instant.now()

fun main(args: Array<String>) {
    LoggerUtil.getLogger("WoB").debug("Running version built at $version")

    notifyOwners(launch)
    applyToOwners {
        sendError("nothin", "whoa, error time", RuntimeException())
    }

    registerAll()

    messagesWithEmbedLists.clear()

    api.addMessageCreateListener(::actOnCreation)
    api.addReactionAddListener(::actOnReaction)
}
