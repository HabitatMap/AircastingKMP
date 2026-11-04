package pl.llp.aircasting.recording

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.SessionConfig
import kotlin.uuid.Uuid

/** The network a fixed AirBeam uploads over. Ends up in logs, so toString() leaves the password out. */
data class WifiCredentials(val ssid: String, val password: String) {
  override fun toString() = "WifiCredentials(ssid=$ssid)"
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
 * session"). Registers it on the backend first: the AirBeam needs the token and the
 * sensor_type_ids from that answer. Stores nothing locally — the AirBeam uploads by itself.
 */
class FixedSessionStarter(
  private val link: AirBeamLink,
  private val register: suspend (NewFixedSession) -> RegisteredFixedSession?,
) {
  private val log = Logger.withTag("FixedSessionStarter")

  suspend fun start(
    target: DiscoveredAirBeam,
    details: NewSessionDetails,
    placement: Placement,
    wifi: WifiCredentials,
  ): FixedStartResult {
    // AB2/AB3/Mini V1 take a user token over setAuthCommand; what they get with v3 is still open.
    if (target.device != AirBeamDevice.Mini.V2) return FixedStartResult.DeviceFailed(ConfigResult.BadConfigFailure)

    val session = NewFixedSession(Uuid.random(), details.name, details.tags, target, placement)
    val registered = try {
      register(session) ?: return FixedStartResult.SignedOut
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (e: Exception) {
      log.e(e) { "${session.uuid}: backend create failed" }
      return FixedStartResult.ServerError
    }
    val ids = registered.sensorTypeIds
    val pm1 = ids[MINI_PM1] ?: ids[PM1]
    val pm25 = ids[MINI_PM25] ?: ids[PM25]
    if (pm1 == null || pm25 == null) {
      log.e { "${session.uuid}: no PM sensor_type_id in ${registered.sensorTypeIds}" }
      return FixedStartResult.ServerError
    }

    val result = link.configure(
      SessionConfig.FixedWiFi(
        uuid = session.uuid,
        authToken = registered.sessionToken,
        latitude = placement.coordinates.latitude,
        longitude = placement.coordinates.longitude,
        ssid = wifi.ssid,
        password = wifi.password,
        zoneOffset = 0, // Mini V2's 0x13 carries none; V1 devices will need the real one
        pm1Index = pm1,
        pm25Index = pm25,
      )
    )
    if (result != ConfigResult.Success) return FixedStartResult.DeviceFailed(result)
    link.disconnect()
    log.i { "Fixed ${session.uuid} running on ${target.name}" }
    return FixedStartResult.Started
  }
}

/** The live v3 backend answers with the names we sent (as legacy expects); API_V3.md documents the canonical ones. */
private const val MINI_PM1 = "AirBeamMini-PM1"
private const val MINI_PM25 = "AirBeamMini-PM2.5"
private const val PM1 = "AirBeam-PM1"
private const val PM25 = "AirBeam-PM2.5"