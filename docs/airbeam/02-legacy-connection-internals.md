# AirBeam Communication — Legacy Connection Internals (extracted from the old Android app)

> **What this is.** A factual extraction of *how the legacy AircastingAndroid app
> (`dev` branch) actually connects* to the four AirBeam devices — discovery, per-device
> handshake, auth, the connection abstraction, and reconnection. Every claim cites the
> legacy source as `file:line`.
>
> **Why it exists.** `01-connecting.md` says *what* connection must achieve (behavioral,
> device-agnostic). `ble_mobile_app_guide.md` gives the Mini **V2** wire protocol
> exhaustively. Neither documents the legacy mechanics for **AB2 / AB3 / Mini V1**, the
> **auth-credential source** (the §9 open question), or the legacy **abstraction shape**.
> This doc fills that gap, and annotates each area **KEEP** / **DISCARD** for the KMP rewrite.
>
> **Source.** All paths below are under
> `app/src/main/java/pl/llp/aircasting/util/helpers/` in the legacy repo unless noted.
> Line numbers are from the `dev` branch at extraction time — treat as approximate anchors.

---

## 1. Scope & relationship to the other docs

| Doc | Level | Covers |
|-----|-------|--------|
| `01-connecting.md` | Behavioral spec | *What* must happen, device-agnostic |
| `ble_mobile_app_guide.md` | Byte protocol | Mini **V2** wire format |
| **this doc** | Legacy extraction | *How* the old code connects AB2 / AB3 / Mini V1 + abstraction |

Mini V2 mechanics are **not** duplicated here — see `ble_mobile_app_guide.md`.

---

## 2. Discovery & identification

**Legacy uses classic `BluetoothAdapter.startDiscovery()` for *every* device type** —
no `BluetoothLeScanner`, no `ScanFilter`, no Nordic scanner, and it **never reads advertised
service UUIDs at scan time**. Found devices arrive via a `BroadcastReceiver` on
`BluetoothDevice.ACTION_FOUND`; the bonded-device list seeds results too.

- Scan entry: `bluetooth/BluetoothManagerDefault.kt:28-30` (`adapter?.startDiscovery()`),
  bonded via `:15-18`.
- UI live scan: `SelectDeviceController.kt:17-26,52-54,65-68`.
- Reconnect discovery: `AirBeamDiscoveryService.kt:29-46,63-70`, `DISCOVERY_TIMEOUT = 5000L` (`:26`).

**Type is decided purely from the advertised name**, at `DeviceItem` construction
(`data/model/DeviceItem.kt:15,30-40`). Substrings, checked in this order
(`DeviceItem.kt:25-28`):

```
airbeam2      -> AIRBEAM2
airbeam3      -> AIRBEAM3
airbeammini   -> AIRBEAMMINI   (one enum value — name does NOT encode V1 vs V2)
airbeam       -> AIRBEAM1      (generic fallback)
```

Service UUIDs exist but are consulted **only after connecting** (inside GATT
`isRequiredServiceSupported`), never for discovery:

| Device | GATT service | Notes |
|--------|--------------|-------|
| AB3 **and** Mini V1 | `0000ffdd-0000-1000-8000-00805f9b34fb` | **shared**; differ only by measurement characteristics |
| Mini V2 | `a0e1f000-0001-4b3c-8e9a-1f2d3c4b5a60` | distinct custom service |
| AB2 | `00001101-0000-1000-8000-00805F9B34FB` | classic RFCOMM SPP, not GATT |

**Mini V1-vs-V2 is resolved LATE** — connect-V2-first, fall back to V1 on failure
(`AirBeamMiniFallbackConnector.kt:47-57,100-132`). `DeviceItem.firmwareVersion` defaults to
`V1` and is only authoritatively set to `V2` after a successful V2 connect (`:82-91`).

**`DeviceItem`** carries: `name`, `address` (MAC), `id = name.split(":","-").last()`,
`type`, `firmwareVersion`; the live `BluetoothDevice` is `@IgnoredOnParcel` and re-resolved
from the MAC address downstream (`DeviceItem.kt:5,10-11`; re-resolve at
`AirBeamMiniFallbackConnector.kt:61-63`).

- **DISCARD:** classic discovery + name-only typing + late V1/V2 fallback + MAC-centric id.
- **KEEP / IMPROVE:** BLE scan (Kable) exposes advertised service UUIDs → resolve
  **V1 vs V2 at discovery** (`a0e1f000…` present → V2, else V1). This is the improvement
  `01-connecting.md` §2 asks for and the legacy could not do. AB3 and Mini V1 still can't be
  split by advertised UUID (shared `0000ffdd`) — split them by name (`airbeam3` vs
  `airbeammini`), consistent with legacy.
