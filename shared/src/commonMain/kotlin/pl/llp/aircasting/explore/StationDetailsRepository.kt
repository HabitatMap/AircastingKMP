package pl.llp.aircasting.explore

import pl.llp.aircasting.data.network.ExploreMapApi
import pl.llp.aircasting.data.network.toStationDetails
import pl.llp.aircasting.domain.StationDetails

interface StationDetailsRepository {
  suspend fun station(uuid: String): StationDetails
}

class NetworkStationDetailsRepository(private val api: ExploreMapApi) : StationDetailsRepository {
  override suspend fun station(uuid: String): StationDetails = api.station(uuid).toStationDetails()
}