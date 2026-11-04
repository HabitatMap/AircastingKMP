package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** Server default thresholds per canonical parameter. Public, no auth. */
class ThresholdSetsApi(private val client: HttpClient) {
  suspend fun thresholdSets(): ThresholdSetsDto = client.get(ApiEndpoints.V3ThresholdSets).body()
}