package pl.llp.aircasting.bluetooth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

sealed interface SessionAction {
  data class Connect(val target: DiscoveredAirBeam) : SessionAction
  data class ConfigureAndStart(val config: SessionConfig) : SessionAction
  data object StopSession : SessionAction
  data object Disconnect : SessionAction
  data object UserCancelled : SessionAction
}

sealed interface SessionState {
  data object Idle : SessionState

  data class Connecting(
    val target: DiscoveredAirBeam,
    val attempt: Int = 1
  ) : SessionState

  data class Connected(
    val device: AirBeamDevice,
    val connection: AirBeamConnection,
    val target: DiscoveredAirBeam
  ) : SessionState

  data class Configuring(
    val device: AirBeamDevice,
    val connection: AirBeamConnection,
    val config: SessionConfig,
    val target: DiscoveredAirBeam
  ) : SessionState

  data class Recording(
    val device: AirBeamDevice,
    val connection: AirBeamConnection,
    val config: SessionConfig,
    val target: DiscoveredAirBeam,
    val startTimeMs: Long
  ) : SessionState

  data class Reconnecting(
    val target: DiscoveredAirBeam,
    val attempt: Int,
    val previousConfig: SessionConfig? = null
  ) : SessionState

  data class Failed(val reason: FailureReason) : SessionState
}

interface AirBeamSessionController {
  val state: StateFlow<SessionState>
  fun dispatch(action: SessionAction)
}

