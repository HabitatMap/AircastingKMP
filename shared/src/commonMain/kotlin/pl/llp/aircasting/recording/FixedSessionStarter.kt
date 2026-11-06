package pl.llp.aircasting.recording

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.domain.GeoLocation
import kotlin.uuid.Uuid

/** The network a fixed AirBeam uploads over. Ends up in logs, so toString() leaves the password out. */
data class WifiCredentials(val ssid: String, val password: String) {
  override fun toString() = "WifiCredentials(ssid=$ssid)"
}

/** How a fixed AirBeam reaches the backend. */
sealed interface Uplink {
  data class Wifi(val credentials: WifiCredentials) : Uplink
  /** AB2 and AB3 only (DOMAIN_KNOWLEDGE.md "Cellular"). */
  data object Cellular : Uplink
}

sealed interface FixedStartResult {
  /** The AirBeam uploads on its own; the link is dropped. */
  data object Started : FixedStartResult
  data object SignedOut : FixedStartResult
  /** The backend refused, or answered without what the device needs. The device was not touched. */
  data object ServerError : FixedStartResult
  /** The link stays up, so a retry needs no reconnect. */
  data class DeviceFailed(val result: ConfigResult) : FixedStartResult
}

/**
 * Starts a fixed session over an already connected [link] (DOMAIN_KNOWLEDGE.md "Starting a fixed
 * session"). Registers it on the backend first: the AirBeam needs a token from that answer, and Mini V2
 * also its sensor_type_ids. Stores nothing locally — the AirBeam uploads by itself.
 */
class FixedSessionStarter(
  private val link: AirBeamLink,
  /** The phone's standard UTC offset in whole hours, which V1 Wi-Fi configs carry ([standardOffsetHours]). */
  private val zoneOffsetHours: () -> Int,
  private val register: suspend (NewFixedSession) -> RegisteredFixedSession?,
) {
  private val log = Logger.withTag("FixedSessionStarter")

  suspend fun start(
    target: DiscoveredAirBeam,
    details: NewSessionDetails,
    placement: Placement,
    uplink: Uplink,
  ): FixedStartResult {
    // Checked before registering, so a refused start leaves no empty session on the backend.
    if (uplink == Uplink.Cellular && target.device !in CellularDevices) {
      return FixedStartResult.DeviceFailed(ConfigResult.BadConfigFailure)
    }

    val session = NewFixedSession(Uuid.random(), details.name, details.tags, target, placement)
    val registered = try {
      register(session) ?: return FixedStartResult.SignedOut
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (e: Exception) {
      log.e(e) { "${session.uuid}: backend create failed" }
      return FixedStartResult.ServerError
    }
    val config = configFor(target.device, session.uuid, registered, placement.coordinates, uplink)
      ?: return FixedStartResult.ServerError

    val result = link.configure(config)
    if (result != ConfigResult.Success) return FixedStartResult.DeviceFailed(result)
    link.disconnect()
    log.i { "Fixed ${session.uuid} running on ${target.name}" }
    return FixedStartResult.Started
  }

  /** Null when the backend's answer lacks what the device needs. */
  private fun configFor(
    device: AirBeamDevice,
    uuid: Uuid,
    registered: RegisteredFixedSession,
    at: GeoLocation,
    uplink: Uplink,
  ): SessionConfig? = when (uplink) {
    Uplink.Cellular -> SessionConfig.FixedCellular(uuid, registered.userToken, at.latitude, at.longitude)
    is Uplink.Wifi ->
      if (device == AirBeamDevice.Mini.V2) miniV2Config(uuid, registered, at, uplink.credentials)
      else SessionConfig.FixedWiFi(
        uuid = uuid,
        authToken = registered.userToken,
        latitude = at.latitude,
        longitude = at.longitude,
        ssid = uplink.credentials.ssid,
        password = uplink.credentials.password,
        zoneOffset = zoneOffsetHours(),
      )
  }

  /** Mini V2 uploads with the session token and tags each reading with a sensor_type_id. */
  private fun miniV2Config(uuid: Uuid, registered: RegisteredFixedSession, at: GeoLocation, wifi: WifiCredentials): SessionConfig? {
    val ids = registered.sensorTypeIds
    val pm1 = ids[MINI_PM1] ?: ids[PM1]
    val pm25 = ids[MINI_PM25] ?: ids[PM25]
    if (pm1 == null || pm25 == null) {
      log.e { "$uuid: no PM sensor_type_id in $ids" }
      return null
    }
    return SessionConfig.FixedWiFi(
      uuid = uuid,
      authToken = registered.sessionToken,
      latitude = at.latitude,
      longitude = at.longitude,
      ssid = wifi.ssid,
      password = wifi.password,
      zoneOffset = 0, // Mini V2's 0x13 carries none
      pm1Index = pm1,
      pm25Index = pm25,
    )
  }
}

private val CellularDevices = setOf(AirBeamDevice.AirBeam2, AirBeamDevice.AirBeam3)

/** The live v3 backend answers with the names we sent (as legacy expects); API_V3.md documents the canonical ones. */
private const val MINI_PM1 = "AirBeamMini-PM1"
private const val MINI_PM25 = "AirBeamMini-PM2.5"
private const val PM1 = "AirBeam-PM1"
private const val PM25 = "AirBeam-PM2.5"
