package pl.llp.aircasting.domain

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.SensorThresholdQueries
import pl.llp.aircasting.data.local.db.Sensor_thresholds

interface ThresholdsRepository {
  fun observeAll(): Flow<List<SensorThreshold>>
  fun observeBySensorName(sensorName: String): Flow<SensorThreshold?>
  suspend fun findBySensorName(sensorName: String): SensorThreshold?
  suspend fun save(threshold: SensorThreshold)
  suspend fun delete(sensorName: String)
}


class LocalThresholdsRepository(
  private val queries: SensorThresholdQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ThresholdsRepository {
  override fun observeAll(): Flow<List<SensorThreshold>> {
    return queries.selectAllThresholds()
      .asFlow()
      .mapToList(ioDispatcher)
      .map { list -> list.map { it.toDomain() } }
  }
  override fun observeBySensorName(sensorName: String): Flow<SensorThreshold?> {
    return queries.selectThresholdBySensorName(sensorName)
      .asFlow()
      .mapToOneOrNull(ioDispatcher)
      .map { it?.toDomain() }
  }
  override suspend fun findBySensorName(sensorName: String): SensorThreshold? {
    return withContext(ioDispatcher) {
      queries.selectThresholdBySensorName(sensorName).executeAsOneOrNull()?.toDomain()
    }
  }
  override suspend fun save(threshold: SensorThreshold) {
    withContext(ioDispatcher) {
      queries.insertOrReplaceThreshold(
        sensor_name = threshold.sensorName,
        very_low = threshold.veryLow.toLong(),
        low = threshold.low.toLong(),
        medium = threshold.medium.toLong(),
        high = threshold.high.toLong(),
        very_high = threshold.veryHigh.toLong(),
      )
    }
  }
  override suspend fun delete(sensorName: String) {
    withContext(ioDispatcher) {
      queries.deleteThresholdBySensorName(sensorName)
    }
  }
  private fun Sensor_thresholds.toDomain(): SensorThreshold {
    return SensorThreshold(
      sensorName = sensor_name,
      veryLow = very_low.toInt(),
      low = low.toInt(),
      medium = medium.toInt(),
      high = high.toInt(),
      veryHigh = very_high.toInt(),
    )
  }
}