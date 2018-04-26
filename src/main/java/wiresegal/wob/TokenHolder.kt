package wiresegal.wob

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
