package com.lunarlogic.aircasting.data.network

import kotlinx.serialization.json.Json

val AircastingJson = Json {
  ignoreUnknownKeys = true
  encodeDefaults = true
}