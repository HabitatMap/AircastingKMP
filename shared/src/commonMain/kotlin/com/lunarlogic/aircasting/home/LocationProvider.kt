package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.domain.GeoLocation

interface LocationProvider {
  suspend fun current(): GeoLocation?
}