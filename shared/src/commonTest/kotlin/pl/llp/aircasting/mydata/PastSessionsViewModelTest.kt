package pl.llp.aircasting.mydata

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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FakePastSessionsRepository : PastSessionsRepository {
  val sessions = MutableStateFlow(emptyList<PastSession>())
  override fun observe(): Flow<List<PastSession>> = sessions
}

@OptIn(ExperimentalCoroutinesApi::class)
class PastSessionsViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val repo = FakePastSessionsRepository()
  private val thresholds = FakeThresholdsRepository(
    defaults = listOf(SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)),
    aliases = mapOf("AirBeamMini-PM2.5" to "AirBeam-PM2.5"),
  )

  private fun TestScope.subscribedViewModel(): PastSessionsViewModel {
    val vm = PastSessionsViewModel(repo, thresholds)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    return vm
  }

  private fun past(uuid: String, pm25Avg: Double = 12.0) = PastSession(
    Session(
      uuid = uuid,
      type = SessionType.MOBILE,
      status = SessionStatus.FINISHED,
      name = uuid,
      tags = emptyList(),
      startTime = Instant.parse("2026-11-23T12:34:00Z"),
      endTime = Instant.parse("2026-11-23T12:34:00Z") + 1.hours,
      timeZone = "Europe/Warsaw",
      measurementInterval = 1.seconds,
      deviceModel = "AirBeamMini",
    ),
    listOf(
      StreamSummary("AirBeamMini-PM1", "µg/m³", 1.0, 1.0, 1.0),
      StreamSummary("AirBeamMini-PM2.5", "µg/m³", pm25Avg, pm25Avg, pm25Avg),
    ),
  )

  @Test
  fun each_past_session_becomes_a_card_in_the_repository_order() = runTest {
    repo.sessions.value = listOf(past("b"), past("a"))
    val vm = subscribedViewModel()
    advanceUntilIdle()

    assertEquals(listOf("b", "a"), vm.state.value.sessions.map { it.uuid })
    assertEquals(MeasurementLevel.MEDIUM, vm.state.value.sessions[0].selected?.avg?.level)
  }

  @Test
  fun selecting_a_chip_changes_only_that_card_and_survives_a_new_list() = runTest {
    repo.sessions.value = listOf(past("a"), past("b"))
    val vm = subscribedViewModel()

    vm.selectStream("a", "AirBeamMini-PM1")
    repo.sessions.value = listOf(past("a", pm25Avg = 40.0), past("b", pm25Avg = 40.0))
    advanceUntilIdle()

    assertEquals(
      listOf("AirBeamMini-PM1", "AirBeamMini-PM2.5"),
      vm.state.value.sessions.map { it.selected?.sensorName },
    )
  }
}
