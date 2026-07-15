package com.lunarlogic.aircasting.di

import com.lunarlogic.aircasting.bluetooth.transport.classic.AirBeam2Credentials

object StubAirBeam2Credentials : AirBeam2Credentials {
  override suspend fun sessionUuid() = "00000000-0000-0000-0000-000000000000"
  override suspend fun authToken() = "stub-token"
}