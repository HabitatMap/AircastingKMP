package com.lunarlogic.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Thin wrapper over the region-search endpoint. One sensor triple per call. */
class FixedStationsApi(private val client: HttpClient) {
  suspend fun activeInRegion(query: FixedStationsQuery): SessionsInRegionDto =
    client.get("/api/fixed/active/sessions.json") {
      parameter("q", AircastingJson.encodeToString(query))
    }.body()
}
