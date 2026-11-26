package pl.llp.aircasting.active

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.explore.FakeThresholdsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val repo = FakeActiveSessionsRepository()
  private val fixedRepo = FakeActiveFixedSessionsRepository()
  private val thresholds = FakeThresholdsRepository(
    defaults = listOf(SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)),
    aliases = mapOf("AirBeamMini-PM2.5" to "AirBeam-PM2.5"),
  )
  private val finished = mutableListOf<String>()

  private fun TestScope.subscribedViewModel(): ActiveViewModel {
    val vm = ActiveViewModel(repo, fixedRepo, thresholds, FixedClock(T0), finish = { finished += it })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    return vm
  }

  @Test
  fun each_active_mobile_session_becomes_a_card_coloured_by_the_thresholds() = runTest {
    repo.mobile.value = listOf(mini("a", pm25 = 12.0), mini("b"))
    val vm = subscribedViewModel()
    advanceUntilIdle()

    val cards = vm.state.value.mobile
    assertEquals(listOf("a", "b"), cards.map { it.uuid })
    assertEquals(MeasurementLevel.MEDIUM, cards[0].selected?.level)
    assertEquals(CardState.NoDataYet, cards[1].state)
  }

  @Test
  fun selecting_a_chip_changes_only_that_card_and_survives_new_readings() = runTest {
    repo.mobile.value = listOf(mini("a", pm25 = 12.0), mini("b", pm25 = 12.0))
    val vm = subscribedViewModel()

    vm.selectStream("a", "AirBeamMini-PM1")
    repo.mobile.value = listOf(mini("a", pm25 = 40.0), mini("b", pm25 = 40.0))
    advanceUntilIdle()

    assertEquals(
      listOf("AirBeamMini-PM1", "AirBeamMini-PM2.5"),
      vm.state.value.mobile.map { it.selected?.sensorName },
    )
  }

  @Test
  fun finish_asks_first_and_dismissing_finishes_nothing() = runTest {
    repo.mobile.value = listOf(mini("a"))
    val vm = subscribedViewModel()

    vm.requestFinish("a")
    advanceUntilIdle()
    assertEquals("a", vm.state.value.confirmingFinish?.uuid)

    vm.dismissFinish()
    advanceUntilIdle()
    assertNull(vm.state.value.confirmingFinish)
    assertEquals(emptyList(), finished)
  }

  @Test
  fun confirming_finishes_that_session_and_closes_the_dialog() = runTest {
    repo.mobile.value = listOf(mini("a"), mini("b"))
    val vm = subscribedViewModel()

    vm.requestFinish("b")
    vm.confirmFinish()
    advanceUntilIdle()

    assertEquals(listOf("b"), finished)
    assertNull(vm.state.value.confirmingFinish)
  }

  @Test
  fun phone_microphone_sessions_get_their_own_section_and_can_be_finished() = runTest {
    repo.mobile.value = listOf(mini("a"), mic("m"))
    val vm = subscribedViewModel()
    advanceUntilIdle()

    assertEquals(listOf("a"), vm.state.value.mobile.map { it.uuid })
    assertEquals(listOf("m"), vm.state.value.microphone.map { it.uuid })

    vm.requestFinish("m")
    advanceUntilIdle()
    assertEquals("m", vm.state.value.confirmingFinish?.uuid)
  }

  @Test
  fun mobile_cards_show_while_the_fixed_fetch_has_not_answered() = runTest {
    // The server timed out: the fixed flow emits nothing. combine must not wait on it.
    fixedRepo.flow = flow { awaitCancellation() }
    repo.mobile.value = listOf(mini("a"))
    val vm = subscribedViewModel()
    advanceUntilIdle()

    assertEquals(listOf("a"), vm.state.value.mobile.map { it.uuid })
  }

  @Test
  fun fixed_sessions_get_their_own_section() = runTest {
    fixedRepo.fixed.value = listOf(fixed("f"))
    val vm = subscribedViewModel()
    advanceUntilIdle()

    assertEquals(listOf("f"), vm.state.value.fixed.map { it.uuid })
    assertEquals(CardState.Live, vm.state.value.fixed.single().state)
  }

  @Test
  fun selecting_a_chip_on_a_fixed_card_sticks() = runTest {
    fixedRepo.fixed.value = listOf(fixed("f"))
    val vm = subscribedViewModel()

    vm.selectStream("f", "AirBeamMini-PM1")
    advanceUntilIdle()

    assertEquals("AirBeamMini-PM1", vm.state.value.fixed.single().selected?.sensorName)
  }

  @Test
  fun confirming_a_fixed_card_finishes_it_on_the_server_not_locally() = runTest {
    repo.mobile.value = listOf(mini("a"))
    fixedRepo.fixed.value = listOf(fixed("f"))
    val vm = subscribedViewModel()

    vm.requestFinish("f")
    advanceUntilIdle()
    assertEquals("f", vm.state.value.confirmingFinish?.uuid)
    vm.confirmFinish()
    advanceUntilIdle()

    assertEquals(listOf("f"), fixedRepo.finished)
    assertEquals(emptyList(), finished)
  }

  @Test
  fun reconnect_refetches_the_fixed_sessions() = runTest {
    val vm = subscribedViewModel()

    vm.reconnect()

    assertEquals(1, fixedRepo.refreshes)
  }

  private fun fixed(uuid: String) = ActiveFixedSession(
    uuid = uuid,
    name = "Rooftop",
    tags = emptyList(),
    deviceModel = "AirBeamMini",
    isIndoor = false,
    timeZone = "Europe/Warsaw",
    startTime = T0,
    lastReportedAt = T0,
    readings = listOf(
      LatestReading("AirBeamMini-PM1", "µg/m³", 3.0, T0),
      LatestReading("AirBeamMini-PM2.5", "µg/m³", 12.0, T0),
    ),
  )

  private fun mic(uuid: String) = mini(uuid).let {
    it.copy(
      session = it.session.copy(deviceId = "Builtin", deviceModel = "Phone Microphone"),
      readings = listOf(LatestReading("Phone Microphone", "dB")),
    )
  }

  private fun mini(uuid: String, pm25: Double? = null) = ActiveMobileSession(
    session = Session(
      uuid = uuid,
      type = SessionType.MOBILE,
      status = SessionStatus.RECORDING,
      name = "Living room",
      tags = emptyList(),
      startTime = T0,
      timeZone = "Europe/Warsaw",
      measurementInterval = 1.seconds,
      deviceModel = "AirBeamMini",
    ),
    readings = listOf(
      LatestReading("AirBeamMini-PM1", "µg/m³", pm25?.let { 3.0 }, pm25?.let { T0 }),
      LatestReading("AirBeamMini-PM2.5", "µg/m³", pm25, pm25?.let { T0 }),
    ),
  )

  private companion object {
    val T0: Instant = Instant.fromEpochMilliseconds(1_786_000_000_000)
  }
}

class FakeActiveSessionsRepository : ActiveSessionsRepository {
  val mobile = MutableStateFlow(emptyList<ActiveMobileSession>())
  override fun observeMobile(): Flow<List<ActiveMobileSession>> = mobile
}
class FakeActiveFixedSessionsRepository : ActiveFixedSessionsRepository {
  val fixed = MutableStateFlow(emptyList<ActiveFixedSession>())
  var flow: Flow<List<ActiveFixedSession>> = fixed
  var refreshes = 0
  val finished = mutableListOf<String>()
  override fun observeFixed(): Flow<List<ActiveFixedSession>> = flow
  override suspend fun finish(uuid: String) {
    finished += uuid
  }
  override fun refresh() {
    refreshes++
  }
}

class FixedClock(private val now: Instant) : Clock {
  override fun now(): Instant = now
}
