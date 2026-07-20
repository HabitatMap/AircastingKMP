package com.lunarlogic.aircasting.data.network

import kotlin.test.Test
import kotlin.test.assertEquals


class FixedStationsQueryTest {
  @Test
  fun encodes_to_the_q_json_the_backend_expects() {
    val q = FixedStationsQuery(
      timeFrom = "1700000000", timeTo = "1700086400",
      west = -74.1, east = -73.9, south = 40.7, north = 40.8,
      sensorName = "government-pm2.5", unitSymbol = "µg/m³",
      measurementType = "Particulate Matter",
    )
    assertEquals(
      """{"time_from":"1700000000","time_to":"1700086400","tags":"","usernames":"",""" +
        """"west":-74.1,"east":-73.9,"south":40.7,"north":40.8,""" +
        """"sensor_name":"government-pm2.5","unit_symbol":"µg/m³","measurement_type":"Particulate Matter"}""",
      AircastingJson.encodeToString(q),
    )
  }
}