package com.lunarlogic.aircasting.domain

enum class Pollutant(
  val sensorName: String,
  val unitSymbol: String,
  val measurementType: String,
) {
  PM25("government-pm2.5", "µg/m³", "Particulate Matter"),
  NO2("government-no2", "ppb", "Nitrogen Dioxide"),
  OZONE("government-ozone", "ppb", "Ozone"),
}

/** Lat/lng bounding box for a region query. */
data class GeoSquare(val north: Double, val south: Double, val east: Double, val west: Double)