package pl.llp.aircasting.bluetooth.protocol

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.uuid.Uuid

sealed class AirBeamProtocol {
  object Standard {
    private const val BEGIN = 0xFE.toByte()
    private const val END = 0xFF.toByte()
    private const val START_MOBILE = 0x01.toByte()
    private const val START_FIXED_WIFI = 0x02.toByte()
    private const val START_FIXED_CELL = 0x03.toByte()
    private const val SET_UUID = 0x04.toByte()
    private const val SET_AUTH = 0x05.toByte()
    private const val SET_LOCATION = 0x06.toByte()
    private const val SET_TIME = 0x08.toByte()
    private const val START_SYNC = 0x09.toByte()
    private const val DELETE_MEMORY = 0x0A.toByte()

    fun setTimeCommand(): ByteArray {
      val dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
      // Format: dd/MM/yy-HH:mm:ss
      val year = (dateTime.year % 100).toString().padStart(2, '0')
      val month = dateTime.month.number.toString().padStart(2, '0')
      val day = dateTime.day.toString().padStart(2, '0')
      val hour = dateTime.hour.toString().padStart(2, '0')
      val minute = dateTime.minute.toString().padStart(2, '0')
      val second = dateTime.second.toString().padStart(2, '0')
      val timeString = "$day/$month/$year-$hour:$minute:$second"

      return byteArrayOf(BEGIN, SET_TIME) + timeString.encodeToByteArray() + byteArrayOf(END)
    }

    fun setUuidCommand(uuid: Uuid): ByteArray =
      byteArrayOf(BEGIN, SET_UUID) + uuid.toString().encodeToByteArray() + byteArrayOf(END)

    fun setUuidCommand(uuidString: String): ByteArray =
      byteArrayOf(BEGIN, SET_UUID) + uuidString.encodeToByteArray() + byteArrayOf(END)

    @OptIn(ExperimentalEncodingApi::class)
    fun setAuthCommand(token: String): ByteArray =
      byteArrayOf(BEGIN, SET_AUTH) + Base64.encode("$token:X".encodeToByteArray()).encodeToByteArray() + byteArrayOf(END)

    fun setLocationCommand(lat: Double, long: Double): ByteArray =
      byteArrayOf(BEGIN, SET_LOCATION) +
        "${lat.toAirbeamPayload()},${long.toAirbeamPayload()}".encodeToByteArray() +
        byteArrayOf(END)

    fun startFixedCommand(ssid: String, pass: String, zoneOffset: Int): ByteArray {
      val payload = "$ssid,$pass,$zoneOffset".encodeToByteArray()
      return byteArrayOf(BEGIN, START_FIXED_WIFI) + payload + byteArrayOf(END)
    }

    fun startMobileCommand(): ByteArray = byteArrayOf(BEGIN, START_MOBILE, END)
    fun startFixedCellCommand(): ByteArray = byteArrayOf(BEGIN, START_FIXED_CELL, END)
    fun startSyncCommand(): ByteArray = byteArrayOf(BEGIN, START_SYNC, END)
    fun clearMemoryCommand(): ByteArray = byteArrayOf(BEGIN, DELETE_MEMORY, END)
  }

  /**
   * AirBeam 2 protocol.
   * Frame format: command_byte + payload + [0xFF] (no 0xFE start delimiter).
   */
  object AirBeam2 {
    private const val END = 0xFF.toByte()
    private const val START_MOBILE = 0x01.toByte()
    private const val START_FIXED_WIFI = 0x02.toByte()
    private const val START_FIXED_CELL = 0x03.toByte()
    private const val SET_UUID = 0x04.toByte()
    private const val SET_AUTH = 0x05.toByte()
    private const val SET_LOCATION = 0x06.toByte()

    /** Mobile Session (Bluetooth): [0x01, 0xFF] */
    fun startMobileCommand(): ByteArray = byteArrayOf(START_MOBILE, END)

    /** Session UUID: [0x04] + "<UUID>" + [0xFF] */
    fun setUuidCommand(uuid: Uuid): ByteArray =
      byteArrayOf(SET_UUID) + uuid.toString().encodeToByteArray() + byteArrayOf(END)

    fun setUuidCommand(uuidString: String): ByteArray =
      byteArrayOf(SET_UUID) + uuidString.encodeToByteArray() + byteArrayOf(END)

    /** Authentication Token: [0x05] + "<UUID_AUTH>" + [0xFF] */
    fun setAuthCommand(authToken: String): ByteArray =
      byteArrayOf(SET_AUTH) + authToken.encodeToByteArray() + byteArrayOf(END)

    /** Coordinates: [0x06] + "<LONGITUDE>,<LATITUDE>" + [0xFF] */
    fun setLocationCommand(lat: Double, long: Double): ByteArray =
      byteArrayOf(SET_LOCATION) +
        "${long.toAirbeamPayload()},${lat.toAirbeamPayload()}".encodeToByteArray() +
        byteArrayOf(END)

    /** WiFi credentials: [0x02] + "<SSID>,<PASSWORD>,<ZONE_OFFSET>" + [0xFF] */
    fun startFixedWifiCommand(ssid: String, pass: String, zoneOffset: Int): ByteArray =
      byteArrayOf(START_FIXED_WIFI) +
        "$ssid,$pass,$zoneOffset".encodeToByteArray() +
        byteArrayOf(END)

    /** Cellular mode: [0x03, 0xFF] */
    fun startFixedCellCommand(): ByteArray = byteArrayOf(START_FIXED_CELL, END)
  }
}

private fun Double.toAirbeamPayload(): String {
  // airbeam expects <1-2digits><dot><6digits>
  val factor = 10.0.pow(6)
  val rounded = (abs(this) * factor).roundToLong()
  val integerPart = rounded / factor.toLong()
  val fractionPart = (rounded % factor.toLong()).toString().padStart(6, '0')
  val sign = if (this < 0) "-" else ""
  return "$sign$integerPart.$fractionPart"
}
