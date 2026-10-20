package pl.llp.aircasting.data.network

import kotlinx.serialization.json.Json

val AircastingJson = Json {
  ignoreUnknownKeys = true
  encodeDefaults = true
  // Omit nulls; non-null defaults (contribute = true) still go out.
  explicitNulls = false
}