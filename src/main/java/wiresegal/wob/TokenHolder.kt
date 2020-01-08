package wiresegal.wob

import java.awt.Color
import java.util.*

/**
 * @author WireSegal
 * Created at 12:03 AM on 1/26/18.
 */

private val gitProperties: Properties by lazy {
    val properties = Properties()

    try {
        val propertyStream = EmbeddedInfo::class.java.getResourceAsStream("/git.properties")
        properties.load(propertyStream)
    } catch (e: Exception) {
        // NO-OP
    }

    properties
}

val token: String? = System.getenv("DISCORD_TOKEN")
val arcanumToken: String? = System.getenv("ARCANUM_TOKEN")
val homepageTarget = System.getenv("BRANDONSANDERSON_URL") ?: "https://www.brandonsanderson.com/"
val urlTarget = System.getenv("ARCANUM_URL") ?: "https://wob.coppermind.net"
val wikiTarget = System.getenv("WIKI_URL") ?: "coppermind.net"
val wikiCommand = System.getenv("WIKI_COMMAND") ?: "coppermind|cm"
val wobCommand = System.getenv("WOB_COMMAND") ?: "wob"
val iconUrl = System.getenv("ARCANUM_ICON") ?: "https://cdn.discordapp.com/emojis/373082865073913859.png?v=1"
val wikiIconUrl = System.getenv("WIKI_ICON") ?: "https://cdn.discordapp.com/emojis/432391749550342145.png?v=1"
val embedColor = Color((System.getenv("ARCANUM_COLOR") ?: "003A52").toInt(16))
val wikiEmbedColor = Color((System.getenv("WIKI_COLOR") ?: "CB6D51").toInt(16))

const val progressCachePersistence: Long = 10 * 60 * 1000 // 10 minutes persistence for the cache

fun propertyGetter(name: String) = lazy<String?> {
    val property = gitProperties.getProperty(name)
    if (property == null || property.contains("$")) null else property
}

fun propertyGetter(name: String, defaultValue: String) = lazy<String> {
    val property = gitProperties.getProperty(name)
    if (property == null || property.contains("$")) defaultValue else property
}

val version by propertyGetter("git.commit.time")
val fullCommit by propertyGetter("git.commit.id")
val commitId by propertyGetter("git.commit.id.abbrev")
val commitDesc by propertyGetter("git.commit.message.short")
val committer by propertyGetter("git.commit.user.name")
val origin by propertyGetter("git.remote.origin.url", "https://github.com/Palanaeum/WoBBot.git")
