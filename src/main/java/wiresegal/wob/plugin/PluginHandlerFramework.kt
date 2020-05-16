package wiresegal.wob.plugin

import org.javacord.api.entity.message.Message
import wiresegal.wob.misc.util.BotRanks
import wiresegal.wob.misc.util.checkPermissions

/**
 * @author WireSegal
 * Created at 10:03 AM on 4/26/18.
 */

/**
 * Annotate any plugin register method with this annotation.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisterHandlers


typealias InputPredicate = (content: String, trimmed: String, stripped: String, message: Message) -> Boolean

typealias InputFunction = (content: String, trimmed: String, stripped: String, message: Message) -> Unit

data class TextHandler(val matches: InputPredicate,
                       val handle: InputFunction,
                       val caseSensitive: Boolean)

data class DocumentedCommand(val matches: (Message) -> Boolean,
                             val name: String)

val textHandlers = mutableListOf<TextHandler>()
val visibleCommands = mutableListOf<DocumentedCommand>()

fun addHiddenCommand(keyword: String, caseSensitive: Boolean = false, handle: InputFunction) {
    textHandlers.add(TextHandler({ it, _, _, _ -> it.startsWith("!$keyword ") || it == "!$keyword" }, handle, caseSensitive))
}

fun addHiddenCommand(keyword: String, alias: List<String>, caseSensitive: Boolean = false, handle: InputFunction) {
    addHiddenCommand(keyword, caseSensitive, handle)
    for (a in alias)
        addHiddenCommand(a, caseSensitive, handle)
}

fun addHiddenAdminCommand(keyword: String, caseSensitive: Boolean = false, handle: InputFunction) {
    textHandlers.add(TextHandler({ it, _, _, msg -> msg.checkPermissions(BotRanks.ADMIN) && (it.startsWith("!$keyword ") || it == "!$keyword") }, handle, caseSensitive))
}

fun addHiddenAdminCommand(keyword: String, alias: List<String>, caseSensitive: Boolean = false, handle: InputFunction) {
    addHiddenAdminCommand(keyword, caseSensitive, handle)
    for (a in alias)
        addHiddenAdminCommand(a, caseSensitive, handle)
}

fun addCommand(keyword: String, caseSensitive: Boolean = false, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ true }, "!$keyword"))
    addHiddenCommand(keyword, caseSensitive, handle)
}

fun addCommand(keyword: String, alias: List<String>, caseSensitive: Boolean = false, handle: InputFunction) {
    addCommand(keyword, caseSensitive, handle)
    for (a in alias)
        addHiddenCommand(a, caseSensitive, handle)
}

fun addAdminCommand(keyword: String, caseSensitive: Boolean = false, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ it.checkPermissions(BotRanks.ADMIN) }, "!$keyword (Admin only)"))
    addHiddenAdminCommand(keyword, caseSensitive, handle)
}

fun addAdminCommand(keyword: String, alias: List<String>, caseSensitive: Boolean = false, handle: InputFunction) {
    addAdminCommand(keyword, caseSensitive, handle)
    for (a in alias)
        addHiddenAdminCommand(a, caseSensitive, handle)
}

fun addSoftHiddenCommand(keyword: String, caseSensitive: Boolean = false, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ it.checkPermissions(BotRanks.ADMIN) }, "!$keyword"))
    addHiddenCommand(keyword, caseSensitive, handle)
}

fun addSoftHiddenCommand(keyword: String, alias: List<String>, caseSensitive: Boolean = false, handle: InputFunction) {
    addSoftHiddenCommand(keyword, caseSensitive, handle)
    for (a in alias)
        addHiddenCommand(a, caseSensitive, handle)
}

fun addCalloutHandler(callout: String, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ true }, callout))
    val call = callout.replace("\\W".toRegex(), "").toLowerCase()
    textHandlers.add(TextHandler({ _, _, it, _ -> it.startsWith(call) }, handle, false))
}

fun addHiddenCalloutHandler(callout: String, handle: InputFunction) {
    val call = callout.replace("\\W".toRegex(), "").toLowerCase()
    textHandlers.add(TextHandler({ _, _, it, _ -> it.startsWith(call) }, handle, false))
}

fun addMultiCalloutHandler(callouts: List<String>, handle: InputFunction) {
    val calls = callouts.map { it.replace("\\W".toRegex(), "").toLowerCase() }
    textHandlers.add(TextHandler({ _, _, str, _ -> calls.all { it in str } }, handle, false))
}

fun addExactCalloutHandler(callout: String, handle: InputFunction) {
    textHandlers.add(TextHandler({ it, _, _, _ -> it == callout }, handle, false))
}
