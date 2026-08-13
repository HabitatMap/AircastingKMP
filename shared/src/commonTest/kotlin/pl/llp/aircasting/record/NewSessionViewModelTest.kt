package pl.llp.aircasting.record

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewSessionViewModelTest {

  @Test
  fun `the wizard opens on the method step with an empty form`() {
    val state = NewSessionViewModel().state.value

    assertEquals(NewSessionStep.Method, state.step)
    assertNull(state.form.method)
    assertFalse(state.canContinue, "Next must be dead until a method is picked")
  }

  @Test
  fun `next walks method to details to confirm`() {
    val vm = NewSessionViewModel()

    vm.select(RecordingMethod.PhoneMicrophone)
    vm.next()
    assertEquals(NewSessionStep.Details, vm.state.value.step)

    vm.rename("Morning commute")
    vm.next()
    assertEquals(NewSessionStep.Confirm, vm.state.value.step)
  }

  @Test
  fun `next is a no-op while the current step is incomplete`() {
    val vm = NewSessionViewModel()

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
    val vm = NewSessionViewModel()
    vm.select(RecordingMethod.PhoneMicrophone)
    vm.next()
    vm.rename("Morning commute")

    vm.back()

    assertEquals(NewSessionStep.Method, vm.state.value.step)
    assertEquals("Morning commute", vm.state.value.form.name)
  }

  @Test
  fun `tags are parsed from the comma-separated text, trimmed and de-duplicated`() {
    val vm = NewSessionViewModel()

    vm.editTags(" school , traffic,, school ,  ")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.tags)
  }

  @Test
  fun `a half-typed tag still counts, so Next never silently drops it`() {
    val vm = NewSessionViewModel()

    vm.editTags("school, traffic")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.tags)
  }

  @Test
  fun `a suggestion appends to what is already typed`() {
    val vm = NewSessionViewModel()
    vm.editTags("school,")

    vm.addTag("traffic")

    assertEquals(listOf("school", "traffic"), vm.state.value.form.tags)
    assertEquals("school, traffic", vm.state.value.form.tagsInput)
  }

  @Test
  fun `the sampling interval defaults to one second and is replaceable`() {
    val vm = NewSessionViewModel()
    assertEquals(SamplingInterval.OneSecond, vm.state.value.form.interval)

    vm.setInterval(SamplingInterval.FiveMinutes)

    assertEquals(SamplingInterval.FiveMinutes, vm.state.value.form.interval)
  }

  @Test
  fun `progress fills as the wizard advances`() {
    assertEquals(1f / 3, NewSessionStep.Method.progress)
    assertEquals(1f, NewSessionStep.Confirm.progress)
    assertTrue(NewSessionStep.entries.all { it.progress in 0f..1f })
  }
}