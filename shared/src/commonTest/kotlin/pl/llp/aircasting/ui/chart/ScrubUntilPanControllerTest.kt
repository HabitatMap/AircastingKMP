package pl.llp.aircasting.ui.chart

import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.common.Point
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrubUntilPanControllerTest {

  // Slops in px: 60 = the chart's slop when fully zoomed out, 20 = the system touch slop.
  /** Zoomed in: there is history to pan to. */
  private val zoomedIn = ScrubUntilPanController(panSlop = 60f, touchSlop = 20f, canPan = { true })

  /** Fully zoomed out: the whole series fits, so there is nothing to pan. */
  private val zoomedOut = ScrubUntilPanController(panSlop = 60f, touchSlop = 20f, canPan = { false })

  /** What the zoomed-in controller says for [interaction], when it takes it at all. */
  private fun shows(interaction: Interaction): Boolean {
    assertTrue(zoomedIn.shouldAcceptInteraction(interaction, emptyList()))
    return zoomedIn.shouldShowMarker(interaction, emptyList())
  }

  private fun zoomedOutShows(interaction: Interaction): Boolean {
    assertTrue(zoomedOut.shouldAcceptInteraction(interaction, emptyList()))
    return zoomedOut.shouldShowMarker(interaction, emptyList())
  }

  // --- Zoomed in: drag pans, hold shows ---

  @Test
  fun zoomed_in_a_press_alone_shows_nothing() {
    assertFalse(shows(Interaction.Press(Point(100f, 50f))))
  }

  @Test
  fun zoomed_in_dragging_without_holding_never_scrubs() {
    shows(Interaction.Press(Point(100f, 50f)))

    assertFalse(shows(Interaction.Move(Point(110f, 50f))))
    assertFalse(shows(Interaction.Move(Point(300f, 50f))))
  }

  @Test
  fun zoomed_in_press_and_hold_shows_the_measurement() {
    shows(Interaction.Press(Point(100f, 50f)))

    assertTrue(shows(Interaction.LongPress(Point(100f, 50f))))
  }

  @Test
  fun zoomed_in_a_held_finger_may_wobble_within_the_touch_slop() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.LongPress(Point(100f, 50f)))

    assertTrue(shows(Interaction.Move(Point(119f, 60f))))
  }

  @Test
  fun zoomed_in_hold_then_pull_sideways_pans_and_hides_it() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.LongPress(Point(100f, 50f)))

    assertFalse(shows(Interaction.Move(Point(121f, 50f))))
    // Still panning, even back over the start.
    assertFalse(shows(Interaction.Move(Point(100f, 50f))))
  }

  @Test
  fun zoomed_in_scrolling_the_page_up_or_down_hides_it() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.LongPress(Point(100f, 50f)))

    assertFalse(shows(Interaction.Move(Point(100f, 71f))))
  }

  @Test
  fun zoomed_in_a_hold_after_the_drag_started_shows_nothing() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.Move(Point(200f, 50f)))

    assertFalse(shows(Interaction.LongPress(Point(200f, 50f))))
  }

  @Test
  fun lifting_the_finger_hides_it() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.LongPress(Point(100f, 50f)))

    assertFalse(shows(Interaction.Release(Point(100f, 50f))))
  }

  @Test
  fun a_new_press_forgets_the_last_hold() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.LongPress(Point(100f, 50f)))
    shows(Interaction.Release(Point(100f, 50f)))

    assertFalse(shows(Interaction.Press(Point(300f, 50f))))
    assertTrue(shows(Interaction.LongPress(Point(300f, 50f))))
  }

  @Test
  fun moves_and_holds_without_a_press_are_ignored() {
    // A mouse hovering, or the second finger of a pinch after Vico sent Release.
    assertFalse(zoomedIn.shouldAcceptInteraction(Interaction.Move(Point(1f, 1f)), emptyList()))
    assertFalse(zoomedIn.shouldAcceptInteraction(Interaction.LongPress(Point(1f, 1f)), emptyList()))
  }

  // --- Fully zoomed out: press shows, drag scrubs ---

  @Test
  fun fully_zoomed_out_the_tooltip_shows_as_soon_as_the_finger_lands() {
    assertTrue(zoomedOutShows(Interaction.Press(Point(100f, 50f))))
  }

  @Test
  fun fully_zoomed_out_the_tooltip_scrubs_across_the_whole_width() {
    zoomedOutShows(Interaction.Press(Point(100f, 50f)))

    assertTrue(zoomedOutShows(Interaction.Move(Point(900f, 50f))))
  }

  @Test
  fun fully_zoomed_out_scrolling_the_page_still_hides_it() {
    zoomedOutShows(Interaction.Press(Point(100f, 50f)))

    assertFalse(zoomedOutShows(Interaction.Move(Point(100f, 71f))))
  }

  @Test
  fun fully_zoomed_out_drift_up_or_down_mid_scrub_keeps_it() {
    // Past the pan slop the chart's drag owns the gesture, so the page can't scroll any more.
    zoomedOutShows(Interaction.Press(Point(100f, 50f)))
    zoomedOutShows(Interaction.Move(Point(200f, 50f)))

    assertTrue(zoomedOutShows(Interaction.Move(Point(400f, 90f))))
  }

  @Test
  fun fully_zoomed_out_holding_keeps_it() {
    zoomedOutShows(Interaction.Press(Point(100f, 50f)))

    assertTrue(zoomedOutShows(Interaction.LongPress(Point(100f, 50f))))
  }
}
