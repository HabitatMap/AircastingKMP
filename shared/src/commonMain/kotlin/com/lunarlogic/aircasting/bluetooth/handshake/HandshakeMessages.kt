package com.lunarlogic.aircasting.bluetooth.handshake

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val BEGIN = 0xFE.toByte()
private const val END = 0xFF.toByte()
private const val UUID_CODE= 0x04.toByte()
private const val AUTH_TOKEN_CODE= 0x05.toByte()

object HandshakeMessages {

  fun uuidMessage(sessionUuid: String): ByteArray = frame(UUID_CODE, sessionUuid)

  @OptIn(ExperimentalEncodingApi::class)
  fun authTokenMessage(authToken: String): ByteArray =
    frame(AUTH_TOKEN_CODE, Base64.encode("$authToken:X".encodeToByteArray()))

  private fun frame(code: Byte, payload: String): ByteArray =
    byteArrayOf(BEGIN, code) + payload.encodeToByteArray() + byteArrayOf(END)
}
