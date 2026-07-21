package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.data.network.FixedStationsApi
import com.lunarlogic.aircasting.data.network.FixedStationsRepository
import com.lunarlogic.aircasting.data.network.createAircastingHttpClient
import com.lunarlogic.aircasting.domain.GeoLocation
import com.lunarlogic.aircasting.domain.Pollutant
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

  private val clock = object : Clock { override fun now() = Instant.parse("2024-01-15T12:00:00Z") }

  @Test
  fun no_location_gives_NoLocation_card_and_empty_carousel_without_any_network_call() = runTest {
    var called = false
    val engine = MockEngine { called = true; ok(regionJson("")) }

    val state = homeRepo(engine).load(userLocation = null)

    assertEquals(HomeUiState.AirQuality.NoLocation, state.airQuality)
    assertTrue(state.nearby.isEmpty())
    assertFalse(called, "must not hit the network when location is unknown")
  }

  @Test
  fun located_fills_the_nearest_card_and_the_full_sorted_carousel() = runTest {
    // PM2.5 returns near + far; NO2 + Ozone only the near site (identical coords → merges)
    val engine = MockEngine { request ->
      val q = request.url.parameters["q"] ?: ""
      when {
        "government-pm2.5" in q -> ok(regionJson(
          session(1, NEAR_LAT, NEAR_LNG, "government-pm2.5", "µg/m³", "Particulate Matter", 4.2) + "," +
            session(2, FAR_LAT, FAR_LNG, "government-pm2.5", "µg/m³", "Particulate Matter", 8.2),
        ))
        "government-no2" in q -> ok(regionJson(
          session(3, NEAR_LAT, NEAR_LNG, "government-no2", "ppb", "Nitrogen Dioxide", 9.1)))
        "government-ozone" in q -> ok(regionJson(
          session(4, NEAR_LAT, NEAR_LNG, "government-ozone", "ppb", "Ozone", 4.2)))
        else -> ok(regionJson(""))
      }
    }

    val state = homeRepo(engine).load(GeoLocation(40.78, -73.96))

    // AQ card = nearest, with all 3 pollutants merged
    val aq = state.airQuality
    assertIs<HomeUiState.AirQuality.Loaded>(aq)
    assertEquals("Central Park Station", aq.stationName)
    assertEquals(
      setOf(Pollutant.PM25, Pollutant.NO2, Pollutant.OZONE),
      aq.readings.map { it.pollutant }.toSet(),
    )
    assertTrue(aq.distanceMeters < 300.0, "nearest ~140m, got ${aq.distanceMeters}")

    // Carousel = both physical sites, nearest first
    assertEquals(2, state.nearby.size)
    assertEquals(NEAR_LAT, state.nearby.first().station.location.latitude)
    assertTrue(state.nearby[0].distanceMeters < state.nearby[1].distanceMeters)
  }

  @Test
  fun located_but_no_stations_gives_NoReadings_card_and_empty_carousel() = runTest {
    val engine = MockEngine { ok(regionJson("")) }

    val state = homeRepo(engine).load(GeoLocation(40.78, -73.96))

    assertEquals(HomeUiState.AirQuality.NoReadings, state.airQuality)
    assertTrue(state.nearby.isEmpty())
  }

  private fun homeRepo(engine: MockEngine) = HomeRepository(
    FixedStationsRepository(FixedStationsApi(createAircastingHttpClient(engine)), clock),
  )

  private companion object {
    const val NEAR_LAT = 40.781
    const val NEAR_LNG = -73.961
    const val FAR_LAT = 40.85
    const val FAR_LNG = -74.05

    fun regionJson(sessions: String) =
      """{"fetchableSessionsCount":0,"sessions":[$sessions]}"""

    fun session(
      id: Long, lat: Double, lng: Double,
      sensor: String, unit: String, type: String, value: Double,
    ) = """                                                                                                                                                                                             
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