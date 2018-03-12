package wiresegal.wob

/**
 * @author WireSegal
 * Created at 12:03 AM on 1/26/18.
 */

val token: String = System.getenv("DISCORD_TOKEN")
val arcanumToken: String? = System.getenv("ARCANUM_TOKEN")
val urlTarget = System.getenv("ARCANUM_URL") ?: "https://wob.coppermind.net"
