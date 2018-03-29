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

data class Entry(val id: String,
                 val event: Int,
                 val eventName: String,
                 val date: String,
                 val eventState: ReviewState?,
                 val paraphrased: Boolean,
                 val note: String?,
                 val lines: List<Line>) {
    fun getFooterText(): String = Jsoup.parse(note).text()

    override fun toString() = "$urlTarget/events/$event/#e$id"
}

enum class ReviewState {
    LEGACY, PENDING, APPROVED
}

data class Line(val speaker: String, val text: String) {
    fun getTrueSpeaker(): String = Jsoup.parse(speaker).text()
    fun getTrueText(): String = Remark(Options.github().apply { inlineLinks = true }).convert(text)
}

fun nakedApiRequest(url: String): String {
    return Jsoup.connect(url)
            .ignoreContentType(true)
            .header("Accept", "application/json; charset=utf-8")
            .apply { if (arcanumToken != null) header("Authorization", "Token $arcanumToken") }
            .method(Connection.Method.GET)
            .execute().body()
}

fun apiRequest(type: String, vararg params: Pair<String, Any>): String {
    val allParams = mutableMapOf(*params)
    allParams["format"] = "json"

    return nakedApiRequest("$urlTarget/api/$type?" +
            allParams.entries.joinToString("&") { "${it.key}=${it.value}" })
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
    val eventJson = apiRequest("search_entry",
            "ordering" to "rank", "page_size" to 250, "query" to urlParams)
    val results = JsonParser().parse(eventJson).asJsonObject
    return results.getAsJsonArray("results").map { GSON.fromJson(it, Entry::class.java) } to !results.get("next").isJsonNull
}

val GSON: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeHierarchyAdapter(ReviewState::class.java, ReviewDeserializer)
        .create()

private object ReviewDeserializer : JsonDeserializer<ReviewState> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext) =
            when (json.asString) {
                "APPROVED" -> ReviewState.APPROVED
                "PENDING" -> ReviewState.PENDING
                else -> ReviewState.LEGACY
            }
}
