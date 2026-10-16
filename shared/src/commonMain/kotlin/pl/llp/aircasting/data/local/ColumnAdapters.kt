package pl.llp.aircasting.data.local

import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Instant

internal val instantAdapter = object : ColumnAdapter<Instant, Long> {
  override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)
  override fun encode(value: Instant): Long = value.toEpochMilliseconds()
}

internal val stringListAdapter = object : ColumnAdapter<List<String>, String> {
  override fun decode(databaseValue: String): List<String> =
    if (databaseValue.isEmpty()) emptyList() else databaseValue.split(",")

  override fun encode(value: List<String>): String = value.joinToString(",")
}