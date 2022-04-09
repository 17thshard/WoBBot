package wiresegal.wob.misc.util

import org.javacord.api.entity.user.User
import org.javacord.api.entity.channel.ServerChannel
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.MessageAuthor
import org.javacord.api.entity.permission.PermissionState
import org.javacord.api.entity.permission.PermissionType
import wiresegal.wob.permissions

/**
 * @author WireSegal
 * Created at 9:41 PM on 2/15/18.
 */

enum class BotRanks {
    ADMIN,
    MANAGE_MESSAGES,
    USER
}

fun Message.checkPermissions(level: BotRanks) = checkPermissions(null, level)
fun Message.checkPermissions(id: Long?, level: BotRanks) = author.checkPermissions(id, channel, level)


fun MessageAuthor.checkPermissions(channel: TextChannel, level: BotRanks) = checkPermissions(null, channel, level)
fun MessageAuthor.checkPermissions(id: Long?, channel: TextChannel, level: BotRanks) = if (isUser) asUser().get().checkPermissions(id, channel, level) else false

fun User.checkPermissions(channel: TextChannel, level: BotRanks) = checkPermissions(null, channel, level)

fun User.checkPermissions(id: Long?, channel: TextChannel, level: BotRanks): Boolean {

    if (this.isBotOwner) return true
    if (channel !is ServerChannel) return true
    val server = try {
        channel.asServerTextChannel().get()
    } catch (err: NoSuchElementException) {
        return false
    }
    val localPerms = server.getEffectivePermissions(this)
    val roles = server.server.getRoles(this)
    val channelId = server.server.id

    val managerInfo = permissions[channelId]
    val isManager = managerInfo != null && roles.any { it.id in managerInfo }
    val isAdmin = localPerms.getState(PermissionType.ADMINISTRATOR) == PermissionState.ALLOWED
    val isImplicitManager = localPerms.getState(PermissionType.MANAGE_MESSAGES) == PermissionState.ALLOWED
    val isCreator = id == this.id

    return isAdmin ||
            (level != BotRanks.ADMIN &&
                    (isManager || isImplicitManager ||
                            (level == BotRanks.USER && isCreator)))
}
