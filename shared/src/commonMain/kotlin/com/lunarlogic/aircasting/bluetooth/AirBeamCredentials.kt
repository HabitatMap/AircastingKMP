package com.lunarlogic.aircasting.bluetooth

interface AirBeamCredentials {
  suspend fun sessionUuid(): String // identity, written first  (0x04)
  suspend fun authToken(): String   // credential, written second (0x05)
}