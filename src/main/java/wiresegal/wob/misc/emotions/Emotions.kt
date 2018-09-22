package wiresegal.wob.misc.emotions

import de.btobastian.javacord.entities.channels.TextChannel
import de.btobastian.javacord.entities.message.Message
import java.util.concurrent.CompletableFuture

/**
 * @author WireSegal
 * Created at 10:20 PM on 9/21/18.
 */


private object Reference

val EMOTIONS = listOf("blush", "shrug", "love", "yes", "no", "anger", "wink", "irritated", "upset", "facepalm", "fear", "unamused", "eyeroll", "lul", "happy", "cry", "sad", "content", "hug")

fun TextChannel.sendEmotion(name: String): CompletableFuture<Message>
        = sendMessage(Reference::class.java.getResourceAsStream("/$name.png"), "$name.png")

