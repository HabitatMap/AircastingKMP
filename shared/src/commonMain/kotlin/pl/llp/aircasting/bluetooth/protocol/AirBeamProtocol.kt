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
  /**
   * AirBeam 3 & AirBeam Mini V1 protocol.
   * Frame format: [0xFE] + command_byte + payload + [0xFF]
   */
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
   * AirBeam Mini V2 protocol.
   * BLE GATT Service: a0e1f000-0001-4b3c-8e9a-1f2d3c4b5a60
   * Status Characteristic: a0e1f000-0002-4b3c-8e9a-1f2d3c4b5a60 (Notifies)
   * Command Characteristic: a0e1f000-0003-4b3c-8e9a-1f2d3c4b5a60 (Write)
   * Response Characteristic: a0e1f000-0004-4b3c-8e9a-1f2d3c4b5a60 (Notifies)
   * Measurement Characteristic: a0e1f000-0005-4b3c-8e9a-1f2d3c4b5a60 (Notifies)
   * Sync Characteristic: a0e1f000-0006-4b3c-8e9a-1f2d3c4b5a60 (Notifies)
   * Format: Raw binary in Little-Endian byte order.
   */
  object MiniV2 {
    val SERVICE_UUID: Uuid = Uuid.parse("a0e1f000-0001-4b3c-8e9a-1f2d3c4b5a60")
    val STATUS_CHARACTERISTIC_UUID: Uuid = Uuid.parse("a0e1f000-0002-4b3c-8e9a-1f2d3c4b5a60")
    val COMMAND_CHARACTERISTIC_UUID: Uuid = Uuid.parse("a0e1f000-0003-4b3c-8e9a-1f2d3c4b5a60")
    val RESPONSE_CHARACTERISTIC_UUID: Uuid = Uuid.parse("a0e1f000-0004-4b3c-8e9a-1f2d3c4b5a60")
    val MEASUREMENT_CHARACTERISTIC_UUID: Uuid = Uuid.parse("a0e1f000-0005-4b3c-8e9a-1f2d3c4b5a60")
    val SYNC_CHARACTERISTIC_UUID: Uuid = Uuid.parse("a0e1f000-0006-4b3c-8e9a-1f2d3c4b5a60")

    private const val CMD_CONTINUE_SESSION = 0x10.toByte()
    private const val CMD_DISCARD_SESSION = 0x11.toByte()
    private const val CMD_START_WIFI_SYNC = 0x12.toByte()
    private const val CMD_NEW_SESSION_CONFIG = 0x13.toByte()
    private const val CMD_GET_SENSORS = 0x14.toByte()
    private const val CMD_TIME_SYNC = 0x15.toByte()
    private const val CMD_START_BLE_SYNC = 0x16.toByte()

    private const val SESSION_TYPE_FIXED = 0x00.toByte()
    private const val SESSION_TYPE_MOBILE = 0x01.toByte()

    sealed interface NackErrorCode {
      data object NoSession : NackErrorCode              // 0x01
      data object InvalidConfig : NackErrorCode          // 0x02
      data object StorageHasMeasurements : NackErrorCode // 0x03
      data object ClearStorageFailed : NackErrorCode     // 0x04
      data object InvalidWifiCredentials : NackErrorCode // 0x05
      data object SyncFailed : NackErrorCode             // 0x06
      data class Unknown(val code: Byte) : NackErrorCode

      companion object {
        fun from(code: Byte): NackErrorCode = when (code) {
          0x01.toByte() -> NoSession
          0x02.toByte() -> InvalidConfig
          0x03.toByte() -> StorageHasMeasurements
          0x04.toByte() -> ClearStorageFailed
          0x05.toByte() -> InvalidWifiCredentials
          0x06.toByte() -> SyncFailed
          else -> Unknown(code)
        }
      }
    }

    sealed interface CommandResponse {
      data object Ack : CommandResponse                                 // 0x20
      data object Ready : CommandResponse                               // 0x22
      data class SensorInfo(val info: String) : CommandResponse         // 0x23
      data class Nack(val errorCode: NackErrorCode) : CommandResponse   // 0x21

      companion object {
        fun parse(bytes: ByteArray): CommandResponse? {
          if (bytes.isEmpty()) return null
          return when (bytes[0]) {
            0x20.toByte() -> Ack
            0x22.toByte() -> Ready
            0x23.toByte() -> SensorInfo(bytes.decodeToString(startIndex = 1))
            0x21.toByte() -> {
              val code = if (bytes.size > 1) bytes[1] else 0x00.toByte()
              Nack(NackErrorCode.from(code))
            }
            else -> null
          }
        }
      }
    }

    sealed interface StatusNotification {
      data class Idle(val batteryLevel: Byte) : StatusNotification
      data class HasSavedSession(
        val battery: Byte,
        val sessionUuid: ByteArray,
        val hasMeasurements: Boolean,
        val fileSize: Long,
      ) : StatusNotification {
        override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (other !is HasSavedSession) return false
          return battery == other.battery &&
            sessionUuid.contentEquals(other.sessionUuid) &&
            hasMeasurements == other.hasMeasurements &&
            fileSize == other.fileSize
        }
        override fun hashCode(): Int {
          var result = battery.toInt()
          result = 31 * result + sessionUuid.contentHashCode()
          result = 31 * result + hasMeasurements.hashCode()
          result = 31 * result + fileSize.hashCode()
          return result
        }
      }
      data class Running(
        val battery: Byte,
        val sessionUuid: ByteArray,
      ) : StatusNotification {
        override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (other !is Running) return false
          return battery == other.battery && sessionUuid.contentEquals(other.sessionUuid)
        }
        override fun hashCode(): Int {
          var result = battery.toInt()
          result = 31 * result + sessionUuid.contentHashCode()
          return result
        }
      }
      data class ReadyToSync(
        val fileSize: Long,
        val password: String,
      ) : StatusNotification

      companion object {
        fun parse(bytes: ByteArray): StatusNotification? {
          if (bytes.isEmpty()) return null
          return when (bytes[0]) {
            0x00.toByte() -> {
              val battery = if (bytes.size > 1) bytes[1] else 0.toByte()
              Idle(battery)
            }
            0x01.toByte() -> {
              if (bytes.size < 27) return null
              val battery = bytes[1]
              val uuid = bytes.copyOfRange(2, 18)
              val hasMeasurements = bytes[18] != 0.toByte()
              val fileSize = bytes.toLittleEndianLong(19)
              HasSavedSession(battery, uuid, hasMeasurements, fileSize)
            }
            0x02.toByte() -> {
              if (bytes.size < 18) return null
              val battery = bytes[1]
              val uuid = bytes.copyOfRange(2, 18)
              Running(battery, uuid)
            }
            0x03.toByte() -> {
              if (bytes.size < 9) return null
              val fileSize = bytes.toLittleEndianLong(1)
              val password = if (bytes.size > 9) bytes.decodeToString(startIndex = 9) else ""
              ReadyToSync(fileSize, password)
            }
            else -> null
          }
        }
      }
    }

    /**
     * Time Sync (0x15) - Prerequisite.
     * Command 0x15 + 8-byte epoch timestamp (i64 LE) — 9 bytes total.
     */
    fun timeSyncCommand(timestampSeconds: Long = Clock.System.now().epochSeconds): ByteArray {
      return byteArrayOf(CMD_TIME_SYNC) + timestampSeconds.toLittleEndianBytes()
    }

    /**
     * Mobile Session: Command 0x13 (NewSessionConfig) — 20 bytes total.
     * [0] = 0x13
     * [1..17] = 16-byte Session UUID (Uuid LE)
     * [17..19] = 2-byte Interval in seconds (u16 LE)
     * [19] = 0x01 (SessionType::MOBILE)
     */
    fun mobileSessionConfigCommand(
      uuid: Uuid,
      intervalSeconds: Int = 1,
    ): ByteArray {
      val buffer = ByteArray(20)
      buffer[0] = CMD_NEW_SESSION_CONFIG
      uuid.to16BytesLittleEndian().copyInto(buffer, destinationOffset = 1)
      intervalSeconds.toU16LittleEndianBytes().copyInto(buffer, destinationOffset = 17)
      buffer[19] = SESSION_TYPE_MOBILE
      return buffer
    }

    /**
     * Fixed Session (WiFi): Command 0x13 (NewSessionConfig) — 134 bytes total.
     * [0] = 0x13
     * [1..17] = 16-byte Session UUID (Uuid LE)
     * [17..19] = 2-byte Interval in seconds (u16 LE)
     * [19] = 0x00 (SessionType::FIXED)
     * [20] = pm1_index (u8)
     * [21] = pm2_5_index (u8)
     * [22..38] = 16-byte Auth Token (u128 LE / raw 16 bytes decoded from 32-char hex)
     * [38..70] = 32-byte WiFi SSID (UTF-8, null-padded)
     * [70..134] = 64-byte WiFi Password (UTF-8, null-padded)
     */
    fun fixedWifiSessionConfigCommand(
      uuid: Uuid,
      intervalSeconds: Int = 60,
      pm1Index: Int = 0,
      pm25Index: Int = 1,
      authToken: String,
      ssid: String,
      password: String,
    ): ByteArray {
      val buffer = ByteArray(134)
      buffer[0] = CMD_NEW_SESSION_CONFIG
      uuid.to16BytesLittleEndian().copyInto(buffer, destinationOffset = 1)
      intervalSeconds.toU16LittleEndianBytes().copyInto(buffer, destinationOffset = 17)
      buffer[19] = SESSION_TYPE_FIXED
      buffer[20] = pm1Index.toByte()
      buffer[21] = pm25Index.toByte()

      val tokenBytes = parseAuthTokenTo16Bytes(authToken)
      tokenBytes.copyInto(buffer, destinationOffset = 22)

      val ssidBytes = ssid.encodeToByteArray()
      val ssidLen = minOf(ssidBytes.size, 32)
      ssidBytes.copyInto(buffer, destinationOffset = 38, startIndex = 0, endIndex = ssidLen)

      val passBytes = password.encodeToByteArray()
      val passLen = minOf(passBytes.size, 64)
      passBytes.copyInto(buffer, destinationOffset = 70, startIndex = 0, endIndex = passLen)

      return buffer
    }

    /** 0x10: ContinueSession */
    fun continueSessionCommand(): ByteArray = byteArrayOf(CMD_CONTINUE_SESSION)

    /** 0x11: DiscardSession (stops session without syncing) */
    fun discardSessionCommand(): ByteArray = byteArrayOf(CMD_DISCARD_SESSION)

    /** 0x12: StartWiFiSync (ends session and triggers upload over WiFi) */
    fun startWifiSyncCommand(): ByteArray = byteArrayOf(CMD_START_WIFI_SYNC)

    /** 0x14: GetSensors (by default returns "PM1,μg/m3;PM2.5,μg/m3") */
    fun getSensorsCommand(): ByteArray = byteArrayOf(CMD_GET_SENSORS)

    /** 0x16: StartBleSync (transfers stored measurements over sync characteristic) */
    fun startBleSyncCommand(): ByteArray = byteArrayOf(CMD_START_BLE_SYNC)
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

