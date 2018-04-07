package wiresegal.wob

import de.btobastian.javacord.DiscordApi
import de.btobastian.javacord.DiscordApiBuilder
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.utils.logging.LoggerUtil
import java.awt.Color
import java.util.*

/**
 * @author WireSegal
 * Created at 9:12 PM on 1/25/18.
 */

val arcanumColor = Color(0x003A52)
const val iconUrl = "https://cdn.discordapp.com/emojis/373082865073913859.png?v=1"

private val apiProvider = DiscordApiBuilder().setToken(token).login()

val api: DiscordApi by lazy { apiProvider.join() }

data class EmbeddedInfo(val user: Long, val channel: Long, val index: Int, val embeds: List<EmbedBuilder>)

val messagesWithEmbedLists = SavedTypedMap<Long, EmbeddedInfo>(fileInHome("wob_bot_messages"),
        { it.toString() }, { it.toLongOrNull() ?: 0L },
        { _, value -> "${value.user}:${value.channel}" }, { key, value ->
    val values = value.split(":")
    if (values.size > 1) {
        val uid = values[0].toLongOrNull()
        val channel = values[1].toLongOrNull()
        if (uid != null && channel != null) async {
            api.servers.forEach {
                it.textChannels.filter { it.id == channel }.forEach { it.getMessageById(key).whenComplete { message, _ ->
                    message.finalizeMessage(uid)
                } }
            }
        }
    }
    null }, false)
val messageToAuthor = SavedTypedMap(fileInHome("wob_bot_deletable"), { it.toString() }, { it.toLongOrNull() ?: 0L },
        { _, value -> value.toString() }, { _, value -> value.toLongOrNull() ?: 0L })
val permissions = SavedTypedMap(fileInHome("wob_bot_permissions"), { it.toString() }, { it.toLongOrNull() ?: 0L },
        { _, value -> value.joinToString(",") }, { _, value -> value.split(",").mapNotNull { it.toLongOrNull() } })


val gitProperties: Properties by lazy {
    val properties = Properties()

    try {
        val propertyStream = EmbeddedInfo::class.java.getResourceAsStream("/git.properties")
        properties.load(propertyStream)
    } catch (e: Exception) {
        // NO-OP
    }

    properties
}

val version: String? by lazy {
    val property = gitProperties.getProperty("git.commit.time")
    if (property.contains("$")) null else property
}
val commitId: String? by lazy {
    val property = gitProperties.getProperty("git.commit.id.abbrev")
    if (property.contains("$")) null else property
}
val commitDesc: String? by lazy {
    val property = gitProperties.getProperty("git.commit.message.short")
    if (property.contains("$")) null else property
}
val committer: String? by lazy {
    val property = gitProperties.getProperty("git.commit.user.name")
    if (property.contains("$")) null else property
}

fun main(args: Array<String>) {
    LoggerUtil.getLogger("WoB").debug("Running version built at $version")

    notifyOwners()

    api.addMessageCreateListener(::actOnCreation)
    api.addReactionAddListener(::actOnReaction)
}