- **iOS reality:** no MAC. Use Kable's opaque platform identifier → your `DeviceId` value class.
  AB2 (classic) will never appear in a BLE scan — see §8.

---

## 3. Per-device handshake

### 3.0 Shared framing — `common/HexMessagesBuilder.kt`

Frame = `0xFE, <code>, <ASCII-hex payload>, 0xFF` (`:8-9,78-98`; payload is the ASCII string
hex-encoded, `:67-76`). Codes (`:13-14`): `UUID_CODE = 0x04`, `AUTH_TOKEN_CODE = 0x05`.

The auth message is **not** a raw token — `authTokenMessage()` (`:38-43`) builds
`"<authToken>:X"`, Base64-encodes it (`NO_WRAP`), then frames with `0x05`:
```
frame(0x05, Base64("<authToken>:X"))
```

Every handshake writes **two things, in order**: `0x04` = **session UUID** (identity),
then `0x05` = **auth credential**.

### 3.1 AirBeam 3 (BLE)

`AirBeam3Configurator` only defines measurement UUIDs; all handshake logic is in the base
`SyncableAirBeamConfigurator`.

- Service `0000ffdd`; **config (write) characteristic `0000ffde`** — all handshake writes go here.
- Measurement chars (`AirBeam3Configurator.kt:29-43`): temp `0000ffe1`, humidity `0000ffe3`,
  pm1 `0000ffe4`, pm2.5 `0000ffe5`, pm10 `0000ffe6`.
- Handshake — `sendAuth(uuid)` (`SyncableAirBeamConfigurator.kt:160-168`): atomic Nordic queue
  `uuidRequest(0x04)` → `sleep(500)` → `authRequest(0x05)`, `WRITE_TYPE_DEFAULT`.
  UUID = session UUID; token = `mSettings.getAuthToken()`.

### 3.2 AirBeam Mini V1 (BLE)

`AirBeamMiniConfigurator` extends the **same** `SyncableAirBeamConfigurator` → identical
service `0000ffdd`, config char `0000ffde`, UUID→auth order, `0xFE/0xFF` framing, and
`sleep(500)` timing as AB3. Only differences:
- Measurement chars (`AirBeamMiniConfigurator.kt:36-40`): pm1 `0000ffe4`, pm2.5 `0000ffe5`,
  battery `0000ffe7` (no temp/humidity/pm10).
- Overrides `reconnectMobileSession()` to (re)start `BatteryLevelService` (`:42-53`).

### 3.3 AirBeam 2 (classic RFCOMM serial)

`AirBeam2Connector.kt`: opens `createRfcommSocketToServiceRecord(SPP_SERIAL)` on a manual
`Thread`, `socket.connect()`, then **`sleep(5000)`** (`ESTIMATED_CONNECTING_TIME_SECONDS`,
`:33,72-85`) before writing anything. Auth over the `OutputStream`
(`AirBeam2Configurator.kt:14-18`): `sendUUID` → **`sleep(3000)`** → `sendAuthToken`. Byte
format identical to BLE (same `HexMessagesBuilder`), written raw + flushed.

### 3.4 Mini V2

No auth handshake. See `ble_mobile_app_guide.md` §2-3 (subscribe, ~300 ms settle, device
sends Status). Not repeated here.

- **DISCARD:** hardcoded `sleep(5000)`/`sleep(3000)`/`sleep(500)` timing guesses; fire-and-forget
  auth with no verification (see §5).
- **KEEP:** the `0xFE/0xFF` framing + `Base64("<token>:X")` encoding + UUID-then-auth order — these
  are the wire contract. Reimplement as **pure `commonMain` byte code** (kotlinx, unit-testable,
  no radio).

---

## 4. Auth credential — where it comes from  *(resolves `01-connecting.md` §9)*

**The credential is the user's account `authentication_token`, obtained from the AirCasting
backend at login and cached in SharedPreferences under `"auth_token"`. It is not per-session,
not hardcoded, not fetched at connect time.**

Trace:
- Read at handshake: `mSettings.getAuthToken()` → `Settings.kt:56-58`, key `AUTH_TOKEN_KEY="auth_token"` (`:13`).
- Written at login: `Settings.login(...)` `saveToSettings(AUTH_TOKEN_KEY, authToken)` (`Settings.kt:248-258`),
  called from `LoginController.kt:63-68` (and `CreateAccountController.kt:93-96`).
