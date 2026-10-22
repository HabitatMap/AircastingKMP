package pl.llp.aircasting.recording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import pl.llp.aircasting.domain.GeoLocation
import kotlin.time.Duration.Companion.seconds

/**
 * The phone's newest location fix, kept fresh by continuous tracking while the new-session wizard
 * is open and while a session records. Read once per measurement, so [latest] never waits.
 */
interface PhoneLocation {
  fun latest(): GeoLocation?

  /** Keeps [latest] fresh until cancelled. Concurrent trackers share one GPS session. */
  suspend fun track()

  /** The first fix of the running GPS session, never one left from an earlier one. Runs the GPS while waiting. */
  suspend fun awaitFix(): GeoLocation
}

/** How long the GPS outlives its last tracker, so the wizard can hand it to the engine without a restart. */
internal val GpsHandOff = 5.seconds

/** The shared half: platforms only supply [fixes], which run the GPS while collected. */
class TrackedPhoneLocation(fixes: Flow<GeoLocation>, scope: CoroutineScope) : PhoneLocation {
  private val newest = MutableStateFlow<GeoLocation?>(null)

  /** One GPS session however many collect it; null until its first fix, and again once it stops. */
  private val live = fixes.stateIn(
    scope,
    SharingStarted.WhileSubscribed(GpsHandOff.inWholeMilliseconds, replayExpirationMillis = 0),
    null,
  )

  override fun latest() = newest.value

  override suspend fun track() {
    live.collect { fix -> if (fix != null) newest.value = fix }
  }

  override suspend fun awaitFix() = live.filterNotNull().first().also { newest.value = it }
}
