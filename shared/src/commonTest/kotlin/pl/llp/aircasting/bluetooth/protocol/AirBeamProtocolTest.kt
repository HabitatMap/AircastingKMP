package pl.llp.aircasting.bluetooth.protocol

import kotlin.test.Test
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
