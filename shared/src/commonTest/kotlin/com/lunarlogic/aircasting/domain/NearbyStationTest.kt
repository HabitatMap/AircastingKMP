package com.lunarlogic.aircasting.domain

import kotlin.math.abs
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class NearbyStationTest {

  @Test
  fun distance_to_same_point_is_zero() {
    val p = GeoLocation(40.71, -74.0)
    assertEquals(0.0, p.distanceMetersTo(p))
  }

  @Test
  fun distance_of_one_degree_longitude_at_equator_is_about_111km() {
    val d = GeoLocation(0.0, 0.0).distanceMetersTo(GeoLocation(0.0, 1.0))
    assertTrue(abs(d - 111_194.9) < 1.0, "expected ~111195m, got $d")
  }

  @Test
  fun distance_of_one_degree_latitude_is_about_111km() {
    // proves the formula uses the latitude term, not just longitude
    val d = GeoLocation(0.0, 0.0).distanceMetersTo(GeoLocation(1.0, 0.0))
    assertTrue(abs(d - 111_194.9) < 1.0, "expected ~111195m, got $d")
  }

  @Test
  fun merges_pollutant_readings_by_physical_location() {
    val central = GeoLocation(40.78, -73.96)
    val downtown = GeoLocation(40.71, -74.0)

    val byPollutant = mapOf(
      Pollutant.PM25 to listOf(
        fixedStation(central, Pollutant.PM25, value = 4.2),
        fixedStation(downtown, Pollutant.PM25, value = 8.2),
      ),
      Pollutant.NO2 to listOf(fixedStation(central, Pollutant.NO2, value = 9.1)),
      Pollutant.OZONE to listOf(fixedStation(central, Pollutant.OZONE, value = 4.2)),
    )

    val merged = mergeByStation(byPollutant)

    assertEquals(2, merged.size)
    val centralStation = merged.single { it.location == central }
    assertEquals(
      setOf(Pollutant.PM25, Pollutant.NO2, Pollutant.OZONE),
      centralStation.readings.map { it.pollutant }.toSet(),
    )
    val downtownStation = merged.single { it.location == downtown }
    assertEquals(listOf(Pollutant.PM25), downtownStation.readings.map { it.pollutant })
  }

  @Test
  fun merged_station_uses_freshest_reading_time() {
    val loc = GeoLocation(40.78, -73.96)
    val older = Instant.parse("2024-01-15T08:00:00Z")
    val newer = Instant.parse("2024-01-15T09:30:00Z")

    val merged = mergeByStation(
      mapOf(
        Pollutant.PM25 to listOf(fixedStation(loc, Pollutant.PM25, updatedAt = older)),
        Pollutant.NO2 to listOf(fixedStation(loc, Pollutant.NO2, updatedAt = newer)),
      ),
    )

    assertEquals(newer, merged.single().updatedAt)
  }

  @Test
  fun nearest_picks_closest_station_and_reports_distance() {
    val me = GeoLocation(40.78, -73.96)
    val near = stationAt(GeoLocation(40.781, -73.961)) // ~140m
    val far = stationAt(GeoLocation(41.0, -74.2))       // ~30km

    val result = listOf(far, near).nearestTo(me)!!

    assertEquals(near, result.station)
    assertTrue(result.distanceMeters < 200.0, "got ${result.distanceMeters}")
  }

  @Test
  fun nearest_of_empty_list_is_null() {
    assertNull(emptyList<NearbyStation>().nearestTo(GeoLocation(0.0, 0.0)))
  }

  @Test
  fun square_around_spans_the_radius_in_km_and_stays_symmetric() {
    val center = GeoLocation(40.78, -73.96)
    val radiusKm = 15.0
    val box = center.squareAround(radiusKm)

    // symmetric around the center point
    assertEquals(center.latitude, (box.north + box.south) / 2, 1e-9)
    assertEquals(center.longitude, (box.east + box.west) / 2, 1e-9)

    // each edge sits ~radiusKm from center — ground-truthed with haversine, not the formula
    val toNorthEdge = center.distanceMetersTo(GeoLocation(box.north, center.longitude))
    val toEastEdge = center.distanceMetersTo(GeoLocation(center.latitude, box.east))
    assertEquals(radiusKm * 1000, toNorthEdge, 100.0) // within 100 m
    assertEquals(radiusKm * 1000, toEastEdge, 100.0)
  }


  @Test
  fun by_distance_sorts_all_stations_nearest_first() {
    val me = GeoLocation(40.78, -73.96)
    val near = stationAt(GeoLocation(40.781, -73.961))
    val mid = stationAt(GeoLocation(40.80, -73.98))
    val far = stationAt(GeoLocation(40.85, -74.05))

    val ranked = listOf(far, near, mid).byDistanceFrom(me)

    assertEquals(listOf(near, mid, far), ranked.map { it.station })
    assertTrue(ranked[0].distanceMeters < ranked[1].distanceMeters)
    assertTrue(ranked[1].distanceMeters < ranked[2].distanceMeters)
  }

  private fun fixedStation(
    location: GeoLocation,
    pollutant: Pollutant,
    value: Double = 5.0,
    updatedAt: Instant = Instant.parse("2024-01-15T09:30:00Z"),
  ) = FixedStation(
    id = 0,
    uuid = "u",
    name = "Central Park Station",
    location = location,
    isIndoor = false,
    sensorName = pollutant.sensorName,
    unitSymbol = pollutant.unitSymbol,
    value = value,
    threshold = SensorThreshold(pollutant.sensorName, 0, 12, 35, 55, 150),
    updatedAt = updatedAt,
  )

  private fun stationAt(location: GeoLocation) = NearbyStation(
    name = "Central Park Station",
    location = location,
    isIndoor = false,
    readings = emptyList(),
    updatedAt = Instant.parse("2024-01-15T09:30:00Z"),
  )
}

