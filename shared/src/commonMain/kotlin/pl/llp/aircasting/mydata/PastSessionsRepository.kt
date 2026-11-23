package pl.llp.aircasting.mydata

import kotlinx.coroutines.flow.Flow
import pl.llp.aircasting.domain.session.Session

/** One stream's whole-session aggregates; all null when the stream never got a reading. */
data class StreamSummary(
  val sensorName: String,
  val unitSymbol: String,
  val avg: Double? = null,
  val min: Double? = null,
  val max: Double? = null,
)

data class PastSession(val session: Session, val streams: List<StreamSummary>)

interface PastSessionsRepository {
  /** Finished mobile sessions (AirBeam and phone mic), newest first, each with its streams' aggregates. */
  fun observe(): Flow<List<PastSession>>
}
