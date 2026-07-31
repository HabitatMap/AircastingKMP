package pl.llp.aircasting.home

import pl.llp.aircasting.domain.GeoLocation

interface LocationProvider {
  suspend fun current(): GeoLocation?
}