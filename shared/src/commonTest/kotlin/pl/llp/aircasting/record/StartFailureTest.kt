package pl.llp.aircasting.record

import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.EnStrings

/** What Confirm tells the user when Start didn't work. */
class StartFailureTest {

  @Test
  fun `nothing to say until a start has failed`() {
    listOf(StartState.Idle, StartState.Preparing(PrepareStep.Configuring), StartState.Started).forEach {
      assertNull(EnStrings.startFailure(it), "$it")
    }
  }

  /** Nack 0x05: the one failure the user can fix themselves, on the Details step. */
  @Test
  fun `a wrong wifi password points at the network details`() {
    assertEquals(EnStrings.startFailedWifi, EnStrings.startFailure(StartState.Failed(ConfigResult.WifiPassFailure)))
  }

  @Test
  fun `each way a start fails reads differently in every locale`() {
    val failures = listOf(
      StartState.SignedOut,
      StartState.ServerError,
      StartState.Failed(ConfigResult.WifiPassFailure),
      StartState.Failed(ConfigResult.FirstMeasurementFailure),
      StartState.Failed(ConfigResult.NotConnected),
      StartState.Failed(ConfigResult.BadConfigFailure),
    )
    AppStrings.forEach { (tag, strings) ->
      val messages = failures.map { strings.startFailure(it) }
      assertTrue(messages.none { it.isNullOrBlank() }, "missing failure message in $tag")
      assertEquals(messages.size, messages.toSet().size, "two failures share a message in $tag")
    }
  }
}