package pl.llp.aircasting.record

import pl.llp.aircasting.i18n.Strings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class SamplingInterval(val every: Duration) {
  OneSecond(1.seconds),
  FiveSeconds(5.seconds),
  OneMinute(1.minutes),
  FiveMinutes(5.minutes),
  TenMinutes(10.minutes),
}

fun Strings.label(interval: SamplingInterval): String = when (interval) {
  SamplingInterval.OneSecond -> samplingSeconds(1)
  SamplingInterval.FiveSeconds -> samplingSeconds(5)
  SamplingInterval.OneMinute -> samplingMinutes(1)
  SamplingInterval.FiveMinutes -> samplingMinutes(5)
  SamplingInterval.TenMinutes -> samplingMinutes(10)
}

data class NewSessionForm(
  val method: RecordingMethod? = null,
  val name: String = "",
  val tagsInput: String = "",
  val interval: SamplingInterval = SamplingInterval.OneSecond,
) {
  val tags: List<String>
    get() = tagsInput.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}

enum class NewSessionStep { Method, Details, Confirm }

val NewSessionStep.progress: Float
  get() = (ordinal + 1).toFloat() / NewSessionStep.entries.size

data class SessionSummary(
  val type: String,
  val name: String,
  val tags: List<String>,
  val interval: String,
)

fun Strings.summarize(form: NewSessionForm) = SessionSummary(
  type = form.method?.let { copyFor(it).name }.orEmpty(),
  name = form.name,
  tags = form.tags,
  interval = label(form.interval),
)