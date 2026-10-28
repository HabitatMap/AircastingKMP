package pl.llp.aircasting.data.auth

import app.cash.turbine.test
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoredAuthSessionTest {

  @Test
  fun a_fresh_install_starts_signed_out() {
    assertEquals(AuthState.SignedOut, StoredAuthSession(MapSettings()).state.value)
  }

  @Test
  fun the_session_and_its_profile_survive_a_process_restart() {
    val settings = MapSettings()
    StoredAuthSession(settings).start("tok-123", Jane)

    // A second instance over the same storage is what a cold app start looks like.
    assertEquals(AuthState.SignedIn("tok-123", Jane), StoredAuthSession(settings).state.value)
  }

  @Test
  fun a_token_saved_by_a_build_without_profiles_still_signs_in() {
    // Exactly what an install upgraded from the token-only build has on disk.
    val settings = MapSettings().apply { putString("auth_token", "tok-123") }

    assertEquals(AuthState.SignedIn("tok-123", profile = null), StoredAuthSession(settings).state.value)
  }

  @Test
  fun ending_the_session_erases_it_from_storage_too() {
    val settings = MapSettings()
    val session = StoredAuthSession(settings).apply { start("tok-123", Jane) }

    session.end()

    assertEquals(AuthState.SignedOut, session.state.value)
    // The important half: neither the token nor the email may come back on the next launch.
    assertEquals(AuthState.SignedOut, StoredAuthSession(settings).state.value)
    assertTrue(settings.keys.isEmpty(), "left behind: ${settings.keys}")
  }

  @Test
  fun an_updated_profile_replaces_the_stored_one() {
    val settings = MapSettings()
    val session = StoredAuthSession(settings).apply { start("tok-123", Jane) }

    session.updateProfile("tok-123", Renamed)

    assertEquals(AuthState.SignedIn("tok-123", Renamed), session.state.value)
    assertEquals(AuthState.SignedIn("tok-123", Renamed), StoredAuthSession(settings).state.value)
  }

  @Test
  fun a_profile_arriving_after_sign_out_does_not_revive_the_session() {
    val settings = MapSettings()
    val session = StoredAuthSession(settings).apply { start("tok-123", Jane) }

    // A GET /user still in flight when the user taps Sign out lands here.
    session.end()
    session.updateProfile("tok-123", Renamed)

    assertEquals(AuthState.SignedOut, session.state.value)
    assertTrue(settings.keys.isEmpty(), "left behind: ${settings.keys}")
  }

  @Test
  fun observers_see_every_transition() = runTest {
    val session = StoredAuthSession(MapSettings())

    session.state.test {
      assertEquals(AuthState.SignedOut, awaitItem())
      session.start("tok-123", Jane)
      assertEquals(AuthState.SignedIn("tok-123", Jane), awaitItem())
      session.updateProfile("tok-123", Renamed)
      assertEquals(AuthState.SignedIn("tok-123", Renamed), awaitItem())
      session.end()
      assertEquals(AuthState.SignedOut, awaitItem())
    }
  }

  @Test
  fun a_profile_fetched_for_a_previous_account_is_ignored() {
    val settings = MapSettings()
    val session = StoredAuthSession(settings).apply { start("tok-123", Jane) }

    // Signed out and in as someone else while a request made with tok-123 was in flight.
    session.end()
    session.start("tok-456", Bob)
    session.updateProfile("tok-123", Renamed)

    assertEquals(AuthState.SignedIn("tok-456", Bob), session.state.value)
    assertEquals(AuthState.SignedIn("tok-456", Bob), StoredAuthSession(settings).state.value)
  }
}

private val Bob = AccountProfile(name = "bob", email = "bob@example.com")
private val Jane = AccountProfile(name = "jane", email = "jane@example.com")
private val Renamed = AccountProfile(name = "jane_doe", email = "jane@example.com")
