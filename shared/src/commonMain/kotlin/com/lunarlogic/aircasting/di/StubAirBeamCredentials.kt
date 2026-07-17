package com.lunarlogic.aircasting.di

import com.lunarlogic.aircasting.bluetooth.AirBeamCredentials

object StubAirBeamCredentials : AirBeamCredentials {
  override suspend fun sessionUuid() = "00000000-0000-0000-0000-000000000000"
  override suspend fun authToken() = "stub-token"
}