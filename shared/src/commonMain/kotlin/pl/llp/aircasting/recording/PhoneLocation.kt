package pl.llp.aircasting.recording

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pl.llp.aircasting.domain.GeoLocation

/**
 * The phone's newest location fix, kept fresh by continuous tracking while a session records.
 * Read once per measurement, so it must never wait for a fix.
 */
interface PhoneLocation {
  fun latest(): GeoLocation?

  /** Keeps [latest] fresh until cancelled. */
  suspend fun track()
}

/** The shared half: platforms only supply [fixes], which run the GPS while collected. */
class TrackedPhoneLocation(private val fixes: Flow<GeoLocation>) : PhoneLocation {
  private val newest = MutableStateFlow<GeoLocation?>(null)

  override fun latest() = newest.value

  override suspend fun track() = fixes.collect { newest.value = it }
}