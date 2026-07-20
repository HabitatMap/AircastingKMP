package com.lunarlogic.aircasting.data.network

import com.lunarlogic.aircasting.domain.MeasurementLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class FixedStationMapperTest {
  private val json = """                                                                                                                          
      {                                                                                                                                             
        "fetchableSessionsCount": 1,                                                                                                                
        "sessions": [{                                                                                                                              
          "id": 123, "uuid": "abc", "title": "Downtown Civic Station",                                                                              
          "type": "FixedSession", "username": "gov",                                                                                                
          "latitude": 40.71, "longitude": -74.0, "is_indoor": false,                                                                                
          "last_hour_average": 8.2,                                                                                                                 
          "start_time_local": "2024-01-15T00:00:00",                                                                                                
          "end_time_local": "2024-01-15T09:30:00",                                                                                                  
          "streams": { "Sensor": {                                                                                                                  
            "sensor_name": "government-pm2.5", "measurement_type": "Particulate Matter",                                                            
            "measurement_short_type": "PM",                                                                                                         
            "threshold_very_low": 0, "threshold_low": 12, "threshold_medium": 35,                                                                   
            "threshold_high": 55, "threshold_very_high": 150,                                                                                       
            "last_measurement_value": 8.2, "unit_name": "micrograms", "unit_symbol": "µg/m³"                                                        
          }}                                                                                                                                        
        }]                                                                                                                                          
      }                                                                                                                                             
    """.trimIndent()
  @Test
  fun maps_region_response_to_domain_fixed_station() {
    val dto = AircastingJson.decodeFromString(SessionsInRegionDto.serializer(), json)
    val station = dto.sessions.single().toFixedStation()

    assertEquals(123L, station.id)
    assertEquals("Downtown Civic Station", station.name)
    assertEquals(8.2, station.value)
    assertEquals("government-pm2.5", station.sensorName)
    assertEquals("µg/m³", station.unitSymbol)
    assertEquals(40.71, station.location.latitude)
    assertEquals("2024-01-15T09:30:00Z", station.updatedAt.toString())
    assertEquals(MeasurementLevel.LOW, station.level)   // 8.2 in [0,12] — ties to step 1 banding
  }
}