@file:Suppress("ConstantConditionIf")

package wiresegal.wob.lint

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import wiresegal.wob.*
import java.time.Instant
import java.util.*

/**
 * @author WireSegal
 * Created at 4:45 PM on 3/28/18.
 */

var fetches = 0

var verbose = true

fun main(args: Array<String>) {
    val fromCache = System.getenv("LINTER_CACHE")?.toLowerCase()?.toBoolean() ?: run {
        logForce("Do you want to load Arcanum from the last cached data? (y/n)")
        Scanner(System.`in`).next("[ynYN]").toLowerCase() == "y"
    }

    verbose = System.getenv("LINTER_VERBOSE")?.toLowerCase()?.toBoolean() ?: run {
        logForce("Do you want the log to be verbose? (y/n)")
        Scanner(System.`in`).next("[ynYN]").toLowerCase() == "y"
    }

    logForce("========= Starting parsing...")

    var results: ArcanumDataSet? = null

    val arcanumCacheFile = fileInHome("arcanum_cache")

    if (fromCache && arcanumCacheFile.exists()) {
        log("Loading Arcanum data from cached file...")
        try {
            results = ArcanumDataSet(JsonParser().parse(arcanumCacheFile.reader()).asJsonObject)
            log("Finished loading from cache!")
        } catch (ignored: Exception) {
            log("Failed to load from cache...")
            ignored.printStackTrace()
        }
    }

    if (results == null) {
        log("Fetching entries from Arcanum.")

        log("Doing fetch ${++fetches}...")

        val data = JsonParser().parse(apiRequest("entry", "page_size" to 250)).asJsonObject

        log("Finished fetch $fetches...")

        results = ArcanumDataSet(data)
        log("Finished fetching from Arcanum!")

        log("Writing to cache file...")
        arcanumCacheFile.writeText(GSON.toJson(results))
        log("Finished writing to cache.")
    }

    logForce("========= Parsing done. Collected " + results.results.size + " results!")



    // Linting after here!


    results.printMatching("No <p> tags") {
        it.lines.any { "<p>" !in it.text }
    }
    results.printMatching("Blank speaker and/or line") {
        it.lines.any { it.getTrueText().isBlank() || it.getTrueSpeaker().isBlank() }
    }
    results.printMatching("Non-ascii") {
        it.lines.any { it.text.any { it.toInt() > 128 } || it.speaker.any { it.toInt() > 128 } }
    }
    results.printMatching("<br>") {
        it.lines.any { "<br>" in it.text }
    }
    results.printMatching("Multiple newlines") {
        it.lines.any { "\n\\s+\n".toRegex() in it.text }
    }
    results.printMatching("Double spaces after periods") {
        it.lines.any { ".  " in it.text }
    }

    logForce("========= Finished!")
}

fun logForce(data: Any?) = println((if (verbose) "[${Instant.now()}] " else "") + "$data")

fun log(data: Any?) {
    if (verbose)
        logForce(data)


}class ArcanumDataSet(data: JsonObject) {
    val results: List<Entry>

    init {
        val allEntries = mutableListOf<Entry>()
        results = allEntries

        var results = data
        results.getAsJsonArray("results").mapTo(allEntries) { GSON.fromJson(it, Entry::class.java) }
        while (results.has("next") && !results.get("next").isJsonNull) {
            log("Doing fetch ${++fetches}...")
            results = JsonParser().parse(nakedApiRequest(results.get("next").asString)).asJsonObject
            log("Finished fetch $fetches...")
            results.getAsJsonArray("results").mapTo(allEntries) { GSON.fromJson(it, Entry::class.java) }
        }
    }

    fun printMatching(name: String, matcher: (Entry) -> Boolean) {
        logForce("======= All entries which aren't in compliance with: $name")
        for (entry in results)
            if (matcher(entry))
                if (verbose)
                    log("    $entry")
                else
                    logForce(entry.id)
        logForce("======= Finished scan for $name")
    }
}