class DefaultAirBeamSessionController(
  private val connector: AirBeamConnector,
  private val clock: Clock = Clock.System,
  private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : AirBeamSessionController {

  private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
  override val state: StateFlow<SessionState> = _state.asStateFlow()

  private val log = Logger.withTag("AirBeamSessionController")

  private var activeConnection: AirBeamConnection? = null
  private var activeTarget: DiscoveredAirBeam? = null
  private var activeConfig: SessionConfig? = null
  private var connectJob: Job? = null
  private var configJob: Job? = null
  private var statusJob: Job? = null
  private var reconnectJob: Job? = null

  override fun dispatch(action: SessionAction) {
    log.i { "Dispatched action: $action (current state: ${_state.value})" }
    when (action) {
      is SessionAction.Connect -> handleConnect(action.target)
      is SessionAction.ConfigureAndStart -> handleConfigureAndStart(action.config)
      SessionAction.StopSession -> handleDisconnect()
      SessionAction.Disconnect -> handleDisconnect()
      SessionAction.UserCancelled -> handleDisconnect()
    }
  }

  private fun handleConnect(target: DiscoveredAirBeam) {
    cancelActiveSession()
    activeTarget = target
    _state.value = SessionState.Connecting(target, attempt = 1)

    connectJob = externalScope.launch {
      connectInternal(target)
    }
  }

  private suspend fun connectInternal(target: DiscoveredAirBeam) {
    try {
      val conn = connector.connect(target)
      val outcome = conn.status.first {
        it is ConnectionStatus.Ready || it is ConnectionStatus.Failed
      }

      when (outcome) {
        is ConnectionStatus.Ready -> {
          activeConnection = conn
          activeTarget = target
          _state.value = SessionState.Connected(outcome.device, conn, target)
          observeConnectionStatus(conn, target)
        }
        is ConnectionStatus.Failed -> {
          _state.value = SessionState.Failed(outcome.reason)
        }
        else -> {
          _state.value = SessionState.Failed(FailureReason.HandshakeFailed)
        }
      }
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (e: Exception) {
      log.e(e) { "Error connecting to ${target.name}" }
      _state.value = SessionState.Failed(FailureReason.HandshakeFailed)
    }
  }

  private fun handleConfigureAndStart(config: SessionConfig) {
    val currentState = _state.value
    if (currentState !is SessionState.Connected) {
      log.w { "Cannot configure and start session when not in Connected state. Current: $currentState" }
      return
    }

    val conn = currentState.connection
    val device = currentState.device
    val target = currentState.target
    activeConfig = config

    configJob?.cancel()
    _state.value = SessionState.Configuring(device, conn, config, target)

    configJob = externalScope.launch {
      val result = conn.configure(config)
      if (result is ConfigResult.Success) {
        val now = clock.now().toEpochMilliseconds()
        _state.value = SessionState.Recording(device, conn, config, target, startTimeMs = now)
        log.i { "Session recording started successfully for device: $device" }
      } else {
        log.e { "Configuration failed with result: $result" }
        _state.value = SessionState.Failed(FailureReason.HandshakeFailed)
      }
    }
  }

  private fun observeConnectionStatus(connection: AirBeamConnection, target: DiscoveredAirBeam) {
    statusJob?.cancel()
    statusJob = connection.status.onEach { status ->
      log.d { "Observed connection status change: $status" }
      if (status is ConnectionStatus.DisconnectedUnexpectedly) {
        val currentState = _state.value
        if (currentState is SessionState.Recording || currentState is SessionState.Connected) {
          triggerAutoReconnect(target, currentState)
        } else {
          _state.value = SessionState.Idle
        }
      }
    }.launchIn(externalScope)
  }

  private fun triggerAutoReconnect(target: DiscoveredAirBeam, lastKnownState: SessionState) {
    statusJob?.cancel()
    reconnectJob?.cancel()

    val prevConfig = (lastKnownState as? SessionState.Recording)?.config ?: activeConfig
    _state.value = SessionState.Reconnecting(target, attempt = 1, previousConfig = prevConfig)

    reconnectJob = externalScope.launch {
      var attempt = 1
      val maxAttempts = 5
      var reconnected = false

      while (attempt <= maxAttempts && !reconnected) {
        log.i { "Auto-reconnect attempt $attempt for ${target.name}" }
        _state.value = SessionState.Reconnecting(target, attempt, previousConfig = prevConfig)
        
        delay((2 * attempt).seconds)

        try {
          val conn = connector.connect(target)
          val outcome = conn.status.first {
            it is ConnectionStatus.Ready || it is ConnectionStatus.Failed
          }

          if (outcome is ConnectionStatus.Ready) {
            activeConnection = conn
            reconnected = true
            log.i { "Successfully reconnected to ${target.name} on attempt $attempt" }

            if (prevConfig != null) {
              _state.value = SessionState.Configuring(outcome.device, conn, prevConfig, target)
              val configResult = conn.configure(prevConfig)
              if (configResult is ConfigResult.Success) {
                _state.value = SessionState.Recording(
                  outcome.device,
                  conn,
                  prevConfig,
                  target,
                  startTimeMs = clock.now().toEpochMilliseconds()
                )
              } else {
                _state.value = SessionState.Connected(outcome.device, conn, target)
              }
            } else {
              _state.value = SessionState.Connected(outcome.device, conn, target)
            }
            observeConnectionStatus(conn, target)
          }
        } catch (_: Exception) {
          log.w { "Reconnect attempt $attempt failed" }
        }

        attempt++
      }

      if (!reconnected) {
        log.e { "Auto-reconnect failed after $maxAttempts attempts" }
        _state.value = SessionState.Failed(FailureReason.LinkTimeout)
      }
    }
  }

  private fun handleDisconnect() {
    cancelActiveSession()
    _state.value = SessionState.Idle
  }

  private fun cancelActiveSession() {
    connectJob?.cancel()
    connectJob = null
    configJob?.cancel()
    configJob = null
    statusJob?.cancel()
    statusJob = null
    reconnectJob?.cancel()
    reconnectJob = null

    val conn = activeConnection
    activeConnection = null
    activeTarget = null
    activeConfig = null

    if (conn != null) {
      externalScope.launch {
        try {
          conn.disconnect()
        } catch (e: Exception) {
          log.w(e) { "Error disconnecting connection" }
        }
      }
    }
  }
}
