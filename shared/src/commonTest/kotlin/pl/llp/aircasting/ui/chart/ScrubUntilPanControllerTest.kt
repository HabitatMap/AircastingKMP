package pl.llp.aircasting.ui.chart

import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.common.Point
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrubUntilPanControllerTest {

  // Slops in px: 60 sideways (the chart's pan slop), 20 up/down (the page's scroll slop).
  private val controller = ScrubUntilPanController(panSlop = 60f, verticalSlop = 20f, canPan = { true })

  /** Fully zoomed out: the whole series fits, so there is nothing to pan. */
  private val zoomedOut = ScrubUntilPanController(panSlop = 60f, verticalSlop = 20f, canPan = { false })
  private fun zoomedOutShows(interaction: Interaction): Boolean {
    assertTrue(zoomedOut.shouldAcceptInteraction(interaction, emptyList()))
    return zoomedOut.shouldShowMarker(interaction, emptyList())
  }

  /** What the controller says for [interaction], when it takes it at all. */
  private fun shows(interaction: Interaction): Boolean {
    assertTrue(controller.shouldAcceptInteraction(interaction, emptyList()))
    return controller.shouldShowMarker(interaction, emptyList())
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
  fun the_tooltip_shows_as_soon_as_the_finger_lands() {
    assertTrue(shows(Interaction.Press(Point(100f, 50f))))
  }

  @Test
  fun moving_within_the_pan_slop_scrubs() {
    shows(Interaction.Press(Point(100f, 50f)))

    assertTrue(shows(Interaction.Move(Point(159f, 50f))))
    assertTrue(shows(Interaction.Move(Point(41f, 50f))))
  }

  @Test
  fun moving_past_the_pan_slop_hides_it_as_the_chart_starts_panning() {
    shows(Interaction.Press(Point(100f, 50f)))

    assertFalse(shows(Interaction.Move(Point(161f, 50f))))
  }

  @Test
  fun once_panning_it_stays_hidden_even_back_inside_the_slop() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.Move(Point(200f, 50f)))

    assertFalse(shows(Interaction.Move(Point(100f, 50f))))
  }

  @Test
  fun scrolling_the_page_up_or_down_hides_it() {
    shows(Interaction.Press(Point(100f, 50f)))

    assertFalse(shows(Interaction.Move(Point(100f, 71f))))
  }

  @Test
  fun lifting_the_finger_hides_it() {
    shows(Interaction.Press(Point(100f, 50f)))

    assertFalse(shows(Interaction.Release(Point(100f, 50f))))
  }

  @Test
  fun a_new_press_starts_over_after_a_pan() {
    shows(Interaction.Press(Point(100f, 50f)))
    shows(Interaction.Move(Point(300f, 50f)))
    shows(Interaction.Release(Point(300f, 50f)))

    assertTrue(shows(Interaction.Press(Point(300f, 50f))))
  }

  @Test
  fun moves_without_a_press_are_ignored() {
    // A mouse hovering, or the second finger of a pinch after Vico sent Release.
    assertFalse(controller.shouldAcceptInteraction(Interaction.Move(Point(1f, 1f)), emptyList()))
  }
}