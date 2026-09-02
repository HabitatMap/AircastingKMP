package pl.llp.aircasting.bluetooth.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.uuid.Uuid

class AirBeamProtocolTest {

  // --- AirBeam 3 (Standard Protocol) Tests ---

  @Test
  fun ab3_mobile_command_returns_0xFE_0x01_0xFF() {
    val expected = byteArrayOf(0xFE.toByte(), 0x01, 0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.Standard.startMobileCommand())
  }

  @Test
  fun ab3_wifi_fixed_command_returns_0xFE_0x02_payload_0xFF() {
    val expected = byteArrayOf(0xFE.toByte(), 0x02) +
      "MySSID,MyPass,2".encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.Standard.startFixedCommand("MySSID", "MyPass", 2))
  }

  @Test
  fun ab3_cellular_fixed_command_returns_0xFE_0x03_0xFF() {
    val expected = byteArrayOf(0xFE.toByte(), 0x03, 0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.Standard.startFixedCellCommand())
  }

  @Test
  fun ab3_uuid_command_returns_0xFE_0x04_uuid_0xFF() {
    val uuidString = "12345678-1234-1234-1234-123456789abc"
    val uuid = Uuid.parse(uuidString)
    val expected = byteArrayOf(0xFE.toByte(), 0x04) +
      uuidString.encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.Standard.setUuidCommand(uuid))
    assertContentEquals(expected, AirBeamProtocol.Standard.setUuidCommand(uuidString))
  }

  @Test
  fun ab3_auth_command_returns_0xFE_0x05_base64_credentials_0xFF() {
    val expected = byteArrayOf(0xFE.toByte(), 0x05) +
      "YWJjOlg=".encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.Standard.setAuthCommand("abc"))
  }

  @Test
  fun ab3_location_command_returns_latitude_first() {
    val expectedPayload = "52.229700,21.012200"
    val expected = byteArrayOf(0xFE.toByte(), 0x06) +
      expectedPayload.encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.Standard.setLocationCommand(52.2297, 21.0122))
  }

  // --- AirBeam Mini V2 Protocol Tests ---

  @Test
  fun miniV2_time_sync_command_returns_0x15_and_8byte_LE_timestamp() {
    val timestamp = 1700000000L
    val command = AirBeamProtocol.MiniV2.timeSyncCommand(timestamp)
    assertEquals(9, command.size)
    assertEquals(0x15.toByte(), command[0])
    // Verify LE encoding of timestamp
    val expectedTimestampBytes = byteArrayOf(
      (timestamp and 0xFF).toByte(),
      ((timestamp shr 8) and 0xFF).toByte(),
      ((timestamp shr 16) and 0xFF).toByte(),
      ((timestamp shr 24) and 0xFF).toByte(),
      ((timestamp shr 32) and 0xFF).toByte(),
      ((timestamp shr 40) and 0xFF).toByte(),
      ((timestamp shr 48) and 0xFF).toByte(),
      ((timestamp shr 56) and 0xFF).toByte()
    )
    assertContentEquals(expectedTimestampBytes, command.copyOfRange(1, 9))
  }

  @Test
  fun miniV2_mobile_session_config_returns_20_bytes() {
    val uuid = Uuid.parse("12345678-1234-1234-1234-123456789abc")
    val command = AirBeamProtocol.MiniV2.mobileSessionConfigCommand(uuid, intervalSeconds = 1)
    assertEquals(20, command.size)
    assertEquals(0x13.toByte(), command[0])
    assertEquals(0x01.toByte(), command[19]) // SessionType::MOBILE
    // Interval 1 -> u16 LE -> [0x01, 0x00] at indices 17, 18
    assertEquals(0x01.toByte(), command[17])
    assertEquals(0x00.toByte(), command[18])
  }

  @Test
  fun miniV2_fixed_wifi_session_config_returns_134_bytes() {
    val uuid = Uuid.parse("12345678-1234-1234-1234-123456789abc")
    val command = AirBeamProtocol.MiniV2.fixedWifiSessionConfigCommand(
      uuid = uuid,
      intervalSeconds = 60,
      pm1Index = 0,
      pm25Index = 1,
      authToken = "my_token",
      ssid = "MySSID",
      password = "MyPassword"
    )
    assertEquals(134, command.size)
    assertEquals(0x13.toByte(), command[0])
    assertEquals(0x00.toByte(), command[19]) // SessionType::FIXED
    assertEquals(0.toByte(), command[20])   // pm1Index
    assertEquals(1.toByte(), command[21])   // pm25Index
    // Interval 60 -> u16 LE -> [0x3C, 0x00]
    assertEquals(0x3C.toByte(), command[17])
    assertEquals(0x00.toByte(), command[18])
  }

  @Test
  fun miniV2_control_commands_return_expected_single_byte() {
    assertContentEquals(byteArrayOf(0x10.toByte()), AirBeamProtocol.MiniV2.continueSessionCommand())
    assertContentEquals(byteArrayOf(0x11.toByte()), AirBeamProtocol.MiniV2.discardSessionCommand())
    assertContentEquals(byteArrayOf(0x12.toByte()), AirBeamProtocol.MiniV2.startWifiSyncCommand())
    assertContentEquals(byteArrayOf(0x14.toByte()), AirBeamProtocol.MiniV2.getSensorsCommand())
    assertContentEquals(byteArrayOf(0x16.toByte()), AirBeamProtocol.MiniV2.startBleSyncCommand())
  }

  @Test
  fun miniV2_parses_command_responses() {
    assertEquals(
      AirBeamProtocol.MiniV2.CommandResponse.Ack,
      AirBeamProtocol.MiniV2.CommandResponse.parse(byteArrayOf(0x20.toByte()))
    )
    assertEquals(
      AirBeamProtocol.MiniV2.CommandResponse.Ready,
      AirBeamProtocol.MiniV2.CommandResponse.parse(byteArrayOf(0x22.toByte()))
    )
    assertEquals(
      AirBeamProtocol.MiniV2.CommandResponse.SensorInfo("PM1,μg/m3;PM2.5,μg/m3"),
      AirBeamProtocol.MiniV2.CommandResponse.parse(byteArrayOf(0x23.toByte()) + "PM1,μg/m3;PM2.5,μg/m3".encodeToByteArray())
    )
    assertEquals(
      AirBeamProtocol.MiniV2.CommandResponse.Nack(AirBeamProtocol.MiniV2.NackErrorCode.InvalidWifiCredentials),
      AirBeamProtocol.MiniV2.CommandResponse.parse(byteArrayOf(0x21.toByte(), 0x05.toByte()))
    )
  }

  @Test
  fun miniV2_parses_status_notifications() {
    assertEquals(
      AirBeamProtocol.MiniV2.StatusNotification.Idle(batteryLevel = 85.toByte()),
      AirBeamProtocol.MiniV2.StatusNotification.parse(byteArrayOf(0x00.toByte(), 85.toByte()))
    )
  }

  // --- AirBeam 2 Protocol Tests ---

  @Test
  fun ab2_mobile_command_returns_0x01_0xFF() {
    val expected = byteArrayOf(0x01.toByte(), 0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.AirBeam2.startMobileCommand())
  }

  @Test
  fun ab2_uuid_command_returns_0x04_uuid_0xFF() {
    val uuidString = "12345678-1234-1234-1234-123456789abc"
    val uuid = Uuid.parse(uuidString)
    val expected = byteArrayOf(0x04.toByte()) +
      uuidString.encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.AirBeam2.setUuidCommand(uuid))
    assertContentEquals(expected, AirBeamProtocol.AirBeam2.setUuidCommand(uuidString))
  }

  @Test
  fun ab2_auth_command_returns_0x05_token_0xFF() {
    val expected = byteArrayOf(0x05.toByte()) +
      "my_auth_token".encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.AirBeam2.setAuthCommand("my_auth_token"))
  }

  @Test
  fun ab2_location_command_returns_0x06_longitude_latitude_0xFF() {
    val expectedPayload = "21.012200,52.229700"
    val expected = byteArrayOf(0x06.toByte()) +
      expectedPayload.encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.AirBeam2.setLocationCommand(52.2297, 21.0122))
  }

  @Test
  fun ab2_wifi_fixed_command_returns_0x02_ssid_pass_offset_0xFF() {
    val expected = byteArrayOf(0x02.toByte()) +
      "MySSID,MyPass,2".encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.AirBeam2.startFixedWifiCommand("MySSID", "MyPass", 2))
  }

  @Test
  fun ab2_cellular_fixed_command_returns_0x03_0xFF() {
    val expected = byteArrayOf(0x03.toByte(), 0xFF.toByte())
    assertContentEquals(expected, AirBeamProtocol.AirBeam2.startFixedCellCommand())
  }
}
