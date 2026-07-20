package com.lunarlogic.aircasting.data.network

import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampsTest {
  @Test
  fun region_local_time_is_read_as_utc_wall_clock() {
    // legacy: DateConverter.fromString(endTimeLocal, UTC), format yyyy-MM-dd'T'HH:mm:ss
    val instant = parseRegionTimestamp("2024-01-15T09:30:00")
    assertEquals("2024-01-15T09:30:00Z", instant.toString())   // numerals preserved, no offset shift
  }
}