package pl.llp.aircasting.home

import pl.llp.aircasting.domain.GeoLocation

class FakeLocationProvider(private val location: GeoLocation?) : LocationProvider {
  override suspend fun current() = location
}