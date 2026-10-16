package pl.llp.aircasting.domain.session

data class MeasurementStream(
  val id: Long,
  val sessionUuid: String,
  val sensorName: String,
  val unitSymbol: String,
  /** Assigned by the backend at session create; goes into each binary upload frame. */
  val sensorTypeId: Int? = null,
)