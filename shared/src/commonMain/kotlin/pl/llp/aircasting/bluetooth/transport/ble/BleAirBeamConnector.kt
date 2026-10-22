package pl.llp.aircasting.bluetooth.transport.ble

import co.touchlab.kermit.Logger
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import pl.llp.aircasting.bluetooth.AirBeamConnection
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.ConnectResult
import pl.llp.aircasting.bluetooth.ConnectionStatus
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.SensorReading
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.bluetooth.Transport
import pl.llp.aircasting.bluetooth.detection.airBeamFrom
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse
import pl.llp.aircasting.bluetooth.transport.accumulateDistinct
import pl.llp.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val SCAN_TIMEOUT_DURATION = 10.seconds
private val CONNECT_TIMEOUT_DURATION = 30.seconds
private val STATUS_SETTLE_TIMEOUT = 5.seconds

/** Legacy `SyncableAirBeamConfigurator.MAX_MTU`: one ASCII measurement line per notification. */
private const val V1_MTU = 517
/** Legacy `AirBeamMiniV2Configurator`: fits a 244-byte sync chunk. */
private const val V2_MTU = 247

class BleAirBeamConnector() : AirBeamConnector {
  override val supportedTransports = setOf(Transport.BLE)

  override fun scan() = Scanner().advertisements
    .mapNotNull { advertisement ->
      airBeamFrom(advertisement.name, advertisement.uuids)?.let { airbeamDevice ->
        DiscoveredAirBeam(
          DeviceId(advertisement.identifier.toString()),
          advertisement.name ?: "",
          airbeamDevice
        )
      }
    }
    .accumulateDistinct()

