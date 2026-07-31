package pl.llp.aircasting.data.network

import pl.llp.aircasting.domain.FixedStation
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.SensorThreshold

fun SessionInRegionDto.toFixedStation(): FixedStation {
  val sensor = streams.sensor
  return FixedStation(
    id = id,
    uuid = uuid,
    name = title,
    location = GeoLocation(latitude, longitude),
    isIndoor = isIndoor,
    sensorName = sensor.sensorName,
    unitSymbol = sensor.unitSymbol,
    value = lastHourAverage,
    threshold = SensorThreshold(
      sensorName = sensor.sensorName,
      veryLow = sensor.thresholdVeryLow, low = sensor.thresholdLow,
      medium = sensor.thresholdMedium, high = sensor.thresholdHigh,
      veryHigh = sensor.thresholdVeryHigh,
    ),
    updatedAt = parseRegionTimestamp(endTimeLocal),
  )
}