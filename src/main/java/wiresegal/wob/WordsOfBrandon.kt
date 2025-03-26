package wiresegal.wob

import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.intent.Intent
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.core.util.logging.LoggerUtil
import wiresegal.wob.arcanum.notifyOwners
import wiresegal.wob.misc.actOnReaction
import wiresegal.wob.misc.finalizeMessage
import wiresegal.wob.misc.util.SavedTypedMap
import wiresegal.wob.misc.util.async
import wiresegal.wob.misc.util.fileInHome
import java.time.Instant

/**
 * @author WireSegal
 * Created at 9:12 PM on 1/25/18.
 */

private val apiProvider = DiscordApiBuilder()
    .setToken(token)
    .setIntents(
        Intent.GUILDS,
        Intent.GUILD_MEMBERS,
        Intent.GUILD_MESSAGES,
        Intent.GUILD_MESSAGE_REACTIONS,
        Intent.DIRECT_MESSAGES,
        Intent.DIRECT_MESSAGE_REACTIONS
    )
    .login()

val api: DiscordApi by lazy { apiProvider.join() }

data class EmbeddedInfo(val user: Long, val channel: Long, val index: Int, val shouldHide: Boolean, val embeds: List<EmbedBuilder>)

val messagesWithEmbedLists by lazy {
    SavedTypedMap<Long, EmbeddedInfo>(fileInHome("wob_bot_messages"),
            { it.toString() }, { it.toLongOrNull() ?: 0L },
            { _, value -> "${value.user}:${value.channel}" }, { key, value ->
        val values = value.split(":")
        if (values.size > 1) {
            val uid = values[0].toLongOrNull()
            val channel = values[1].toLongOrNull()
            if (uid != null && channel != null) async {
                api.servers.forEach { server ->
                    server.textChannels.filter { it.id == channel }.forEach {
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

fun main() {
    LoggerUtil.getLogger("WoB").debug("Running version built at $version")

    notifyOwners(launch)

    registerAll()

    messagesWithEmbedLists.clear()

    api.addMessageCreateListener(::actOnCreation)
    api.addReactionAddListener(::actOnReaction)
}
