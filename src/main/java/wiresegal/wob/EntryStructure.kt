package wiresegal.wob

import com.google.gson.*
import com.overzealous.remark.Options
import com.overzealous.remark.Remark
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.lang.reflect.Type
import java.net.URLEncoder

/**
 * @author WireSegal
 * Created at 9:17 AM on 3/5/18.
 */

val events: MutableMap<String, Event> = mutableMapOf()

data class Entry(val url: String,
            val event: Event,
            val paraphrased: Boolean,
            val note: String?,
            val lines: List<Line>) {
    fun getFooterText(): String = Jsoup.parse(note).text()
}

data class Event(val url: String, val name: String, val date: String, val reviewState: ReviewState)

enum class ReviewState {
    LEGACY, PENDING, APPROVED
}

data class Line(val speaker: String, val text: String) {
    fun getTrueText(): String = Remark(Options.github().apply { inlineLinks = true }).convert(text)
}

fun apiRequest(type: String, vararg params: Pair<String, Any>): String {
    val allParams = mutableMapOf(*params)
    allParams["format"] = "json"

    return Jsoup.connect("https://wob.coppermind.net/api/$type?" +
            allParams.entries.joinToString("&") { "${it.key}=${it.value}" })
            .ignoreContentType(true)
            .header("Accept", "application/json; charset=utf-8")
            .header("Authorization", "Token $arcanumToken")
            .method(Connection.Method.GET)
            .execute().body()
}

fun entryFromId(id: Int): Entry {
    val eventJson = apiRequest("entry/$id")
    return GSON.fromJson(eventJson, Entry::class.java)
}

fun randomEntry(): Entry {
    val eventJson = apiRequest("random_entry")
    return GSON.fromJson(eventJson, Entry::class.java)
}

fun entriesFromSearch(terms: List<String>): Pair<List<Entry>, Boolean> {
    val urlParams = terms.joinToString("+") { URLEncoder.encode(it, "UTF-8") }
    val list = mutableListOf<Entry>()
    val eventJson = apiRequest("search_entry", "ordering" to "rank", "page_size" to 250, "query" to urlParams)
    val results = JsonParser().parse(eventJson).asJsonObject
    results.getAsJsonArray("results").mapTo(list) { GSON.fromJson(it, Entry::class.java) }

    return list to results.get("next").isJsonPrimitive
}

val GSON: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeHierarchyAdapter(Event::class.java, EventDeserializer)
        .registerTypeHierarchyAdapter(ReviewState::class.java, ReviewDeserializer)
        .create()

private object EventDeserializer : JsonDeserializer<Event> {
    private val GSON: Gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Event {
        if (json.isJsonPrimitive) {
            return events.getOrPut(json.asString) {
                val jsonBody = Jsoup.connect(json.asString).ignoreContentType(true).execute().body()
                val jsonElement = JsonParser().parse(jsonBody)
                context.deserialize(jsonElement, type)
            }
        }
        return GSON.fromJson(json, type)
    }
}

private object ReviewDeserializer : JsonDeserializer<ReviewState> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext) =
            when (json.asString) {
                "APPROVED" -> ReviewState.APPROVED
                "PENDING" -> ReviewState.PENDING
                else -> ReviewState.LEGACY
            }
}
