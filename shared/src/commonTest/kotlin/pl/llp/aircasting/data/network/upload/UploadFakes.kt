package pl.llp.aircasting.data.network.upload

import kotlinx.coroutines.flow.Flow
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementStream
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.UploadOutbox

/** Mirrors LocalUploadOutbox: oldest first, at most [limit]; acknowledge deletes. */
internal class FakeUploadOutbox : UploadOutbox {
  val rows = mutableListOf<Measurement>()
  override suspend fun pending(sessionUuid: String, limit: Int) = rows.sortedBy { it.time }.take(limit)
  override suspend fun acknowledge(measurements: List<Measurement>) { rows -= measurements.toSet() }
}

internal class FakeStreamRepository(streams: List<MeasurementStream>) : MeasurementStreamRepository {
  val streams = streams.toMutableList()
  override suspend fun findBySession(sessionUuid: String) = streams.filter { it.sessionUuid == sessionUuid }
  override suspend fun setSensorTypeId(streamId: Long, sensorTypeId: Int) {
    val index = streams.indexOfFirst { it.id == streamId }
    streams[index] = streams[index].copy(sensorTypeId = sensorTypeId)
  }
  override suspend fun findOrCreate(sessionUuid: String, sensorName: String, unitSymbol: String, sensorTypeId: Int?): Long = error("unused")
  override fun observeBySession(sessionUuid: String): Flow<List<MeasurementStream>> = error("unused")
}