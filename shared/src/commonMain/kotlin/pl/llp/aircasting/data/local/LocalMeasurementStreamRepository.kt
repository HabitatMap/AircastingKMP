package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.MeasurementStreamQueries
import pl.llp.aircasting.data.local.db.Measurement_streams
import pl.llp.aircasting.domain.session.MeasurementStream
import pl.llp.aircasting.domain.session.MeasurementStreamRepository

class LocalMeasurementStreamRepository(
  private val queries: MeasurementStreamQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MeasurementStreamRepository {

  override suspend fun findOrCreate(
    sessionUuid: String,
    sensorName: String,
    unitSymbol: String,
    sensorTypeId: Int?,
  ): Long = withContext(ioDispatcher) {
    queries.transactionWithResult {
      queries.insertStreamIfAbsent(
        session_uuid = sessionUuid,
        sensor_name = sensorName,
        unit_symbol = unitSymbol,
        sensor_type_id = sensorTypeId?.toLong(),
      )
      queries.selectStream(sessionUuid, sensorName).executeAsOne().id
    }
  }

  override suspend fun findBySession(sessionUuid: String): List<MeasurementStream> =
    withContext(ioDispatcher) {
      queries.selectStreamsBySession(sessionUuid).executeAsList().map { it.toDomain() }
    }

  override fun observeBySession(sessionUuid: String): Flow<List<MeasurementStream>> =
    queries.selectStreamsBySession(sessionUuid)
      .asFlow()
      .mapToList(ioDispatcher)
      .map { rows -> rows.map { it.toDomain() } }

  override suspend fun setSensorTypeId(streamId: Long, sensorTypeId: Int) =
    withContext(ioDispatcher) { queries.updateSensorTypeId(sensorTypeId.toLong(), streamId) }
}

private fun Measurement_streams.toDomain() = MeasurementStream(
  id = id,
  sessionUuid = session_uuid,
  sensorName = sensor_name,
  unitSymbol = unit_symbol,
  sensorTypeId = sensor_type_id?.toInt(),
)