private fun Long.toLittleEndianBytes(): ByteArray =
  ByteArray(8) { i -> ((this shr (i * 8)) and 0xFF).toByte() }

private fun Int.toU16LittleEndianBytes(): ByteArray =
  byteArrayOf(this.toByte(), (this shr 8).toByte())

private fun ByteArray.toLittleEndianLong(offset: Int): Long {
  var result = 0L
  for (i in 0 until 8) {
    if (offset + i < size) {
      result = result or ((this[offset + i].toLong() and 0xFFL) shl (i * 8))
    }
  }
  return result
}

private fun uuidStringTo16BytesLittleEndian(uuidStr: String): ByteArray {
  val hex = uuidStr.replace("-", "")
  require(hex.length == 32) { "Invalid UUID string length: $uuidStr" }
  val bytes = ByteArray(16) { i ->
    hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
  }
  bytes.reverse()
  return bytes
}

private fun Uuid.to16BytesLittleEndian(): ByteArray =
  uuidStringTo16BytesLittleEndian(this.toString())

private fun parseAuthTokenTo16Bytes(tokenHex: String): ByteArray {
  val hex = tokenHex.replace("-", "")
  if (hex.length == 32) {
    return ByteArray(16) { i ->
      hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
  }
  val bytes = tokenHex.encodeToByteArray()
  val result = ByteArray(16)
  val len = minOf(bytes.size, 16)
  bytes.copyInto(result, destinationOffset = 0, startIndex = 0, endIndex = len)
  return result
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
