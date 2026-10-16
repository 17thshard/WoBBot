package wiresegal.wob.palanaeum

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Entry (
	val id: Int,
	val event: Int,
	@SerialName("event_name") val eventName: String,
	@SerialName("event_date") val eventDate: LocalDate,
	@SerialName("event_state") val eventState: EventState,
	val date: LocalDate,
	val paraphrased: Boolean,
	@SerialName("modified_date") val modifiedDate: Instant,
	val tags: List<String>,
	val lines: List<EntryLine>,
	val note: String,
)

@Serializable
data class EntryLine(
	val speaker: String,
	val text: String,
)


@Serializable
enum class EventState(val note: String?) {
	@SerialName("N/A") N_A(null),
	PENDING("Pending Review"),
	APPROVED("Approved"),
	;
}

@Serializable
data class SearchPage (
	val count: Int,
	val next: String?,
	val previous: String?,
	val results: List<Entry>,
)
