package pl.llp.aircasting.settings.account

import pl.llp.aircasting.i18n.EnStrings
import pl.llp.aircasting.i18n.FrStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class EditErrorTextTest {

  @Test
  fun a_known_code_is_translated_and_interpolates_its_params() {
    val tooShort = FieldError("too_short", "is too short (minimum is 3 characters)", mapOf("min" to "3"))

    assertEquals("Must be at least 3 characters.", EnStrings.describe(EditError.Rejected(tooShort)))
  }
  @Test
  fun an_unknown_code_falls_back_to_the_servers_message() {
    // The spec: "A client must never break on a code it does not know."
    val novel = FieldError("disposable_domain", "uses a disposable email provider")

    assertEquals("uses a disposable email provider", EnStrings.describe(EditError.Rejected(novel)))
  }

  @Test
  fun an_invalid_current_password_is_translated_instead_of_showing_the_servers_english() {
    val invalid = EditError.Rejected(FieldError("invalid", "is invalid"))

    assertEquals(FrStrings.fieldErrorWrongPassword, FrStrings.describe(PasswordField.Current, invalid))
  }

  @Test
  fun invalid_and_invalid_format_get_the_generic_translation_not_the_servers_english() {
    val invalid = FieldError("invalid", "is invalid")
    val badFormat = FieldError("invalid_format", "is in an invalid format", mapOf("format" to "email"))

    assertEquals(FrStrings.fieldErrorInvalid, FrStrings.describe(EditError.Rejected(invalid)))
    assertEquals(FrStrings.fieldErrorInvalid, FrStrings.describe(EditError.Rejected(badFormat)))
    assertEquals(FrStrings.fieldErrorInvalid, FrStrings.describe(PasswordField.New, EditError.Rejected(invalid)))
  }

  @Test
  fun a_refusal_naming_no_field_is_translated() {
    assertEquals(FrStrings.accountUpdateRefused, FrStrings.describe(EditError.Refused))
  }
}