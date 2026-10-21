package pl.llp.aircasting.record

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FakeAirBeamLink
import pl.llp.aircasting.bluetooth.LinkState
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

class RecordingViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries, Dispatchers.Unconfined)
  private val link = FakeAirBeamLink()
  private val engine = RecordingEngine(
    link,
    sessions,
    LocalMeasurementStreamRepository(db.measurementStreamQueries, Dispatchers.Unconfined),
    LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries, Dispatchers.Unconfined),
    NoPhoneLocation,
    NoHost,
    Clock.System,
    TimeZone.UTC,
  )
  // Lazy: built after `setMain`, or `viewModelScope` binds to the real Main and never runs.
  private val vm by lazy { RecordingViewModel(link, engine) }
  private val miniV2 = DiscoveredAirBeam(DeviceId("AA:BB:CC:DD:EE:FF"), "AirBeamMini-1234", AirBeamDevice.Mini.V2)
  private val form = NewSessionForm(
    method = RecordingMethod.AirBeamMobile,
    name = "Morning walk",
    tagsInput = "bike, commute",
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
  fun start_without_a_connected_airbeam_fails_without_configuring() = runTest {
    vm.start(form)
    runCurrent()

    assertEquals(StartState.Failed(ConfigResult.NotConnected), vm.startState.value)
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

private object NoPhoneLocation : PhoneLocation {
  override fun latest(): GeoLocation? = null
  override suspend fun track() = awaitCancellation()
}

private object NoHost : BackgroundSessionHost {
  override fun start() {}
  override fun stop() {}
}
