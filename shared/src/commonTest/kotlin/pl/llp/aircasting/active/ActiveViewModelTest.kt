package pl.llp.aircasting.active

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val repo = FakeActiveSessionsRepository()
  private val thresholds = FakeThresholdsRepository(
    defaults = listOf(SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)),
    aliases = mapOf("AirBeamMini-PM2.5" to "AirBeam-PM2.5"),
  )
  private val finished = mutableListOf<String>()

  private fun TestScope.subscribedViewModel(): ActiveViewModel {
    val vm = ActiveViewModel(repo, thresholds, finish = { finished += it })
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