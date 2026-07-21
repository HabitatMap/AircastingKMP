package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.data.network.FixedStationsApi
import com.lunarlogic.aircasting.data.network.FixedStationsRepository
import com.lunarlogic.aircasting.data.network.createAircastingHttpClient
import com.lunarlogic.aircasting.domain.GeoLocation
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.distanceMetersTo
import com.lunarlogic.aircasting.domain.squareAround
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class HomeRepositoryTest {

  private val clock = object : Clock {
    override fun now() = Instant.parse("2024-01-15T12:00:00Z")
  }

  @Test
  fun no_location_short_circuits_to_NoLocation_without_any_network_call() = runTest {
    var called = false
    val engine = MockEngine { called = true; ok(regionJson("")) }

    val state = homeRepo(engine).load(userLocation = null)

    assertEquals(HomeUiState.NoLocation, state)
    assertFalse(called, "must not hit the network when location is unknown")
  }

  @Test
  fun located_with_stations_merges_nearest_into_Loaded() = runTest {
    // PM2.5 returns near + far; NO2 + Ozone return only the near site (identical coords → merges)
    val engine = MockEngine { request ->
      val q = request.url.parameters["q"] ?: ""
      when {
        "government-pm2.5" in q -> ok(
          regionJson(
            session(
              1,
              NEAR_LAT,
              NEAR_LNG,
              "government-pm2.5",
              "µg/m³",
              "Particulate Matter",
              4.2
            ) + "," +
              session(2, 41.0, -74.2, "government-pm2.5", "µg/m³", "Particulate Matter", 8.2),
          )
        )

        "government-no2" in q -> ok(
          regionJson(
            session(3, NEAR_LAT, NEAR_LNG, "government-no2", "ppb", "Nitrogen Dioxide", 9.1)
          )
        )

        "government-ozone" in q -> ok(
          regionJson(
            session(4, NEAR_LAT, NEAR_LNG, "government-ozone", "ppb", "Ozone", 4.2)
          )
        )

        else -> ok(regionJson(""))
      }
    }
    val state = homeRepo(engine).load(GeoLocation(40.78, -73.96))

    assertIs<HomeUiState.Loaded>(state)
    assertEquals("Central Park Station", state.stationName)
    assertEquals(
      setOf(Pollutant.PM25, Pollutant.NO2, Pollutant.OZONE),
      state.readings.map { it.pollutant }
        .toSet(),
    )
    assertTrue(state.distanceMeters < 300.0, "nearest is ~140m, got ${state.distanceMeters}")
  }

  @Test
  fun located_but_no_stations_returns_NoReadings() = runTest {
    val engine = MockEngine { ok(regionJson("")) }

    assertEquals(HomeUiState.NoReadings, homeRepo(engine).load(GeoLocation(40.78, -73.96)))
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

  private fun homeRepo(engine: MockEngine) = HomeRepository(
    FixedStationsRepository(FixedStationsApi(createAircastingHttpClient(engine)), clock),
  )

  private companion object {
    const val NEAR_LAT = 40.781
    const val NEAR_LNG = -73.961

    fun regionJson(sessions: String) =
      """{"fetchableSessionsCount":0,"sessions":[$sessions]}"""

    fun session(
      id: Long, lat: Double, lng: Double,
      sensor: String, unit: String, type: String, value: Double,
    ) =
      """                                                                                                                                                                                             
        {"id":$id,"uuid":"u$id","title":"Central Park Station",                                                                                                                                           
         "latitude":$lat,"longitude":$lng,"is_indoor":false,                                                                                                                                              
         "last_hour_average":$value,"end_time_local":"2024-01-15T09:30:00",                                                                                                                               
         "streams":{"Sensor":{"sensor_name":"$sensor","measurement_type":"$type",                                                                                                                         
           "threshold_very_low":0,"threshold_low":12,"threshold_medium":35,                                                                                                                               
           "threshold_high":55,"threshold_very_high":150,                                                                                                                                                 
           "last_measurement_value":$value,"unit_symbol":"$unit"}}}                                                                                                                                       
      """
  }
}

private fun MockRequestHandleScope.ok(body: String) =
  respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

