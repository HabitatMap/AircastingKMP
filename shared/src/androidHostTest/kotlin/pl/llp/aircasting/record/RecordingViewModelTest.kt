package pl.llp.aircasting.record

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.FakeAirBeamLink
import pl.llp.aircasting.bluetooth.LinkState
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.data.local.LocalMeasurementRepository
import pl.llp.aircasting.data.local.LocalMeasurementStreamRepository
import pl.llp.aircasting.data.local.LocalSessionRepository
import pl.llp.aircasting.data.local.testDatabase
import pl.llp.aircasting.domain.GeoLocation

import pl.llp.aircasting.recording.BackgroundSessionHost
import pl.llp.aircasting.recording.PhoneLocation
import pl.llp.aircasting.recording.RecordingEngine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class RecordingViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries, Dispatchers.Unconfined)
  private val link = FakeAirBeamLink()
  private val krakow = GeoLocation(50.06, 19.94)
  private val location = FakePhoneLocation(krakow)
  private val engine = RecordingEngine(
    link,
    sessions,
    LocalMeasurementStreamRepository(db.measurementStreamQueries, Dispatchers.Unconfined),
    LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries, Dispatchers.Unconfined),
    location,
    NoHost,
    Clock.System,
    TimeZone.UTC,
  )
  // Lazy: built after `setMain`, or `viewModelScope` binds to the real Main and never runs.
  private val vm by lazy { RecordingViewModel(link, engine, location) }
  private val miniV2 = DiscoveredAirBeam(DeviceId("AA:BB:CC:DD:EE:FF"), "AirBeamMini-1234", AirBeamDevice.Mini.V2)
  private val otherMini = DiscoveredAirBeam(DeviceId("11:22:33:44:55:66"), "AirBeamMini-9999", AirBeamDevice.Mini.V2)
  private val form = NewSessionForm(
    method = RecordingMethod.AirBeamMobile,
    name = "Morning walk",
    tagsInput = "bike, commute",
    device = miniV2,
  )
  @Test
  fun start_records_the_form_on_the_connected_airbeam() = runTest {
    link.state.value = LinkState.Connected(miniV2)

    vm.start(form)
    runCurrent()

    assertEquals(StartState.Started, vm.startState.value)
    val session = sessions.find(link.configured.single().uuid.toString())
    assertEquals("Morning walk", session?.name)
    assertEquals(listOf("bike", "commute"), session?.tags)
  }

  @Test
  fun start_connects_the_picked_airbeam_and_records_on_it() = runTest {
    vm.start(form)
    runCurrent()

    assertEquals(listOf(miniV2), link.connects)
    assertEquals(StartState.Started, vm.startState.value)
    assertEquals(miniV2.id.value, sessions.find(link.configured.single().uuid.toString())?.deviceId)
  }

  /** Connecting again would tear down a working link and redo the handshake for nothing. */
  @Test
  fun start_reuses_a_link_already_on_the_picked_airbeam() = runTest {
    link.state.value = LinkState.Connected(miniV2)

    vm.start(form)
    runCurrent()

    assertEquals(emptyList(), link.connects)
    assertEquals(StartState.Started, vm.startState.value)
  }

  @Test
  fun start_switches_to_the_picked_airbeam_when_linked_to_another() = runTest {
    link.state.value = LinkState.Connected(otherMini)

    vm.start(form)
    runCurrent()

    assertEquals(listOf(miniV2), link.connects)
    assertEquals(miniV2.id.value, sessions.find(link.configured.single().uuid.toString())?.deviceId)
  }

  @Test
  fun a_failed_connect_fails_without_configuring() = runTest {
    link.connectFailure = FailureReason.NoDeviceFound

    vm.start(form)
    runCurrent()

    assertEquals(StartState.Failed(ConfigResult.NotConnected), vm.startState.value)
    assertEquals(emptyList(), link.configured)
  }

  /** The wizard can't get here without a pick, but the VM mustn't fall back to whatever is linked. */
  @Test
  fun start_without_a_picked_airbeam_fails_without_connecting() = runTest {
    link.state.value = LinkState.Connected(miniV2)

    vm.start(form.copy(device = null))
    runCurrent()

    assertEquals(StartState.Failed(ConfigResult.NotConnected), vm.startState.value)
    assertEquals(emptyList(), link.connects)
    assertEquals(emptyList(), link.configured)
  }

  /** The confirm screen stays put with the form intact, so the user can retry. */
  @Test
  fun a_rejected_configure_is_reported_as_is() = runTest {
    link.state.value = LinkState.Connected(miniV2)
    link.configureResult = ConfigResult.NotSyncedFailure

    vm.start(form)
    runCurrent()

    assertEquals(StartState.Failed(ConfigResult.NotSyncedFailure), vm.startState.value)
  }
  /** The engine records mobile AirBeam sessions only; fixed and mic must not become one. */
  @Test
  fun other_methods_do_not_start_a_mobile_session() = runTest {
    link.state.value = LinkState.Connected(miniV2)

    vm.start(form.copy(method = RecordingMethod.AirBeamFixed))
    runCurrent()

    assertEquals(StartState.Failed(ConfigResult.BadConfigFailure), vm.startState.value)
    assertEquals(emptyList(), link.configured)
  }

  /** The device must not stream before the phone has a fix, or its first readings land in New York. */
  @Test
  fun start_waits_for_a_fix_before_configuring() = runTest {
    location.live.value = null
    link.state.value = LinkState.Connected(miniV2)

    vm.start(form)
    runCurrent()
    assertEquals(StartState.Preparing(PrepareStep.Locating), vm.startState.value)
    assertEquals(emptyList(), link.configured)

    location.live.value = krakow
    runCurrent()

    assertEquals(StartState.Started, vm.startState.value)
    assertEquals(krakow, (link.configured.single() as SessionConfig.Mobile).location)
  }

  /** Indoors there may be no fix at all; recording without one beats never recording. */
  @Test
  fun start_stops_waiting_for_a_fix_after_the_timeout() = runTest {
    location.live.value = null
    link.state.value = LinkState.Connected(miniV2)

    vm.start(form)
    advanceTimeBy(LocatingTimeout - 1.milliseconds)
    runCurrent()
    assertEquals(StartState.Preparing(PrepareStep.Locating), vm.startState.value)

    advanceTimeBy(2.milliseconds)
    runCurrent()
    assertEquals(StartState.Started, vm.startState.value)
  }

  @Test
  fun a_second_tap_while_preparing_starts_nothing_more() = runTest {
    location.live.value = null
    link.state.value = LinkState.Connected(miniV2)

    vm.start(form)
    runCurrent()
    vm.start(form)
    location.live.value = krakow
    runCurrent()

    assertEquals(1, link.configured.size)
  }

  @Test
  fun stop_finishes_the_session() = runTest {
    link.state.value = LinkState.Connected(miniV2)
    vm.start(form)
    runCurrent()

    vm.stop()
    runCurrent()

    assertNotNull(sessions.find(link.configured.single().uuid.toString())?.endTime)
    assertTrue(!engine.recording.first())
  }
}

/** [live] stands for the running GPS session's fix; null until it has one. */
private class FakePhoneLocation(fix: GeoLocation?) : PhoneLocation {
  val live = MutableStateFlow(fix)

  override fun latest() = live.value

  override suspend fun track() = awaitCancellation()

  override suspend fun awaitFix() = live.filterNotNull().first()
}

private object NoHost : BackgroundSessionHost {
  override fun start() {}
  override fun stop() {}
}
