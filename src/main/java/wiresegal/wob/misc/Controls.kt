package wiresegal.wob.misc

import io.github.vjames19.futures.jdk8.zip
import org.javacord.api.entity.DiscordEntity
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.reaction.ReactionAddEvent
import wiresegal.wob.EmbeddedInfo
import wiresegal.wob.messageToAuthor
import wiresegal.wob.messagesWithEmbedLists
import wiresegal.wob.misc.util.BotRanks
import wiresegal.wob.misc.util.checkPermissions
import wiresegal.wob.misc.util.spoilerTag
import wiresegal.wob.misc.util.toJsonNode
import wiresegal.wob.arcanum.embedFromContent
import java.util.concurrent.CompletableFuture

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
    val (uid, channel, index, shouldHide, embeds) = entry
    val newIndex = (index + jump).coerceIn(embeds.indices)
    if (index != newIndex) {
        val newEmbed = embeds[newIndex]
        message.edit(newEmbed)
        messagesWithEmbedLists[message.id] = EmbeddedInfo(uid, channel, newIndex, shouldHide, embeds)
    }
}

fun <T> CompletableFuture<out T>.then(code: (T) -> Unit): CompletableFuture<T> = thenApply { code(it); it }
fun <T> CompletableFuture<T>.catch(code: (Throwable) -> Unit): CompletableFuture<T?> = exceptionally { code(it); null }

fun CompletableFuture<out Message>.setupDeletable(author: DiscordEntity) = setupDeletable(author.id)

fun CompletableFuture<out Message>.setupDeletable(id: Long): CompletableFuture<Message>
        = then { it.setupDeletable(id) }

fun Message.setupDeletable(author: DiscordEntity) = setupDeletable(author.id)

fun Message.setupDeletable(id: Long): Message {
    messageToAuthor[this.id] = id
    addReaction(no)
    return this
}

fun CompletableFuture<out Message>.setupControls(requester: DiscordEntity, index: Int, shouldHide: Boolean = false, embeds: List<EmbedBuilder>): CompletableFuture<Message>
        = then { it.setupControls(requester, index, shouldHide, embeds) }

fun Message.setupControls(requester: DiscordEntity, index: Int, shouldHide: Boolean = false, embeds: List<EmbedBuilder>): Message {
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

    messagesWithEmbedLists[id] = EmbeddedInfo(requester.id, channel.id, index, shouldHide, embeds)
    return this
}

fun Message.finalizeMessage(uid: Long, shouldHide: Boolean = false) {
    val finalEmbed = embeds.first().toBuilder()
    finalEmbed.setTitle(finalEmbed.toJsonNode()["title"].asText().replace(".*\n|\\|\\|".toRegex(), "").spoilerTag(shouldHide))
    edit(finalEmbed)
    messagesWithEmbedLists.remove(id)
    removeAllReactions().whenComplete { _, _ -> setupDeletable(uid) }
}

fun actOnReaction(it: ReactionAddEvent) {
    it.requestMessage().zip(it.requestUser()).then { (message, user) ->
        if (it.reaction.isPresent) {
            val reaction = it.reaction.get()
            if (message.author.isYourself && reaction.emoji.isUnicodeEmoji) {
                val unicode = reaction.emoji.asUnicodeEmoji().get()
                if (!user.isBot && unicode in validReactions) {
                    if (unicode == no) {
                        if (user.checkPermissions(messageToAuthor[message.id], message.channel, BotRanks.USER))
                            it.deleteMessage()
                        else
                            it.removeReaction()
                    } else {
                        val messageValue = messagesWithEmbedLists[message.id]
                        if (messageValue != null) {
                            val (uid, _, _, shouldHide, embeds) = messageValue
                            if (user.checkPermissions(uid, message.channel, BotRanks.USER)) {
                                when (unicode) {
                                    arrowLeft -> updateMessageWithJump(-1, message, messageValue)
                                    jumpLeft -> updateMessageWithJump(-10, message, messageValue)
                                    first -> updateMessageWithJump(-embeds.size, message, messageValue)
                                    arrowRight -> updateMessageWithJump(1, message, messageValue)
                                    jumpRight -> updateMessageWithJump(10, message, messageValue)
                                    last -> updateMessageWithJump(embeds.size, message, messageValue)
                                    done -> message.finalizeMessage(uid, shouldHide)
                                }
                            }
                            it.removeReaction()
                        }
                    }
                }
            }
        }
    }
}
