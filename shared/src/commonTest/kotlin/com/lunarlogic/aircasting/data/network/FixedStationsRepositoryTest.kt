package com.lunarlogic.aircasting.data.network

import com.lunarlogic.aircasting.domain.GeoSquare
import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.domain.Pollutant
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Instant

class FixedStationsRepositoryTest {

  private val fixedNow = Instant.parse("2024-01-15T12:00:00Z")
  private val clock = object : Clock { override fun now() = fixedNow }

  @Test
  fun fetches_and_maps_active_stations_for_a_pollutant() = runTest {
    var capturedQ: String? = null
    val engine = MockEngine { request ->
      capturedQ = request.url.parameters["q"]
      respond(
        content = REGION_JSON,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val repo = FixedStationsRepository(
      api = FixedStationsApi(createAircastingHttpClient(engine)),
      clock = clock,
    )

    val stations = repo.activeStations(
      area = GeoSquare(north = 40.8, south = 40.7, east = -73.9, west = -74.1),
      pollutant = Pollutant.PM25,
    )
    // response decoded + mapped to domain (chains step-2 mapper + step-1 banding)
    val s = stations.single()
    assertEquals("Downtown Civic Station", s.name)
    assertEquals(MeasurementLevel.LOW, s.level)

    // outgoing q built from box + pollutant + clock window
    assertNotNull(capturedQ)
    assertContains(capturedQ!!, "\"sensor_name\":\"government-pm2.5\"")
    assertContains(capturedQ!!, "\"north\":40.8")
    assertContains(capturedQ!!, "\"time_to\":\"${fixedNow.epochSeconds}\"")
  }
  private companion object {
    val REGION_JSON = """                                                                                                                                                                               
        {                                                                                                                                                                                                 
          "fetchableSessionsCount": 1,                                                                                                                                                                    
          "sessions": [{                                                                                                                                                                                  
            "id": 123, "uuid": "abc", "title": "Downtown Civic Station",                                                                                                                                  
            "latitude": 40.71, "longitude": -74.0, "is_indoor": false,                                                                                                                                    
            "last_hour_average": 8.2, "end_time_local": "2024-01-15T09:30:00",                                                                                                                            
            "streams": { "Sensor": {                                                                                                                                                                      
              "sensor_name": "government-pm2.5", "measurement_type": "Particulate Matter",                                                                                                                
              "threshold_very_low": 0, "threshold_low": 12, "threshold_medium": 35,                                                                                                                       
              "threshold_high": 55, "threshold_very_high": 150,                                                                                                                                           
              "last_measurement_value": 8.2, "unit_symbol": "µg/m³"                                                                                                                                       
            }}                                                                                                                                                                                            
          }]                                                                                                                                                                                              
        }                                                                                                                                                                                                 
      """.trimIndent()
  }
}
