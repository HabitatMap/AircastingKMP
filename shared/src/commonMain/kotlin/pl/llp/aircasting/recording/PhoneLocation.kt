package pl.llp.aircasting.recording

import pl.llp.aircasting.domain.GeoLocation

/**
 * The phone's newest location fix, kept fresh by continuous tracking while a session records.
 * Read once per measurement, so it must never wait for a fix.
 */
fun interface PhoneLocation {
  fun latest(): GeoLocation?
}