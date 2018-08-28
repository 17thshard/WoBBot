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
val urlTarget = System.getenv("ARCANUM_URL") ?: "https://wob.coppermind.net"
val wikiTarget = System.getenv("WIKI_URL") ?: "coppermind.net"
val wikiCommand = System.getenv("WIKI_COMMAND") ?: "coppermind|cm"
val wobCommand = System.getenv("WOB_COMMAND") ?: "wob"
val iconUrl = System.getenv("ARCANUM_ICON") ?: "https://cdn.discordapp.com/emojis/373082865073913859.png?v=1"
val wikiIconUrl = System.getenv("WIKI_ICON") ?: "https://cdn.discordapp.com/emojis/432391749550342145.png?v=1"
val embedColor = Color((System.getenv("ARCANUM_COLOR") ?: "003A52").toInt(16))
val wikiEmbedColor = Color((System.getenv("ARCANUM_COLOR") ?: "CB6D51").toInt(16))


val version: String? by lazy {
    val property = gitProperties.getProperty("git.commit.time")
    if (property == null || property.contains("$")) null else property
}
val commitId: String? by lazy {
    val property = gitProperties.getProperty("git.commit.id.abbrev")
    if (property == null || property.contains("$")) null else property
}
val commitDesc: String? by lazy {
    val property = gitProperties.getProperty("git.commit.message.short")
    if (property == null || property.contains("$")) null else property
}
val committer: String? by lazy {
    val property = gitProperties.getProperty("git.commit.user.name")
    if (property == null || property.contains("$")) null else property
}
