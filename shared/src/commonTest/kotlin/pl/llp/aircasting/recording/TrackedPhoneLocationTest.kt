package pl.llp.aircasting.recording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TrackedPhoneLocationTest {
  private val fixes = MutableSharedFlow<GeoLocation>()
  private val krakow = GeoLocation(50.06, 19.94)
  private val warsaw = GeoLocation(52.23, 21.01)

  private fun CoroutineScope.phone() = TrackedPhoneLocation(fixes, this)

  @Test
  fun latest_is_the_newest_fix_while_tracking() = runTest {
    val phone = backgroundScope.phone()
    assertNull(phone.latest())
    backgroundScope.launch { phone.track() }
    runCurrent()

    fixes.emit(krakow)
    fixes.emit(warsaw)
    runCurrent()

    assertEquals(warsaw, phone.latest())
  }

  /** Legacy fallback 3 — the phone's last known location — outlives the session that got it. */
  @Test
  fun the_last_fix_outlives_tracking() = runTest {
    val phone = backgroundScope.phone()
    val tracking = backgroundScope.launch { phone.track() }
    runCurrent()
    fixes.emit(krakow)
    runCurrent()
    tracking.cancel()

    assertEquals(krakow, phone.latest())
  }

  /** The wizard and the engine both track during the hand-off; that must stay one GPS session. */
  @Test
  fun trackers_share_one_gps_session() = runTest {
    val phone = backgroundScope.phone()
    backgroundScope.launch { phone.track() }
    backgroundScope.launch { phone.track() }
    runCurrent()

    assertEquals(1, fixes.subscriptionCount.value)
  }

  /** The warm path: the wizard's GPS already has a fix by the time Start is tapped. */
  @Test
  fun awaitFix_returns_the_running_sessions_fix_at_once() = runTest {
    val phone = backgroundScope.phone()
    backgroundScope.launch { phone.track() }
    runCurrent()
    fixes.emit(krakow)
    runCurrent()

    val fix = backgroundScope.async { phone.awaitFix() }
    runCurrent()

    assertEquals(krakow, fix.await())
  }

  /** A fix left from an earlier session may be hours old and in another city: not a real fix. */
  @Test
  fun awaitFix_waits_past_a_fix_from_an_earlier_gps_session() = runTest {
    val phone = backgroundScope.phone()
    val tracking = backgroundScope.launch { phone.track() }
    runCurrent()
    fixes.emit(warsaw)
    runCurrent()
    tracking.cancel()
    advanceTimeBy(GpsHandOff + GpsHandOff)
    runCurrent()

    val fix = backgroundScope.async { phone.awaitFix() }
    runCurrent()
    assertFalse(fix.isCompleted)

    fixes.emit(krakow)
    runCurrent()
    assertEquals(krakow, fix.await())
    assertEquals(krakow, phone.latest())
  }

  /** The wizard closes as the engine starts tracking; the GPS must not restart in that gap. */
  @Test
  fun the_gps_outlives_its_last_tracker_by_the_hand_off() = runTest {
    val phone = backgroundScope.phone()
    val wizard = backgroundScope.launch { phone.track() }
    runCurrent()
    fixes.emit(krakow)
    runCurrent()
    wizard.cancel()
    runCurrent()

    assertEquals(1, fixes.subscriptionCount.value)
    val fix = backgroundScope.async { phone.awaitFix() }
    runCurrent()
    assertEquals(krakow, fix.await())
  }
}