- Origin: `LoginService.performLogin` (`login/LoginService.kt:19-35`) → `ApiService.login()`
  (`data/api/services/ApiService.kt:50-51`, `@GET`), field `authentication_token`
  (`data/api/response/UserResponse.kt:8-9`).

The identity written *first* (`0x04`) is the **session UUID**, posted via `SendSessionAuth`
from `NewSessionController.kt:525`.

- **KMP implication:** the connection layer must *read* this token from a shared settings store
  (multiplatform-settings or DataStore), fed by the login/auth feature. **Connection depends on
  auth having happened first** — model that dependency explicitly.

---

## 5. Connection state & the "ready" signal

Legacy exposes connection state through **three parallel mechanisms**:
1. `AirBeamConnector.Listener` callbacks — `onConnectionSuccessful/onConnectionFailed/onDisconnect`
   (`common/connector/AirBeamConnector.kt:28-32`), implemented by `AirBeamService`.
2. `StateFlow<AirbeamConnectionStatus?>` = `(deviceItem, sessionUUID, isConnected)`
   (`data/model/AirbeamConnectionStatus.kt`; emitted `AirBeamService.kt:113-121`).
3. EventBus events: `SensorDisconnectedUnexpectedlyEvent`, `AirBeamConnectionFailedEvent`,
   `AirBeamDiscoveryFailedEvent`, `ReconnectionEvent` (sticky), etc. **There is no single
   `SensorConnectedEvent`.**

**Critical: legacy "ready" ≠ `01-connecting.md` "ready."** "Connected" fires on the transport
callback (`onConnectionSuccessful`, `AirBeamConnector.kt:115-122`); **auth is sent afterward**
via the separate `SendSessionAuth` event, and **no device ACK confirms auth succeeded** — auth
writes only report *failure* via `.fail{}`.

- **DISCARD:** three overlapping state channels; "ready-before-handshake."
- **KEEP:** a single observable status. Your `StateFlow<ConnectionStatus>` (already modelled)
  replaces all three. **Fold handshake into reaching `Ready`** — don't publish ready until
  identity+auth (and, for V2, state read) complete.

---

## 6. The abstraction: Connector / Configurator / Reader

Clean role separation worth carrying forward:

- **Connector** (`AirBeamConnector`, abstract) — transport + lifecycle owner: connect/disconnect,
  30 s timeout, state flags, listener. Template methods `start/stop/sendAuth/configureSession/
  reconnectMobileSession/...` (`common/connector/AirBeamConnector.kt:45-54,77-79`).
- **Configurator** (`AirBeamBleConfigurator`) — GATT service discovery + protocol/command writes
  (`airbeamSyncable/configurator/AirBeamBleConfigurator.kt:9`).
- **Reader** — parses inbound notifications into events (`SyncableAirBeamReader.kt:24`,
  `AirBeam2Reader.kt`).
- **Factory** picks by `DeviceItem.Type` (`AirBeamConnectorFactory.kt:21-41`).

Hierarchy:
```
AirBeamConnector (abstract)
├── AirBeam2Connector             — raw classic BluetoothSocket + Thread
├── SyncableAirBeamConnector      — Nordic ConnectionObserver (AB3)
└── AirBeamMiniFallbackConnector  — Nordic; V2-then-V1

AirBeamBleConfigurator (interface)
├── SyncableAirBeamConfigurator (abstract, Nordic BleManager)
│   ├── AirBeam3Configurator
│   └── AirBeamMiniConfigurator      (V1)
└── AirBeamMiniV2Configurator (Nordic BleManager, separate protocol)

SensorService (foreground) → AirBeamService → AirBeamRecordSessionService → AirBeamReconnectSessionService
```

**BLE library:** Nordic `no.nordicsemi.android.ble` for AB3/Mini (`BleManager`,
`ConnectionObserver`; `connect(device).timeout(0).retry(3,100).useAutoConnect(true)`,
`SyncableAirBeamConnector.kt:41-47`). **AB2 = raw Android classic socket**, not GATT, not Nordic.

- **KEEP:** the **Connector = transport / Configurator = protocol / Reader = parser** triad — it
  maps cleanly onto KMP: Kable `Peripheral` = transport; a per-device Configurator (commonMain)
  = handshake + commands; a Reader = notification→domain mapping.
- **DISCARD:** Nordic (Android-only) and the raw classic socket → replaced by Kable in commonMain.

---

## 7. Ownership, backgrounding & reconnection

