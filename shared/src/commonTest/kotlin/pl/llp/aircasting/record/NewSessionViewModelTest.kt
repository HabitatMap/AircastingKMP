package pl.llp.aircasting.record

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.recording.PhoneLocation

class NewSessionViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val location = TrackingPhoneLocation()
  private val ab3 = DiscoveredAirBeam(DeviceId("id-ab3"), "AirBeam3:246f28c47698", AirBeamDevice.AirBeam3)

  /** As legacy `startNewSessionWizard`: the GPS gets the whole wizard to find a fix before Start. */
  @Test
  fun `opening the wizard starts the gps`() {
    NewSessionViewModel(location)

    assertTrue(location.tracking)
  }

  @Test
  fun `the wizard opens on the method step with an empty form`() {
    val state = NewSessionViewModel(location).state.value

    assertEquals(NewSessionStep.Method, state.step)
    assertNull(state.form.method)
    assertFalse(state.canContinue, "Next must be dead until a method is picked")
  }

  @Test
  fun `next walks method to details to confirm`() {
    val vm = NewSessionViewModel(location)

    vm.select(RecordingMethod.PhoneMicrophone)
    vm.next()
    assertEquals(NewSessionStep.Details, vm.state.value.step)

    vm.rename("Morning commute")
    vm.next()
    assertEquals(NewSessionStep.Confirm, vm.state.value.step)
  }

  @Test
  fun `next is a no-op while the current step is incomplete`() {
    val vm = NewSessionViewModel(location)

    vm.next()
    assertEquals(NewSessionStep.Method, vm.state.value.step)

    vm.select(RecordingMethod.PhoneMicrophone)
    vm.next()
    vm.rename("   ")
    vm.next()
    assertEquals(NewSessionStep.Details, vm.state.value.step)
  }

  @Test
  fun `back returns to the previous step and keeps what was typed`() {
    val vm = NewSessionViewModel(location)
    vm.select(RecordingMethod.PhoneMicrophone)
    vm.next()
    vm.rename("Morning commute")

    vm.back()

    assertEquals(NewSessionStep.Method, vm.state.value.step)
    assertEquals("Morning commute", vm.state.value.form.name)
  }

  @Test
  fun `tags are parsed from the comma-separated text trimmed and de-duplicated`() {
    val vm = NewSessionViewModel(location)

    vm.editTags(" school , traffic,, school ,  ")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.tags)
  }

  @Test
  fun `a half-typed tag still counts so Next never silently drops it`() {
    val vm = NewSessionViewModel(location)

    vm.editTags("school, traffic")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.tags)
  }

  @Test
  fun `a suggestion appends to what is already typed`() {
    val vm = NewSessionViewModel(location)
    vm.editTags("school,")

    vm.addTag("traffic")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.tags)
    assertEquals("school, traffic", vm.state.value.form.tagsInput)
  }

  @Test
  fun `the sampling interval defaults to one second and is replaceable`() {
    val vm = NewSessionViewModel(location)
    assertEquals(SamplingInterval.OneSecond, vm.state.value.form.interval)

    vm.setInterval(SamplingInterval.FiveMinutes)

    assertEquals(SamplingInterval.FiveMinutes, vm.state.value.form.interval)
  }

  @Test
  fun `progress fills as the wizard advances`() {
    assertEquals(1f / 4, NewSessionStep.Method.progress)
    assertEquals(1f, NewSessionStep.Confirm.progress)
    assertTrue(NewSessionStep.entries.all { it.progress in 0f..1f })
  }
  @Test
  fun `an AirBeam method leads to the device step`() {
    val vm = NewSessionViewModel(location)

    vm.select(RecordingMethod.AirBeamMobile)
    vm.next()

    assertEquals(NewSessionStep.Device, vm.state.value.step)
  }

  @Test
  fun `next is dead on the device step until a device is picked`() {
    val vm = NewSessionViewModel(location)
    vm.select(RecordingMethod.AirBeamMobile)
    vm.next()

    assertFalse(vm.state.value.canContinue)
    vm.next()
    assertEquals(NewSessionStep.Device, vm.state.value.step)

    vm.selectDevice(ab3)
    vm.next()
    assertEquals(NewSessionStep.Details, vm.state.value.step)
    assertEquals(ab3, vm.state.value.form.device)
  }

  @Test
  fun `the phone microphone skips the device step in both directions`() {
    val vm = NewSessionViewModel(location)
    vm.select(RecordingMethod.PhoneMicrophone)

    vm.next()
    assertEquals(NewSessionStep.Details, vm.state.value.step)

    vm.back()
    assertEquals(NewSessionStep.Method, vm.state.value.step)
  }
}

private class TrackingPhoneLocation : PhoneLocation {
  var tracking = false
    private set

  override fun latest(): GeoLocation? = null

  override suspend fun awaitFix() = awaitCancellation()

  override suspend fun track() {
    tracking = true
    try {
      awaitCancellation()
    } finally {
      tracking = false
    }
  }
}
