package wiresegal.wob.misc

import de.btobastian.javacord.entities.DiscordEntity
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.events.message.reaction.ReactionAddEvent
import wiresegal.wob.EmbeddedInfo
import wiresegal.wob.messageToAuthor
import wiresegal.wob.messagesWithEmbedLists
import wiresegal.wob.misc.util.BotRanks
import wiresegal.wob.misc.util.checkPermissions

/**
 * @author WireSegal
 * Created at 11:41 PM on 2/15/18.
 */

const val arrowLeft = "⬅"
const val arrowRight = "➡"
const val done = "\u23F9"
const val last = "⏭"
const val first = "⏮"
const val jumpLeft = "⏪"
const val jumpRight = "⏩"
const val no = "❌"

val validReactions = listOf(arrowLeft, arrowRight, done, last, first, jumpLeft, jumpRight, no)

fun updateMessageWithJump(jump: Int, message: Message, entry: EmbeddedInfo) {
    val (uid, channel, index, embeds) = entry
    val newIndex = Math.min(Math.max(index + jump, 0), embeds.size - 1)
    if (index != newIndex) {
        val newEmbed = embeds[newIndex]
        message.edit(newEmbed)
        messagesWithEmbedLists[message.id] = EmbeddedInfo(uid, channel, newIndex, embeds)
    }
}

fun Message.setupDeletable(author: DiscordEntity) = setupDeletable(author.id)

fun Message.setupDeletable(id: Long): Message {
    messageToAuthor[this.id] = id
    addReaction(no)
    return this
}

fun Message.setupControls(requester: DiscordEntity, index: Int, embeds: List<EmbedBuilder>): Message {
    if (embeds.size > 2)
        addReaction(first)
    if (embeds.size > 10)
        addReaction(jumpLeft)
    addReaction(arrowLeft)
    addReaction(done)
    addReaction(arrowRight)
    if (embeds.size > 10)
        addReaction(jumpRight)
    if (embeds.size > 2)
        addReaction(last)

    messagesWithEmbedLists[id] = EmbeddedInfo(requester.id, channel.id, index, embeds)
    return this
}

fun Message.finalizeMessage(uid: Long) {
    val finalEmbed = embeds.first().toBuilder()
    finalEmbed.setTitle(finalEmbed.toJsonNode()["title"].asText().replace(".*\n".toRegex(), ""))
    edit(finalEmbed)
    messagesWithEmbedLists.remove(id)
    removeAllReactions().whenComplete { _, _ -> setupDeletable(uid) }
}

fun actOnReaction(it: ReactionAddEvent) {
    if (it.message.isPresent && it.reaction.isPresent) {
        val message = it.message.get()
        val reaction = it.reaction.get()
        if (message.author.isYourself && reaction.emoji.isUnicodeEmoji) {
            val unicode = reaction.emoji.asUnicodeEmoji().get()
            if (!it.user.isBot && unicode in validReactions) {
                if (unicode == no) {
                    if (it.user.checkPermissions(messageToAuthor[message.id], message.channel, BotRanks.USER))
                        it.deleteMessage()
                    else
                        it.removeReaction()
                } else {
                    val messageValue = messagesWithEmbedLists[message.id]
                    if (messageValue != null) {
                        val (uid, _, _, embeds) = messageValue
                        if (it.user.checkPermissions(uid, message.channel, BotRanks.USER)) {
                            when (unicode) {
                                arrowLeft -> updateMessageWithJump(-1, message, messageValue)
                                jumpLeft -> updateMessageWithJump(-10, message, messageValue)
                                first -> updateMessageWithJump(-embeds.size, message, messageValue)
                                arrowRight -> updateMessageWithJump(1, message, messageValue)
                                jumpRight -> updateMessageWithJump(10, message, messageValue)
                                last -> updateMessageWithJump(embeds.size, message, messageValue)
                                done -> message.finalizeMessage(uid)
                            }
                        }
                        it.removeReaction()
                    }
                }
            }
        }
    }
}