**Ownership: a foreground Service.** `SensorService` calls `startForeground(..,
FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)` and returns `START_REDELIVER_INTENT`
(`services/SensorService.kt:72-86`; comment: *"devices need to connect in the Foreground
Service, otherwise measurements will not record in background"*). `DeviceItem`/`sessionUUID`
ride as Parcelable intent extras for redelivery. All services declared
`foregroundServiceType="connectedDevice"`.

**Reconnection:** `AirBeamReconnector` + `AirBeamReconnectSessionService`. Trigger:
unexpected `onDeviceDisconnected` → `SensorDisconnectedUnexpectedlyEvent` → **mobile sessions
only** → `tryToReconnectPeriodically` (`AirBeamService.kt:123-140`). Retries every
`RECONNECTION_TRIES_INTERVAL = 2000L` ms with **no hard cap** (comment
`AirBeamReconnector.kt:52-56`). On success, session status → `RECORDING` and
`reconnectMobileSession()` re-arms device mobile mode.

- **KEEP (behavior):** connection outlives the UI; mobile auto-reconnect; distinguish
  user-disconnect from unexpected drop (your `ConnectionStatus.DisconnectedUnexpectedly`).
- **KMP mapping:** ownership is the `01-connecting.md` §9 question. Android → foreground service
  (`expect`/`actual` + Android `FOREGROUND_SERVICE_CONNECTED_DEVICE`). iOS → CoreBluetooth
  background mode `bluetooth-central` + state restoration. Reconnect loop → a clean Flow + backoff
  policy instead of nested services/EventBus.

---

## 8. Known fragilities in the legacy code (all quoted with source)

Reasons the rewrite exists — do **not** reproduce these:

- **V1/V2 connect-then-fallback** — the entire `AirBeamMiniFallbackConnector` is a
  try-V2-fall-back-to-V1 hack (`:47-57,100-132`). Killed by discovery-time UUID detection (§2).
- **Nordic double-fire** — `fail` *and* `onDeviceFailedToConnect` fire for one failure; needs
  `v2FailureHandled`/`v1FailureHandled` idempotency flags (`:37-44`).
- **Late-callback misclassification** — `.fail` for the V2 leg can fire *after*
  `onDeviceDisconnected`; must capture `isV2Leg` at closure creation (`:69-74`).
- **Service-not-supported kills the service** — V2-not-supported surfaces as
  `onDeviceDisconnected`, whose default teardown `stopSelf()`s the foreground service before the
  V1 fallback, leaving `ConfigureSession` with no subscriber (`:214-228`).
- **BLE+Wi-Fi coexistence** — mid-sync BLE drop deliberately swallowed on Android 12 + ESP32
  (`:202-212`).
- **`nimble` id mismatch** — a disconnect-time `DeviceItem` yields `id="nimble"`, breaking
  reconnect + UI; worked around with a `deviceAddressByDeviceItem` cache
  (`AirBeamConnector.kt:132-143`).
- **AB2 hardcoded `sleep(5000)`** after connect (`AirBeam2Connector.kt:78-79`).
- **Duplicate UUID** — config and SD-metadata characteristics both `0000ffde`
  (`SyncableAirBeamConfigurator.kt:117-126`).
- Pervasive `Log.d("[RECONNECT]"/"[FG-DEBUG]", …)` — these flows were under active debugging.

---

## 9. KMP redesign implications (summary)

| Concern | Legacy | KMP target |
|---------|--------|-----------|
| Scan | classic discovery, all types | Kable BLE scan; AB2 excluded (see below) |
| Type/gen detection | name only; V1/V2 late fallback | advertised service UUID at discovery |
| Stable id | MAC / `name.last()` | opaque `DeviceId` over Kable platform identifier |
| Handshake framing | `HexMessagesBuilder` (Android) | pure `commonMain` byte code, unit-tested |
| Auth token | SharedPreferences `"auth_token"` | shared settings store, fed by login feature |
| "Ready" | transport callback, auth after, no ACK | ready = link + handshake (+V2 state) verified |
| State | 3 channels (callback/flow/EventBus) | one `StateFlow<ConnectionStatus>` |
| Roles | Connector/Configurator/Reader | keep the triad; Kable `Peripheral` = transport |
| BLE lib | Nordic (Android) + raw classic socket | Kable (commonMain) |
| Ownership | foreground service | `expect`/`actual`: FGS (Android) / bg-central (iOS) |
| Reconnect | nested services + EventBus, no cap | Flow + backoff policy |

**AB2 open decision (`01-connecting.md` §9):** classic RFCOMM SPP. Kable/BLE cannot see it;
iOS cannot do classic serial without MFi. Recommend: **drop AB2**, or gate it to an
Android-only `expect`/`actual` classic-socket transport if product requires it. Do not contort
the common BLE abstraction to fit it.
