package pl.llp.aircasting.domain

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.SensorThresholdQueries

interface ThresholdsRepository {
  /** Live: emits again whenever a default, alias or override changes. */
  fun observe(): Flow<Thresholds>
  /** Stores [threshold] as the user's override for its canonical sensor. Local only. */
  suspend fun save(threshold: SensorThreshold)
  /** Drops the user's override for canonical [sensorName], back to the server default. */
  suspend fun resetToDefault(sensorName: String)
}

class LocalThresholdsRepository(
  private val queries: SensorThresholdQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ThresholdsRepository {
  override fun observe(): Flow<Thresholds> = combine(
    queries.selectDefaults(::sensorThreshold).asFlow().mapToList(ioDispatcher),
    queries.selectAliases().asFlow().mapToList(ioDispatcher),
    queries.selectOverrides(::sensorThreshold).asFlow().mapToList(ioDispatcher),
  ) { defaults, aliases, overrides ->
    Thresholds(defaults, aliases.associate { it.alias to it.sensor_name }, overrides)
  }
  override suspend fun save(threshold: SensorThreshold) {
    withContext(ioDispatcher) {
      queries.upsertOverride(
        sensor_name = threshold.sensorName,
        very_low = threshold.veryLow.toLong(),
        low = threshold.low.toLong(),
        medium = threshold.medium.toLong(),
        high = threshold.high.toLong(),
        very_high = threshold.veryHigh.toLong(),
      )
    }
  }

  override suspend fun resetToDefault(sensorName: String) {
    withContext(ioDispatcher) { queries.deleteOverride(sensorName) }
  }
}

/** Row mapper shared by both band tables: same columns, SQLite INTEGER → Long. */
private fun sensorThreshold(
  sensorName: String, veryLow: Long, low: Long, medium: Long, high: Long, veryHigh: Long,
) = SensorThreshold(sensorName, veryLow.toInt(), low.toInt(), medium.toInt(), high.toInt(), veryHigh.toInt())
