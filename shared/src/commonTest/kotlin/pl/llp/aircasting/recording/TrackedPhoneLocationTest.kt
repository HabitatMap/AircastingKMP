package pl.llp.aircasting.recording

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrackedPhoneLocationTest {
  private val fixes = MutableSharedFlow<GeoLocation>()
  private val phone = TrackedPhoneLocation(fixes)
  private val krakow = GeoLocation(50.06, 19.94)
  private val warsaw = GeoLocation(52.23, 21.01)

  @Test
  fun latest_is_the_newest_fix_while_tracking() = runTest {
    assertNull(phone.latest())
    backgroundScope.launch { phone.track() }
    runCurrent()

    fixes.emit(krakow)
    fixes.emit(warsaw)

    assertEquals(warsaw, phone.latest())
  }

  /** Legacy fallback 3 — the phone's last known location — outlives the session that got it. */
  @Test
  fun the_last_fix_outlives_tracking() = runTest {
    val tracking = backgroundScope.launch { phone.track() }
    runCurrent()
    fixes.emit(krakow)
    tracking.cancel()

    assertEquals(krakow, phone.latest())
  }
}