package pl.llp.aircasting.record

import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.i18n.Strings
import pl.llp.aircasting.recording.Placement
import pl.llp.aircasting.recording.WifiCredentials
import kotlin.math.roundToLong
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
  val device: DiscoveredAirBeam? = null,
  val committedTags: List<String> = emptyList(),
  val name: String = "",
  val tagDraft: String = "",
  val interval: SamplingInterval = SamplingInterval.OneSecond,
  /** Fixed only. Indoor by default, as in legacy: no pin to place. */
  val indoor: Boolean = true,
  val outdoorLocation: GeoLocation? = null,
  /** Fixed only. Its toString() hides the password, so this form's does too. */
  val wifi: WifiCredentials = WifiCredentials("", ""),
) {
  /** Chips plus the half-typed word, so Next never drops it. */
  val tags: List<String>
    get() = (committedTags + tagDraft).filter { it.isNotEmpty() }.distinct()

  /** Null while an outdoor session has no pin yet. */
  val placement: Placement?
    get() = if (indoor) Placement.Indoor else outdoorLocation?.let(Placement::Outdoor)
}

/** The steps this form walks: the phone mic needs no device, only an outdoor fixed session needs a pin. */
val NewSessionForm.steps: List<NewSessionStep>
  get() = NewSessionStep.entries.filter { step ->
    when (step) {
      NewSessionStep.Device -> method != RecordingMethod.PhoneMicrophone
      NewSessionStep.Location -> method == RecordingMethod.AirBeamFixed && !indoor
      else -> true
    }
  }

enum class NewSessionStep { Method, Device, Details, Location, Confirm }

val NewSessionStep.progress: Float
  get() = (ordinal + 1).toFloat() / NewSessionStep.entries.size

data class SessionSummary(
  val type: String,
  val name: String,
  val tags: List<String>,
  val interval: String,
  /** Fixed only: where the AirBeam stands. */
  val placement: String?,
)

fun Strings.summarize(form: NewSessionForm) = SessionSummary(
  type = form.method?.let { copyFor(it).name }.orEmpty(),
  name = form.name,
  tags = form.tags,
  // Only AirBeam mobile has a picker: fixed is configured at 60 s (DOMAIN_KNOWLEDGE.md), the mic reads every second.
  interval = label(
    when (form.method) {
      RecordingMethod.AirBeamFixed -> SamplingInterval.OneMinute
      RecordingMethod.PhoneMicrophone -> SamplingInterval.OneSecond
      else -> form.interval
    }
  ),
  placement = if (form.method != RecordingMethod.AirBeamFixed) null else when (val placement = form.placement) {
    Placement.Indoor -> placementIndoor
    is Placement.Outdoor -> "$placementOutdoor (${placement.location.latitude.short()}, ${placement.location.longitude.short()})"
    null -> null
  },
)

/** Four decimals ≈ 11 m. Common Kotlin has no String.format, so round and let toString print it. */
private fun Double.short() = ((this * 10_000).roundToLong() / 10_000.0).toString()