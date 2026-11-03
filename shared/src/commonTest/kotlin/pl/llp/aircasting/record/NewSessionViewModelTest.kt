package pl.llp.aircasting.record

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.recording.PhoneLocation
import pl.llp.aircasting.recording.Placement
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewSessionViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val location = TrackingPhoneLocation()
  private val park = GeoLocation(latitude = 50.05, longitude = 19.93)
  private val ab3 =
    DiscoveredAirBeam(DeviceId("id-ab3"), "AirBeam3:246f28c47698", AirBeamDevice.AirBeam3)

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
  fun `the sampling interval defaults to one second and is replaceable`() {
    val vm = NewSessionViewModel(location)
    assertEquals(SamplingInterval.OneSecond, vm.state.value.form.interval)

    vm.setInterval(SamplingInterval.FiveMinutes)

    assertEquals(SamplingInterval.FiveMinutes, vm.state.value.form.interval)
  }

  @Test
  fun `progress fills as the wizard advances`() {
    assertEquals(1f / 5, NewSessionStep.Method.progress)
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

  @Test
  fun `a fixed session starts indoor and skips the location step`() {
    val vm = fixedAtDetails()
    assertEquals(Placement.Indoor, vm.state.value.form.placement)

    vm.fillDetails()
    vm.next()

    assertEquals(NewSessionStep.Confirm, vm.state.value.step)
  }

  @Test
  fun `fixed details need the wifi name and password before next`() {
    val vm = fixedAtDetails()
    vm.rename("Rooftop")
    vm.next()
    assertEquals(NewSessionStep.Details, vm.state.value.step)

    vm.editWifiName("HomeNet")
    vm.next()
    assertEquals(NewSessionStep.Details, vm.state.value.step)

    vm.editWifiPassword("secret")
    vm.next()
    assertEquals(NewSessionStep.Confirm, vm.state.value.step)
  }

  @Test
  fun `switching back to indoor drops the location step`() {
    val vm = fixedAtDetails()
    vm.fillDetails()
    vm.setIndoor(false)
    vm.next()
    vm.back()

    vm.setIndoor(true)
    vm.next()

    assertEquals(NewSessionStep.Confirm, vm.state.value.step)
  }

  @Test
  fun `a mobile session never asks for wifi or a location`() {
    val vm = NewSessionViewModel(location)
    vm.select(RecordingMethod.AirBeamMobile)
    vm.next()
    vm.selectDevice(ab3)
    vm.next()
    vm.setIndoor(false)

    vm.rename("Morning commute")
    vm.next()

    assertEquals(NewSessionStep.Confirm, vm.state.value.step)
  }

  @Test
  fun `the wifi password stays out of the form's toString`() {
    val vm = fixedAtDetails()
    vm.editWifiPassword("hunter2")

    assertFalse("hunter2" in vm.state.value.form.toString())
  }

  @Test
  fun `a space or comma turns the typed word into a chip`() {
    val vm = NewSessionViewModel(location)

    vm.editTagDraft("school ")
    vm.editTagDraft("traffic,")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.committedTags)
    assertEquals("", vm.state.value.form.tagDraft)
  }

  /** tag_list is space-separated: a tag holding a space would come back from the backend as two. */
  @Test
  fun `a pasted list becomes chips except the word still being typed`() {
    val vm = NewSessionViewModel(location)

    vm.editTagDraft(" school , traffic,, bus")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.committedTags)
    assertEquals("bus", vm.state.value.form.tagDraft)
  }

  @Test
  fun `a half-typed tag still counts so Next never silently drops it`() {
    val vm = NewSessionViewModel(location)
    vm.editTagDraft("school ")

    vm.editTagDraft("traf")

    assertEquals(listOf("school", "traf"), vm.state.value.form.tags)
  }

  @Test
  fun `the add chip turns the typed word into a chip`() {
    val vm = NewSessionViewModel(location)
    vm.editTagDraft("rooftop")

    vm.commitTagDraft()

    assertEquals(listOf("rooftop"), vm.state.value.form.committedTags)
    assertEquals("", vm.state.value.form.tagDraft)
  }

  @Test
  fun `a tag is never added twice`() {
    val vm = NewSessionViewModel(location)

    vm.editTagDraft("school school ")
    vm.addTag("school")

    assertEquals(listOf("school"), vm.state.value.form.tags)
  }

  @Test
  fun `picking a matching tag replaces the half-typed word`() {
    val vm = NewSessionViewModel(location)
    vm.editTagDraft("traf")

    vm.addTag("traffic")

    assertEquals(listOf("traffic"), vm.state.value.form.tags)
    assertEquals("", vm.state.value.form.tagDraft)
  }

  @Test
  fun `removing a chip drops that tag`() {
    val vm = NewSessionViewModel(location)
    vm.editTagDraft("school traffic ")

    vm.removeTag("school")

    assertEquals(listOf("traffic"), vm.state.value.form.tags)
  }

  @Test
  fun `matching tags start with the typed word ignore case and skip ones already in`() {
    val state = NewSessionState(
      form = NewSessionForm(committedTags = listOf("school"), tagDraft = "sc"),
      recentTags = listOf("school", "Scooter", "traffic", "sc"),
    )

    assertEquals(listOf("Scooter"), state.matchingTags)
  }

  /** Legacy ChooseLocationViewMvcImpl: the map opens where the phone is. */
  @Test
  fun `choosing outdoor drops the pin where the phone is`() {
    location.fix = rooftop
    val vm = fixedAtDetails()

    vm.setIndoor(false)

    assertEquals(Placement.Outdoor(rooftop), vm.state.value.form.placement)
  }

  /** …or on New York without a fix. */
  @Test
  fun `without a fix the pin starts in new york`() {
    val vm = fixedAtDetails()

    vm.setIndoor(false)

    assertEquals(Placement.Outdoor(GeoLocation(40.7128, -74.0060)), vm.state.value.form.placement)
  }

  @Test
  fun `a pin moved on the map survives switching back and forth`() {
    location.fix = rooftop
    val vm = fixedAtDetails()
    vm.fillDetails()
    vm.setIndoor(false)
    vm.next()
    vm.placePin(park)

    vm.back()
    vm.setIndoor(true)
    vm.setIndoor(false)
    vm.next()
    assertEquals(Placement.Outdoor(park), vm.state.value.form.placement)

    vm.next()
    assertEquals(NewSessionStep.Confirm, vm.state.value.step)
  }

  private val rooftop = GeoLocation(latitude = 50.06, longitude = 19.94)

  private fun fixedAtDetails() = NewSessionViewModel(location).apply {
    select(RecordingMethod.AirBeamFixed)
    next()
    selectDevice(ab3)
    next()
  }

  private fun NewSessionViewModel.fillDetails() {
    rename("Rooftop")
    editWifiName("HomeNet")
    editWifiPassword("secret")
  }
}

private class TrackingPhoneLocation : PhoneLocation {
  var tracking = false
    private set
  var fix: GeoLocation? = null

  override fun latest(): GeoLocation? = fix

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
