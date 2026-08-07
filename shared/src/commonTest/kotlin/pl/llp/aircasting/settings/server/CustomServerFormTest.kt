package pl.llp.aircasting.settings.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomServerFormTest {

  @Test
  fun `an empty form is not an error, it is just not submittable`() {
    // The supporting text under each field is a *hint* until the user types something wrong.
    // Showing "Enter a valid URL" on a pristine field would be shouting at someone who has
    // not done anything yet.
    val form = CustomServerForm()

    assertFalse(form.hostError)
    assertFalse(form.portError)
    assertNull(form.baseUrl)
  }

  @Test
  fun `a host without a scheme is rejected`() {
    assertTrue(CustomServerForm(host = "aircasting.org").hostError)
    assertNull(CustomServerForm(host = "aircasting.org").baseUrl)
  }

  @Test
  fun `an explicit scheme is kept`() {
    // Self-hosted boxes on a LAN often have no certificate, so http must stay expressible.
    assertEquals("http://192.168.1.10", CustomServerForm(host = "http://192.168.1.10").baseUrl)
  }

  @Test
  fun `both schemes are kept as typed`() {
    // Self-hosted boxes on a LAN often have no certificate, so http must stay expressible.
    assertEquals("https://aircasting.org", CustomServerForm(host = "https://aircasting.org").baseUrl)
    assertEquals("http://192.168.1.10", CustomServerForm(host = "http://192.168.1.10").baseUrl)
  }
  @Test
  fun `the port is appended when supplied`() {
    assertEquals(
      "https://aircasting.org:8080",
      CustomServerForm(host = "https://aircasting.org", port = "8080").baseUrl,
    )
  }
  @Test
  fun `surrounding whitespace and a trailing slash are forgiven`() {
    assertEquals(
      "https://aircasting.org",
      CustomServerForm(host = " https://aircasting.org/ ").baseUrl,
    )
  }

  @Test
  fun `a host with a path or a space is rejected`() {
    assertTrue(CustomServerForm(host = "https://aircasting.org/api").hostError)
    assertTrue(CustomServerForm(host = "https://air casting.org").hostError)
    assertNull(CustomServerForm(host = "https://aircasting.org/api").baseUrl)
  }

  @Test
  fun `the port must be a number in 1-65535`() {
    assertTrue(CustomServerForm(host = "https://aircasting.org", port = "0").portError)
    assertTrue(CustomServerForm(host = "https://aircasting.org", port = "65536").portError)
    assertTrue(CustomServerForm(host = "https://aircasting.org", port = "80a").portError)
    assertFalse(CustomServerForm(host = "https://aircasting.org", port = "65535").portError)
  }


  @Test
  fun `a bad port blocks submission even when the host is fine`() {
    assertNull(CustomServerForm(host = "https://aircasting.org", port = "99999").baseUrl)
  }

  @Test
  fun `a stored url is split back into its two fields`() {
    // Re-entering the wizard should show what is currently in effect, not a blank slate.
    assertEquals(
      CustomServerForm(host = "https://my.server", port = "8080"),
      CustomServerForm.from("https://my.server:8080"),
    )
    assertEquals(CustomServerForm(), CustomServerForm.from(null))
  }

  @Test
  fun `a valid form survives a round trip through its url`() {
    listOf(
      CustomServerForm(host = "https://my.server", port = "8080"),
      CustomServerForm(host = "http://192.168.1.10"),
    ).forEach { assertEquals(it, CustomServerForm.from(it.baseUrl)) }
  }
}