  override suspend fun connect(target: DiscoveredAirBeam): ConnectResult {
    val advertisement = try {
      withTimeoutOrNull(SCAN_TIMEOUT_DURATION) {
        Scanner().advertisements.first { it.identifier.toString() == target.id.value }
      } ?: return ConnectResult.Failed(FailureReason.NoDeviceFound)
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (_: Exception) {
      return ConnectResult.Failed(FailureReason.RadioOrPermissionMissing)
    }

    val mtu = if (target.device is AirBeamDevice.Mini.V2) V2_MTU else V1_MTU
    val peripheral = Peripheral(advertisement) {
      onServicesDiscovered { requestMtuIfSupported(mtu) }
    }
    // Cancelled by Kable the moment this link ends — everything per-link runs in it.
    val linkScope = try {
      withTimeout(CONNECT_TIMEOUT_DURATION) { peripheral.connect() }
    } catch (_: TimeoutCancellationException) {
      peripheral.release()
      return ConnectResult.Failed(FailureReason.LinkTimeout)
    } catch (cancel: CancellationException) {
      peripheral.release()
      throw cancel
    } catch (_: Exception) {
      peripheral.release()
      return ConnectResult.Failed(FailureReason.LinkTimeout)
    }

    return try {
      val readings = peripheral.readings(target.device, linkScope)
      val deviceState = if (target.device is AirBeamDevice.Mini.V2) settleV2(peripheral, linkScope) else null
      ConnectResult.Connected(BleConnection(peripheral, target.device, readings, deviceState))
    } catch (cancel: CancellationException) {
      peripheral.release()
      throw cancel
    } catch (_: Exception) {
      peripheral.release()
      ConnectResult.Failed(FailureReason.HandshakeFailed)
    }
  }

  /** V2 reports its state on connect; it decides what the app does next (new / continue / sync). */
  private suspend fun settleV2(peripheral: Peripheral, linkScope: CoroutineScope): StateFlow<DeviceReportedState>? {
    // Kept subscribed for the life of the link, so a command's reply is never missed.
    peripheral.observe(AirBeamGatt.MiniV2.response).logFailures().launchIn(linkScope)
    val states = peripheral.observe(AirBeamGatt.MiniV2.status).mapNotNull { DeviceReportedState.from(it) }
    // stateIn (suspend overload) subscribes, then suspends until the first frame — that IS the settle.
    val deviceState = withTimeoutOrNull(STATUS_SETTLE_TIMEOUT) { states.stateIn(linkScope) }
    if (deviceState == null && peripheral.state.value !is State.Connected) error("V2 dropped while settling")
    return deviceState
  }
}

/**
 * Subscribes every measurement characteristic of [device] and buffers what arrives until the
 * reader collects it. Closes when the link ends.
 */
private fun Peripheral.readings(device: AirBeamDevice, linkScope: CoroutineScope): Flow<SensorReading> {
  val received = Channel<SensorReading>(Channel.UNLIMITED)
  val line = { bytes: ByteArray ->
    val text = bytes.decodeToString()
    Logger.withTag("BleConnection").v { "V1 line: $text" } // TEMP: verify AB3 names/units, then remove
    listOfNotNull(AirBeamProtocol.Standard.parseReading(text))
  }
  linkScope.coroutineContext.job.invokeOnCompletion { received.close() }
  readingSources(device).forEach { (characteristic, parse) ->
    observe(characteristic)
      .onEach { bytes ->
        line(bytes)
        parse(bytes).forEach { received.trySend(it) }
      }
      .logFailures()
      .launchIn(linkScope)
  }
  return received.receiveAsFlow()
}

private fun readingSources(device: AirBeamDevice): List<Pair<Characteristic, (ByteArray) -> List<SensorReading>>> {
  val line = { bytes: ByteArray -> listOfNotNull(AirBeamProtocol.Standard.parseReading(bytes.decodeToString())) }
  return when (device) {
    // Legacy AirBeam3Configurator.measurementsCharacteristicUUIDs
    AirBeamDevice.AirBeam3 -> with(AirBeamGatt.Standard) { listOf(temperatureF, humidity, PM1, PM2_5, PM10) }
      .map { it to line }
    // Legacy AirBeamMiniConfigurator (its battery characteristic carries no readings)
    AirBeamDevice.Mini.V1 -> with(AirBeamGatt.Standard) { listOf(PM1, PM2_5) }.map { it to line }
    AirBeamDevice.Mini.V2 -> listOf(
      AirBeamGatt.MiniV2.measurement to AirBeamProtocol.MiniV2::parseMeasurement,
      AirBeamGatt.MiniV2.sync to AirBeamProtocol.MiniV2::parseSyncChunk,
    )
    AirBeamDevice.AirBeam2 -> emptyList()
  }
}

private class BleConnection(
  private val peripheral: Peripheral,
  override val device: AirBeamDevice,
  override val readings: Flow<SensorReading>,
  override val deviceState: StateFlow<DeviceReportedState>?,
) : AirBeamConnection {
  private val _status = MutableStateFlow(ConnectionStatus.Ready)
  override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
  private val log = Logger.withTag("BleConnection")

  init {
    // peripheral.scope, not the link scope: Kable cancels the latter on the very drop we watch for.
    peripheral.scope.launch {
      peripheral.state.first { it is State.Disconnected }
      if (_status.compareAndSet(ConnectionStatus.Ready, ConnectionStatus.DisconnectedUnexpectedly)) {
        log.w { "$device disconnected unexpectedly" }
      }
    }
  }

  override suspend fun configure(config: SessionConfig): ConfigResult {
    log.i { "Configuring $device with $config" }
    return guarded {
      when (device) {
        AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1 -> oldSetup(config)
        AirBeamDevice.Mini.V2 -> miniV2Setup(config)
        AirBeamDevice.AirBeam2 -> ConfigResult.UnknownFailure // classic serial, never over BLE
      }
    }
  }

  /**
   * Legacy `reconnectMobileSession()`: V1 devices get mobile mode again; a V2 keeps running its
   * session through the drop, so it gets the time and — if it paused — `ContinueSession`.
   */
  override suspend fun resumeMobileSession(config: SessionConfig.Mobile): ConfigResult = guarded {
    when (device) {
      AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1 -> oldSetup(config)
      AirBeamDevice.Mini.V2 -> {
        sendV2Command(AirBeamProtocol.MiniV2.timeSyncCommand())
        when (deviceState?.value) {
          DeviceReportedState.HasSavedSession ->
            sendV2AndAwait(AirBeamProtocol.MiniV2.continueSessionCommand(), 10.seconds)
          // Running: sync + live data already flow. Idle / unknown: legacy logs and carries on.
          else -> ConfigResult.Success
        }
      }
      AirBeamDevice.AirBeam2 -> ConfigResult.UnknownFailure
    }
  }

  override suspend fun disconnect() {
    _status.compareAndSet(ConnectionStatus.Ready, ConnectionStatus.Disconnected)
    peripheral.release()
  }

  private suspend fun guarded(block: suspend () -> ConfigResult): ConfigResult = try {
    block()
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (e: Exception) {
    log.e(e) { "Configuration failed for $device" }
    ConfigResult.UnknownFailure
  }

  private suspend fun oldSetup(config: SessionConfig): ConfigResult {
    sendCommand(AirBeamProtocol.Standard.setTimeCommand())
    sendCommand(AirBeamProtocol.Standard.setUuidCommand(config.uuid))
    when (config) {
      is SessionConfig.Mobile -> {
        sendCommand(AirBeamProtocol.Standard.startMobileCommand())
      }
      is SessionConfig.FixedWiFi -> {
        sendCommand(AirBeamProtocol.Standard.setAuthCommand(config.authToken))
        sendCommand(AirBeamProtocol.Standard.setLocationCommand(config.latitude, config.longitude))
        sendCommand(AirBeamProtocol.Standard.startFixedCommand(config.ssid, config.password, config.zoneOffset))
      }
      is SessionConfig.FixedCellular -> {
        sendCommand(AirBeamProtocol.Standard.setAuthCommand(config.authToken))
        sendCommand(AirBeamProtocol.Standard.setLocationCommand(config.latitude, config.longitude))
        sendCommand(AirBeamProtocol.Standard.startFixedCellCommand())
      }
    }
    return ConfigResult.Success
  }

  private suspend fun miniV2Setup(config: SessionConfig): ConfigResult {
    // 1. Time Sync (Prerequisite - 0x15 + 8-byte LE timestamp)
    sendV2Command(AirBeamProtocol.MiniV2.timeSyncCommand())

    // 2. Configure Session
    return when (config) {
      is SessionConfig.Mobile ->
        sendV2AndAwait(AirBeamProtocol.MiniV2.mobileSessionConfigCommand(config.uuid), 10.seconds)

      // Device attempts to connect to WiFi: WiFi OK -> session starts; WiFi Failed -> Nack(0x05 InvalidWifiCredentials)
      is SessionConfig.FixedWiFi -> sendV2AndAwait(
        AirBeamProtocol.MiniV2.fixedWifiSessionConfigCommand(
          uuid = config.uuid,
          authToken = config.authToken,
          ssid = config.ssid,
          password = config.password,
          pm1Index = config.pm1Index,
          pm25Index = config.pm25Index,
        ),
        15.seconds,
      )

      is SessionConfig.FixedCellular -> {
        log.e { "Mini V2 does not support FixedCellular configuration" }
        ConfigResult.BadConfigFailure
      }
    }
  }

  /**
   * Sends [command] and waits for the device's verdict. Listening starts before the write, so a
   * fast reply can't slip past. No verdict within [timeout] counts as success, as before.
   */
  private suspend fun sendV2AndAwait(command: ByteArray, timeout: Duration): ConfigResult = coroutineScope {
    val verdict = async(start = CoroutineStart.UNDISPATCHED) {
      withTimeoutOrNull(timeout) {
        peripheral.observe(AirBeamGatt.MiniV2.response)
          .mapNotNull { CommandResponse.parse(it) }
          .first { it !is CommandResponse.Ack }
      }
    }
    sendV2Command(command)
    when (val response = verdict.await()) {
      is CommandResponse.Nack -> {
        log.e { "Mini V2 rejected command 0x${command[0].toString(16)}: ${response.errorCode}" }
        mapNackToConfigResult(response.errorCode)
      }
      else -> ConfigResult.Success
    }
  }

  private fun mapNackToConfigResult(errorCode: AirBeamProtocol.MiniV2.NackErrorCode): ConfigResult {
    return when (errorCode) {
      AirBeamProtocol.MiniV2.NackErrorCode.InvalidConfig -> ConfigResult.BadConfigFailure
      AirBeamProtocol.MiniV2.NackErrorCode.InvalidWifiCredentials -> ConfigResult.WifiPassFailure
      AirBeamProtocol.MiniV2.NackErrorCode.StorageHasMeasurements -> ConfigResult.NotSyncedFailure
      else -> ConfigResult.UnknownFailure
    }
  }

  private suspend fun sendV2Command(command: ByteArray) {
    peripheral.write(AirBeamGatt.MiniV2.command, command, WriteType.WithResponse)
    delay(100.milliseconds)
  }

  private suspend fun sendCommand(command: ByteArray) {
    peripheral.write(AirBeamGatt.Standard.config, command, WriteType.WithResponse)
    delay(500.milliseconds) //give time to AirBeam to process the command as in the old app
  }
}

/** A failed observation (missing characteristic, link dropping) ends that one subscription, not the app. */
private fun <T> Flow<T>.logFailures() = catch { Logger.withTag("BleConnection").w(it) { "Observation ended" } }

/** Disconnects and disposes: a Kable [Peripheral] holds its own scope until [Peripheral.close]. */
private suspend fun Peripheral.release() {
  try {
    disconnect()
  } catch (_: Exception) {
  }
  close()
}


object AirBeamGatt {
  // AB3 and Mini V1 share the same protocol/characteristics
  object Standard {
    private const val BASE = "-0000-1000-8000-00805f9b34fb"
    val service = Uuid.parse("0000ffdd$BASE")
    val config = characteristicOf(service, Uuid.parse("0000ffde$BASE"))
    val sdSync = characteristicOf(service, Uuid.parse("0000ffdf$BASE"))
    val temperatureC = characteristicOf(service, Uuid.parse("0000ffe0$BASE"))
    val temperatureF = characteristicOf(service, Uuid.parse("0000ffe1$BASE"))
    val humidity = characteristicOf(service, Uuid.parse("0000ffe3$BASE"))
    val PM1 = characteristicOf(service, Uuid.parse("0000ffe4$BASE"))
    val PM2_5 = characteristicOf(service, Uuid.parse("0000ffe5$BASE"))
    val PM10 = characteristicOf(service, Uuid.parse("0000ffe6$BASE"))
  }

  object MiniV2 {
    private const val BASE = "-4b3c-8e9a-1f2d3c4b5a60"
    val service = Uuid.parse("a0e1f000-0001$BASE")
    val status = characteristicOf(service, Uuid.parse("a0e1f000-0002$BASE"))
    val command = characteristicOf(service, Uuid.parse("a0e1f000-0003$BASE"))
    val response = characteristicOf(service, Uuid.parse("a0e1f000-0004$BASE"))
    val measurement = characteristicOf(service, Uuid.parse("a0e1f000-0005$BASE"))
    val sync = characteristicOf(service, Uuid.parse("a0e1f000-0006$BASE"))
  }
}
