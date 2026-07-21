package com.lunarlogic.aircasting.data.network

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

fun parseRegionTimestamp(local: String): Instant =
  LocalDateTime.parse(local).toInstant(TimeZone.UTC)