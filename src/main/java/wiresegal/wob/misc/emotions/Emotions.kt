package wiresegal.wob.misc.emotions

import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.message.Message
import java.io.InputStream
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList


/**
 * @author WireSegal
 * Created at 10:20 PM on 9/21/18.
 */


private object Reference {
    fun getResource(name: String): URL
            = Reference::class.java.getResource(name)
    fun getResourceAsStream(name: String): InputStream
            = Reference::class.java.getResourceAsStream(name)

    fun getResourceListing(directory: String): List<Path> {
        val uri = getResource(directory).toURI()
        val myPath = if (uri.scheme == "jar")
            FileSystems.newFileSystem(uri, mapOf<String, Any>()).getPath(directory)
        else
            Paths.get(uri)

        return Files.walk(myPath, 1).toList()
    }
}

val EMOTIONS = Reference.getResourceListing("/emotions")
        .map { it.fileName.toString() }
        .filter { it.endsWith(".png") }
        .map { it.removeSuffix(".png") }

fun TextChannel.sendEmotion(name: String): CompletableFuture<Message>
        = sendMessage(Reference.getResourceAsStream("/emotions/$name.png"), "$name.png")

