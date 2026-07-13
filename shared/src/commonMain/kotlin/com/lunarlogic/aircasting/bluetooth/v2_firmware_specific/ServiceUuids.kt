package com.lunarlogic.aircasting.bluetooth.v2_firmware_specific

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
val MINI_V2_SERVICE = Uuid.parse("a0e1f000-0001-4b3c-8e9a-1f2d3c4b5a60")  // v2_firmware_specific pkg
// ffdd (AB3 + Mini V1 shared GATT service) is post-connect, NOT advertised → not needed for scan