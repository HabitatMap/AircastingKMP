package pl.llp.aircasting.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.SensorThresholdQueries
import pl.llp.aircasting.data.network.ThresholdSetsApi
import kotlin.math.roundToLong

/**
 * Replaces the stored server defaults and aliases with GET /api/v3/threshold_sets.
 * User overrides are untouched. Throws on network failure, before any write.
 */
class ThresholdDefaultsSync(
  private val api: ThresholdSetsApi,
  private val queries: SensorThresholdQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  suspend fun refresh() {
    val sets = api.thresholdSets().thresholdSets
    if (sets.isEmpty()) return

    withContext(ioDispatcher) {
      queries.transaction {
        queries.deleteDefaults()
        queries.deleteAliases()
        sets.forEach { set ->
          val t = set.thresholds
          queries.insertDefault(
            sensor_name = set.sensorName,
            very_low = t.veryLow.roundToLong(),
            low = t.low.roundToLong(),
            medium = t.medium.roundToLong(),
            high = t.high.roundToLong(),
            very_high = t.veryHigh.roundToLong(),
          )
          set.aliases.forEach { queries.insertAlias(alias = it, sensor_name = set.sensorName) }
        }
      }
    }
  }
}