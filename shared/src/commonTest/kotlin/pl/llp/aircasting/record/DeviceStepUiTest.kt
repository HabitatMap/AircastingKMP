package pl.llp.aircasting.record

import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.scan.ScanUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceStepUiTest {
  private val ab3 = DiscoveredAirBeam(DeviceId("id-ab3"), "AirBeam3", AirBeamDevice.AirBeam3)

  @Test
  fun `a missing permission wins over everything else`() {
    assertEquals(
      DeviceStepUi.PermissionNeeded,
      deviceStepUi(permissionGranted = false, bluetoothOff = true, scan = ScanUiState.Scanning(listOf(ab3))),
    )
  }

  @Test
  fun `bluetooth off is shown once permission is granted`() {
    assertEquals(
      DeviceStepUi.BluetoothOff,
      deviceStepUi(permissionGranted = true, bluetoothOff = true, scan = ScanUiState.Idle),
    )
  }

  @Test
  fun `before the first result the list counts as scanning so the empty state never flashes`() {
    val ui = deviceStepUi(permissionGranted = true, bluetoothOff = false, scan = ScanUiState.Idle)

    assertEquals(DeviceStepUi.Devices(emptyList(), scanning = true), ui)
  }

  @Test
  fun `devices found mid-scan are listed while the scan continues`() {
    val ui = deviceStepUi(true, false, ScanUiState.Scanning(listOf(ab3)))

    assertEquals(DeviceStepUi.Devices(listOf(ab3), scanning = true), ui)
  }

  @Test
  fun `a finished scan with no devices is the nothing-found state`() {
    val ui = deviceStepUi(true, false, ScanUiState.Finished(emptyList()))

    assertTrue((ui as DeviceStepUi.Devices).nothingFound)
  }

  @Test
  fun `a failed scan also lands on nothing-found where Try again can recover it`() {
    val ui = deviceStepUi(true, false, ScanUiState.Error(FailureReason.RadioOrPermissionMissing))

    assertTrue((ui as DeviceStepUi.Devices).nothingFound)
  }
}