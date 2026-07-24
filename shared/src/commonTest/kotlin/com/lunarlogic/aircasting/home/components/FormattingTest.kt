package com.lunarlogic.aircasting.home.components

import com.lunarlogic.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FormattingTest {

  @Test
  fun age_under_a_minute_reads_just_now() {
    val t = Instant.fromEpochSeconds(1000)
    assertEquals("just now", ageLabel(t, Instant.fromEpochSeconds(1030), EnStrings))
  }

  @Test
  fun age_floors_to_whole_minutes() {
    val t = Instant.fromEpochSeconds(0)
    assertEquals("2 min ago", ageLabel(t, Instant.fromEpochSeconds(150), EnStrings)) // 2m30s → 2
  }

  @Test
  fun age_floors_to_whole_hours() {
    val t = Instant.fromEpochSeconds(0)
    assertEquals("3 hr ago", ageLabel(t, Instant.fromEpochSeconds(3 * 3600 + 5), EnStrings))
  }
}
