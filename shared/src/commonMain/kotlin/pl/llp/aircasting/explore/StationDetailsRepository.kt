package pl.llp.aircasting.explore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.llp.aircasting.data.network.ExploreMapApi
import pl.llp.aircasting.data.network.toStationDetails
import pl.llp.aircasting.domain.StationDetails

interface StationDetailsRepository {
  suspend fun station(uuid: String): StationDetails
}

class NetworkStationDetailsRepository(private val api: ExploreMapApi) : StationDetailsRepository {
  override suspend fun station(uuid: String): StationDetails = api.station(uuid).toStationDetails()
}

fun StationDetailsRepository.observe(uuid: String): Flow<StationDetails> = flow { emit(station(uuid)) }