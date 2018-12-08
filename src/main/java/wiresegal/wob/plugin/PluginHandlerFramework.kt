package wiresegal.wob.plugin

import de.btobastian.javacord.entities.message.Message
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
                       val handle: InputFunction)

data class DocumentedCommand(val matches: (Message) -> Boolean,
                             val name: String)

val textHandlers = mutableListOf<TextHandler>()
val visibleCommands = mutableListOf<DocumentedCommand>()

fun addHiddenCommand(keyword: String, handle: InputFunction) {
    textHandlers.add(TextHandler({ it, _, _, _ -> it.startsWith("!$keyword ") || it == "!$keyword" }, handle))
}

fun addHiddenCommand(keyword: String, alias: List<String>, handle: InputFunction) {
    addHiddenCommand(keyword, handle)
    for (a in alias)
        addHiddenCommand(a, handle)
}

fun addHiddenAdminCommand(keyword: String, handle: InputFunction) {
    textHandlers.add(TextHandler({ it, _, _, msg -> msg.checkPermissions(BotRanks.ADMIN) && (it.startsWith("!$keyword ") || it == "!$keyword") }, handle))
}

fun addHiddenAdminCommand(keyword: String, alias: List<String>, handle: InputFunction) {
    addHiddenAdminCommand(keyword, handle)
    for (a in alias)
        addHiddenAdminCommand(a, handle)
}

fun addCommand(keyword: String, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ true }, "!$keyword"))
    addHiddenCommand(keyword, handle)
}

fun addCommand(keyword: String, alias: List<String>, handle: InputFunction) {
    addCommand(keyword, handle)
    for (a in alias)
        addHiddenCommand(a, handle)
}

fun addAdminCommand(keyword: String, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ it.checkPermissions(BotRanks.ADMIN) }, "!$keyword (Admin only)"))
    addHiddenAdminCommand(keyword, handle)
}

fun addAdminCommand(keyword: String, alias: List<String>, handle: InputFunction) {
    addAdminCommand(keyword, handle)
    for (a in alias)
        addHiddenAdminCommand(a, handle)
}

fun addSoftHiddenCommand(keyword: String, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ it.checkPermissions(BotRanks.ADMIN) }, "!$keyword"))
    addHiddenCommand(keyword, handle)
}

fun addSoftHiddenCommand(keyword: String, alias: List<String>, handle: InputFunction) {
    addSoftHiddenCommand(keyword, handle)
    for (a in alias)
        addHiddenCommand(a, handle)
}

fun addCalloutHandler(callout: String, handle: InputFunction) {
    visibleCommands.add(DocumentedCommand({ true }, callout))
    val call = callout.replace("\\W".toRegex(), "").toLowerCase()
    textHandlers.add(TextHandler({ _, _, it, _ -> it.startsWith(call) }, handle))
}

fun addHiddenCalloutHandler(callout: String, handle: InputFunction) {
    val call = callout.replace("\\W".toRegex(), "").toLowerCase()
    textHandlers.add(TextHandler({ _, _, it, _ -> it.startsWith(call) }, handle))
}

fun addMultiCalloutHandler(callouts: List<String>, handle: InputFunction) {
    val calls = callouts.map { it.replace("\\W".toRegex(), "").toLowerCase() }
    textHandlers.add(TextHandler({ _, _, str, _ -> calls.all { it in str } }, handle))
}

fun addExactCalloutHandler(callout: String, handle: InputFunction) {
    textHandlers.add(TextHandler({ it, _, _, _ -> it == callout }, handle))
}
