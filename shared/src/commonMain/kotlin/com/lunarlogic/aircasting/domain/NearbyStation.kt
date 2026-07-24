package com.lunarlogic.aircasting.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Instant

/** One pollutant's reading at a physical station */
data class PollutantReading(
  val pollutant: Pollutant,
  val value: Double,
  val unitSymbol: String,
  val level: MeasurementLevel,
)

/** A physical station reassembled from its per-pollutant sensor sessions. */
data class NearbyStation(
  val name: String,
  val location: GeoLocation,
  val isIndoor: Boolean,
  val readings: List<PollutantReading>,
  val updatedAt: Instant,
)

data class StationWithDistance(
  val station: NearbyStation,
  val distanceMeters: Double,
)

private const val EARTH_RADIUS_M = 6_371_000.0

/**
 * Great-circle distance in meters.
 *
 * Purpose: figure out which station is closest to you, and show how far ("0.4 mile away" in the design).
 * Haversine = standard formula for great-circle distance (shortest path over sphere surface).
 */

fun GeoLocation.distanceMetersTo(other: GeoLocation): Double {
  val dLat = (other.latitude - latitude).toRadians()
  val dLon = (other.longitude - longitude).toRadians()
  val a = sin(dLat / 2) * sin(dLat / 2) +
    cos(latitude.toRadians()) * cos(other.latitude.toRadians()) *
    sin(dLon / 2) * sin(dLon / 2)
  return 2 * EARTH_RADIUS_M * asin(sqrt(a))
}

private fun Double.toRadians() = this * PI / 180.0

/**
 * Collapse per-pollutant sensor lists into physical stations, keyed by location.
 * Each station carries one reading per pollutant it reports.
 */
fun mergeByStation(byPollutant: Map<Pollutant, List<FixedStation>>): List<NearbyStation> =
  byPollutant
    .flatMap { (pollutant, stations) -> stations.map { pollutant to it } }
    .groupBy { (_, station) -> station.location }
    .map { (location, entries) ->
      val sample = entries.first().second
      NearbyStation(
        name = sample.name,
        location = location,
        isIndoor = sample.isIndoor,
        readings = entries.map { (pollutant, s) ->
          PollutantReading(pollutant, s.value, s.unitSymbol, s.level)
        },
        updatedAt = entries.maxOf { it.second.updatedAt },
      )
    }

/** All stations ranked by true distance from [location], nearest first. */
fun List<NearbyStation>.byDistanceFrom(location: GeoLocation): List<StationWithDistance> =
  map { StationWithDistance(it, location.distanceMetersTo(it.location)) }
    .sortedBy { it.distanceMeters }

/** Nearest station to [location]; null if the list is empty. */
fun List<NearbyStation>.nearestTo(location: GeoLocation): StationWithDistance? =
  byDistanceFrom(location).firstOrNull()

/** Square roughly ±[radiusKm] around this point (per-axis degree conversion). */
fun GeoLocation.squareAround(radiusKm: Double = 100.0): GeoSquare {
  val latDelta = radiusKm / 111.2
  val lngDelta = radiusKm / (111.2 * cos(latitude.toRadians()))
  return GeoSquare(
    north = latitude + latDelta,
    south = latitude - latDelta,
    east = longitude + lngDelta,
    west = longitude - lngDelta,
  )
}

fun List<PollutantReading>.worstLevel(): MeasurementLevel? =
  maxByOrNull { it.level.ordinal }?.level