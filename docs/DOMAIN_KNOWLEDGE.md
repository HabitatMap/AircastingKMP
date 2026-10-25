# AirCasting — Domain Knowledge Base

> **Purpose.** Single source of truth for *what the app does* — the business/technical
> requirements — independent of platform or framework. Written to survive the Kotlin
> Multiplatform (KMP) rewrite. Organized by **capability**, not by current code.
>
> **Companion doc:** [`KMP_ARCHITECTURE.md`](./KMP_ARCHITECTURE.md) — the target module
> structure derived from this one.
>
> **Scope note.** V2 AirBeam Mini firmware/BLE protocol details are NOT repeated here —
> they live in [`.claude/ble_mobile_app_guide.md`](../.claude/ble_mobile_app_guide.md),
> which stays the source of truth for the V2 wire protocol. This doc references it.
>
> **Tag legend** (portability of each concern to KMP):
> - **KEEP** — portable pure logic → `commonMain`.
> - **PLATFORM** — needs `expect/actual` or native impl (BLE, services, filesystem, UI).
> - **CHANGE-LIKELY** — architectural swap (EventBus→Flows, Room→SQLDelight, Retrofit→Ktor, Dagger→Koin, MVC→Compose).

---

## 0. What AirCasting Is

A citizen-science air-quality app. Users connect an AirBeam sensor (or the phone mic)
over Bluetooth, **record sessions** of measurements (PM1/PM2.5/PM10, temperature,
humidity, sound level), see them live on a **graph + map + measurement table**, and
**sync** them to the AirCasting backend where they can be shared and browsed. Users can
also **follow** remote fixed sensors and watch them update.

Two session shapes, four devices, several sync transports. The whole product is the
matrix of *(device × session-type × transport)* plus the display/backend layer on top.

---

## 1. Device Matrix — the modular core

The single most important axis. Everything device-specific must sit behind one driver
abstraction so all four plug in interchangeably. Details of the V1/V2 protocols below.

| Device | Transport | Wire protocol | Sessions | Battery | Time-sync | Sync mechanism | iOS-portable? |
|--------|-----------|---------------|----------|---------|-----------|----------------|---------------|
| **AirBeam2** (non-syncable) | Classic BT **RFCOMM/SPP** (`BluetoothSocket`) | ASCII→hex framed (V1) | mobile | none | at-configure once | none | ❌ **No** — SPP has no CoreBluetooth equivalent |
| **AirBeam3** | BLE GATT (`0000ffdd`) | ASCII→hex framed (V1) | mobile + fixed | from `NN%` in notification | at-configure once | SD-card CSV over BLE | ✅ |
| **AirBeam Mini V1** | BLE GATT (`0000ffdd`) | ASCII→hex framed (V1) | mobile + fixed | dedicated char `ffe7` + `NN%` | at-configure once | SD-card CSV over BLE | ✅ |
| **AirBeam Mini V2** | BLE GATT (`a0e1f000`) + WiFi | Binary little-endian opcodes (V2) | mobile + fixed | Status byte[1], signed (sign = charging) | `SetTime 0x15`, initial + hourly (mobile only) | BLE stream (`StartBleSync 0x16`); firmware-side WiFi for fixed | ✅ |

Key facts:
- **AB2 is classic Bluetooth serial, not BLE.** SPP UUID `00001101-0000-1000-8000-00805F9B34FB`.
  This is Android-only; the rewrite must decide to keep it Android-gated or drop it.
- **Mini V1 vs V2 both advertise as `"airbeammini"`.** Distinguished at *connect* time by
  which GATT service the device exposes (V2 = `a0e1f000…`, V1 = `0000ffdd…`). Currently done
  via a brittle connect-then-fallback (`AirBeamMiniFallbackConnector`); the V2 guide §1 notes
  the *correct* approach is a scan-time service-UUID filter. **CHANGE-LIKELY.**
- **Discovery today is classic `BluetoothAdapter.startDiscovery()` + name matching** — there is
  no BLE `ScanFilter` / `BluetoothLeScanner` anywhere. Real BLE scanning with service-UUID
  filters is a gap the rewrite must fill. **CHANGE-LIKELY.**
- Device-type detection: `DeviceItem.getType(name)` — case-insensitive substring on the
  advertised name (`airbeam2`, `airbeam3`, `airbeammini`, `airbeam`).

### V1 protocol (AB2/AB3/Mini V1) — `HexMessagesBuilder`, `ResponseParser`
- Frame: `0xFE <configCode> <payload> 0xFF`. Config codes: BT-stream `0x01`, WiFi `0x02`,
  cellular `0x03`, UUID `0x04`, auth-token `0x05`, lat/lng `0x06`, time `0x08`, sync `0x09`,
  clear-SD `0x0a`. Payload = `asciiToHex()` → bytes. **KEEP** (pure byte/string ops).
- **Auth handshake:** write UUID msg, sleep, write auth-token msg (`Base64("$authToken:X")`).
- Response parsing: semicolon-delimited CSV → measurement. Fixed field order: value; sensor
  package; sensor name; type; short type; unit; symbol; 5 thresholds. **KEEP.**
- Measurements only stream in **mobile** mode; fixed uploads go over WiFi/cellular directly.

### V2 protocol (Mini V2) — see `.claude/ble_mobile_app_guide.md`
- Service `a0e1f000-0001-…`; 5 characteristics: Status (notify), Command (write),
  Response (notify), Measurement (indicate), Sync (indicate).
- Binary LE opcodes: ContinueSession `0x10`, DiscardSession `0x11`, StartWiFiSync `0x12`
  (legacy/dormant), NewSessionConfig `0x13`, GetSensors `0x14`, SetTime `0x15`,
  StartBleSync `0x16`.
- No auth handshake — identity is the session UUID inside the config payload.
- Coroutine `CompletableDeferred` command state machine (`WAITING_ACK`/`WAITING_READY`),
  unlike V1 fire-and-forget. Parse/build logic is **KEEP**; the Nordic transport is PLATFORM.

---

## 2. Session Model & Lifecycle

### Domain model (`data/model/Session.kt`) — **KEEP**
Aggregate root. Key fields: `uuid`, `deviceId`, `deviceType`, `type`, `name`, `tags`,
`status`, `startTime`, `endTime`, `version`, `deleted`, `followedAt`, `contribute`,
`locationless`, `indoor`, `streams`, `urlLocation`, `notes`, `averagingFrequency`,
`order`, `isExternal`, `username`, `measurementInterval`, transient `location`,
`streamingMethod`.

Enums:
- `Type`: `MOBILE(0)`, `FIXED(1)`.
- `Status`: `NEW(-1)`, `RECORDING(0)`, `FINISHED(1)`, `DISCONNECTED(2)`.
- `StreamingMethod`: `CELLULAR(0)`, `WIFI(1)` (fixed only).

1 Session → N `MeasurementStream` → N `Measurement`; 1 Session → N `Note`.

### State machine — **KEEP**
```
(create)──▶ NEW ──start──▶ RECORDING (mobile) │ FINISHED (fixed, records autonomously)
                              │
              RECORDING ──user disconnect / BLE drop──▶ DISCONNECTED
              DISCONNECTED ──reconnect success──▶ RECORDING
              RECORDING/DISCONNECTED ──stop/finish──▶ FINISHED
```
- Fixed sessions become `FINISHED` immediately on start — the AirBeam records them
  autonomously and POSTs to the backend; the phone only downloads.
- `setAppropriateStatusForStartOfRecording()` encodes the mobile→RECORDING / fixed→FINISHED
  branch. `stopRecording(date)` sets `endTime` + `FINISHED`.

### Recording lifecycle (mobile) — **KEEP** logic / **PLATFORM** services / **CHANGE-LIKELY** EventBus
1. **Connect** — UI starts a foreground service that connects BLE/BT; on success an auth
   handshake fires (`SendSessionAuth`). Connection timeout 30s.
2. **Configure** — user fills details → `StartRecordingEvent`.
3. **Start** — orchestrator sets status, inserts session in DB, posts `ConfigureSession`;
   for fixed, first creates the backend session (below). Mobile also schedules averaging +
   starts the measurement observer. *(Averaging step dropped in the rewrite — §7.)*
4. **Record** — connector writes config to device; measurements flow back to DB + UI.
5. **Disconnect** — unexpected BLE drop → `SensorDisconnectedUnexpectedlyEvent`; session →
   `DISCONNECTED`; a 2s-interval reconnect loop starts (no hard cap).
6. **Resume** — reconnect success → `RECORDING`. User can also disconnect deliberately
   (standalone mode) keeping the session.
   **Reconnect rules, exactly** (`AirBeamService.onMessageEvent(SensorDisconnectedUnexpectedlyEvent)`,
   `AirBeamReconnector`): only for a **mobile** session (fixed sessions record on the device);
   not in standalone mode; one loop at a time; every **2 s**, **no cap** — it ends on
   success, user stop/disconnect, or sync start. On success the device is **resumed, not
   reconfigured** (`reconnectMobileSession()`): AB3/Mini V1 get the mobile-mode config again;
   AB2 gets its mobile-mode message; **Mini V2 keeps running its session through the drop** —
   it gets a time sync, then `ContinueSession` if it reports `HasSavedSession`, nothing if
   `Running`; legacy logs and carries on for any other state. Re-sending a V2 session config
   would be wrong (it would be rejected or start a second session).
   In the rewrite these live in `AirBeamLink` (connection) — whether a drop means
   `DISCONNECTED` or a finished session is the recording side's call.
7. **Finish** — `StopRecordingEvent` → final averaging *(dropped in the rewrite — §7)*, delete active-session rows, status
   `FINISHED`, backend sync. All services/connectors self-tear-down.

Orchestration classes (all **CHANGE-LIKELY** at the EventBus edges, logic **KEEP**):
`SessionManager` (EventBus front door) → `RecordingHandlerImpl` (lifecycle) →
`AveragingService` + `NewMeasurementEventObserver` + repositories. Connection side:
`AirBeamService` (foreground) → `AirBeamConnectorFactory` → `AirBeamConnector` subclasses.

### Mobile vs Fixed — key differences

| Concern | Mobile | Fixed |
|---------|--------|-------|
| Initial status | `RECORDING` | `FINISHED` (autonomous) |
| Backend session created at start | no | **yes** — `POST` returns location + session_token + sensor type IDs |
| WiFi credentials | n/a | required (written to device) |
| Measurement path | device → phone → DB | device → **backend** → phone downloads |
| Interval default | 1s | 60s |
| Location | live GPS per measurement | fixed lat/lng once |
| `followedAt` | not set | set (auto-followed) |

---

## 3. Measurement Data Flow

Three distinct paths coexist today (the rewrite should unify onto one Flow pipeline):

- **Path A — V1 mobile (AB2/AB3/Mini V1/mic):** reader parses stream → posts
  `NewMeasurementEvent` on EventBus → observer builds `Measurement` + `MeasurementStream`
  and saves to DB. Timestamp/location shared across a "set" of streams
  (`defaultNumberOfStreams`: mic/other=1, Mini=2, AB=5) so a 1s tick aligns.
- **Path B — V2 Mini mobile:** configurator writes **directly** to DB (bypasses the
  observer — `startObservingNewMeasurements` is skipped for V2), then also posts
  `NewMeasurementEvent` *only* for UI refresh. Per-record location from a tracked-locations
  buffer.
- **Path C — Fixed sessions:** device POSTs to backend autonomously; phone **downloads** via
  a 60s polling service + on card-expand.

### Measurement location stamping (mobile) — **KEEP**

Every **mobile** measurement gets a location **when it is stored** ("stamping"), never at
upload time — the v3 mobile upload frame has no "no location" value. If the phone's
location is unknown at that moment, the measurement **copies the last known location**.
The rewrite follows the newest legacy variant (`V2MobileMeasurementsInserter` +
`AirBeamMiniV2StateRepository.getClosestLocation`, used for Mini V2 and SD sync):

1. **Tracked location closest in time** to the measurement (binary search over the
   `tracked_locations` buffer) — matters for backfilled/synced records, whose timestamps are
   in the past.
2. else **last known location in this session** — the newest stored measurement of the
   session with non-null lat/lng (`MeasurementDao.lastKnownLatLng`).
3. else **the phone's last known location** (`LocationHelper.lastLocation()`).
4. else `DEFAULT_LOCATION = (40.7128, -74.0060)` (New York) — last resort, should be rare.

Special case: **maps disabled / location-less** session → every measurement gets
`FAKE_LOCATION = (200.0, 200.0)` (`Session.Location.get(location, locationless = true)`).
Path A (live V1) is the older variant: it stamps `LocationHelper.lastLocation()`, else
`DEFAULT_LOCATION`, refreshed once per full set of streams so a set shares one location.

Fixed-session measurements carry no per-measurement location (it lives on the session).

#### In the rewrite (as of 2026-10-21)

- **Live stamping** (`RecordingEngine.save`): `PhoneLocation.latest()` — the newest fix of
  continuous tracking — else `DEFAULT_LOCATION`. Steps 1–2 above are not built yet (no
  `tracked_locations` buffer, no per-session last-known lookup); they matter for the V2 backlog
  and standalone mode, not for live readings.
- **Who runs the GPS** (as of 2026-10-22). `TrackedPhoneLocation` shares one GPS session
  (`stateIn`, `WhileSubscribed`) among everyone tracking, and keeps it 5 s past the last one
  (`GpsHandOff`) so the wizard hands it to the engine without a restart:
  - **the new-session wizard** — `NewSessionViewModel` tracks for as long as it lives, as legacy
    `NewSessionController.startNewSessionWizard` started GPS when the wizard opened;
  - **the engine** — `RecordingEngine.run()` tracks while a session records over the link;
    `stop()` / `goStandalone()` cancel it.
  Platforms supply a `Flow<GeoLocation>` (`androidLocationFixes`: fused 1 s HIGH_ACCURACY, as
  legacy; `iosLocationFixes`: `CLLocationManager` with background updates). The newest fix is
  kept across GPS sessions (`latest()`, legacy step 3).
- **Start waits for a real fix** (fixes the NYC gap, 2026-10-22). Confirmed in the first real
  Mini V2 run (2026-10-21): GPS registered at Start, the first fix came ~3 s later, so the first
  6 readings were stamped New York and the session's backend bounding box stretched from New York
  to Kraków. Now `RecordingViewModel.start` goes `Preparing(Locating)` → `Preparing(Configuring)`:
  it waits on `PhoneLocation.awaitFix()` — the first fix of the *running* GPS session, never one
  left from an earlier session — before configuring the device, so readings (which start right
  after configure) have a fix to copy. With the wizard's warm GPS that is usually instant.
  **Timeout `LocatingTimeout` = 10 s** (indoors): then it records anyway, from the last known fix
  or `DEFAULT_LOCATION`. Legacy had no such wait; it relied on the warm GPS plus the cached fix.
- **Still open — seed from the cached fix.** Emitting the platform's last known location when
  the GPS starts (Android `FusedLocationProviderClient.lastLocation`; iOS
  `CLLocationManager.location`) would cover the timeout case and a fresh install's first session,
  where `latest()` is still null. Not built.
- **Open question — dropping the defaults entirely.** The v3 mobile frame is fixed-layout
  (`float64` lat/lng, no "no location" value), so nulls can't be sent. Storing `null` locally
  and resolving at upload (the session's first real fix; hold rows until one exists) would
  remove `DEFAULT_LOCATION` without mutating `measurements`. A session that never gets any fix
  would still need a sentinel; `(200, 200)` is the backend's convention for indoor *fixed*
  sessions, but **it is unverified whether mobile upload accepts it** — `invalid_location`
  exists and is undocumented. Best outcome would be the backend accepting `NaN` as explicit
  "no location". Not decided; `(200, 200)` for user-chosen locationless sessions is a separate,
  still-unbuilt feature.

### Live readings off the device — **KEEP** (as implemented in the rewrite, 2026-10-20)

What `AirBeamConnection.readings` emits (`SensorReading`: sensor name, unit symbol, value,
device time or none). Sensor name + unit are the stream identity the v3 create call sends.

| Device | Source | Format | Time |
|--------|--------|--------|------|
| AB3 | notify `ffe1` temp F, `ffe3` humidity, `ffe4` PM1, `ffe5` PM2.5, `ffe6` PM10 (`AirBeam3Configurator`) | ASCII line, below | none — stamp on receipt |
| Mini V1 | notify `ffe4`, `ffe5` (`AirBeamMiniConfigurator`; its `ffe7` sends only battery `"85%"`) | ASCII line | none |
| AB2 | RFCOMM socket, one line per reading (`AirBeam2Reader`) | ASCII line | none |
| Mini V2 | indicate `…0005` live, `…0006` sync backlog | binary LE (`ble_mobile_app_guide.md` §6) | device epoch seconds |

- **ASCII line** (`ResponseParser`): `value;package;sensor name;type;short type;unit name;
  unit symbol;T1;T2;T3;T4;T5` — 12 fields, the five thresholds integers. Anything else
  (battery text, torn line) is dropped.
- **Mini V2 names** carry no metadata on the wire; legacy uploads them as
  `AirBeamMini-PM1` / `AirBeamMini-PM2.5`, unit `µg/m³`
  (`AirBeamMiniV2Configurator.postLiveMeasurementEvents`). Keep these exact names so the
  backend streams line up with legacy-recorded sessions.
- **MTU** (Android only; iOS negotiates itself): V1 BLE devices request 517
  (`SyncableAirBeamConfigurator.MAX_MTU`) — a line doesn't fit the default 20-byte payload;
  Mini V2 requests 247 (fits a 244-byte sync chunk). A refused request is logged, not fatal.
- The V2 sync backlog flows through the same `readings` as live data, so the recording side
  has one save path (dedup on `(stream, time)` makes replays harmless).
- **Mini V2 skips one second every ~23 s — accepted, won't be addressed.** Observed in the
  first real run (2026-10-21): a regular 2 s step in both streams' timestamps, every ~23 s.
  The firmware samples slightly slower than 1 Hz and stamps whole RTC seconds, so a second
  goes unused now and then. Nothing is lost — no reading is dropped. Legacy stores the same
  device timestamps, so it has the same gaps. Don't treat a 2 s step as a gap, and don't
  "fix" it by restamping with the phone clock (that would break dedup against the sync
  backlog, which carries the same device times).

### V1 devices on real hardware (2026-10-22) — **KEEP**

Confirmed by logging every raw line on an AB3 and an AB2. The legacy app is the source of
truth for these devices; where the rewrite's own protocol code disagreed with it, legacy won.

- **Stream names and units, exactly as the firmware sends them** (5 streams on both devices):
  `<Model>-F` (`F`), `<Model>-RH` (`%`), `<Model>-PM1`, `<Model>-PM2.5`, `<Model>-PM10`
  (`µg/m³`, micro sign U+00B5), with `<Model>` = `AirBeam3` / `AirBeam2`. They match legacy's
  SD-card CSV names (`AB3CSVLineParameterHandler`: `"$deviceName-$sensor"`). The 5 lines of a
  set arrive in no fixed order. AB2 also sends a non-reading line `BC:SP=00` right after
  configure; the parser drops it.
- **AB3 mobile config needs a location.** With time → UUID → mobile mode only, the AB3
  streamed a set every ~21 s; adding the location (`0x06`) gave 1 Hz. Legacy
  (`SyncableAirBeamConfigurator.configureMobileSession`) always sends it, falling back to
  `DEFAULT_LOCATION` (`Session.Location.get`).
- **Location payload is `lng,lat`** (`HexMessagesBuilder.locationMessage`) — longitude first.
- **AB2 commands are framed `0xFE … 0xFF`, like AB3.** Legacy builds them with the same
  `HexMessagesBuilder`. The rewrite's `AirBeamProtocol.AirBeam2` (no `0xFE`, raw auth token)
  does not match legacy — don't use it. AB2 mobile in the rewrite: UUID, wait 3 s, then
  `0xFE 0x01 0xFF` → 1 Hz. On reconnect, only mobile mode (legacy `reconnectMobileSession`).
- **The auth token is not sent in mobile sessions.** `Base64("$token:X")` is an HTTP Basic
  credential, the firmware's for its *own* uploads (fixed sessions over WiFi/cellular). Legacy
  sent it for mobile too only because `NewSessionController.goToSessionDetails` posts
  `SendSessionAuth` for every AirBeam session. Hardware runs without it: AB3 and AB2 both 1 Hz.
  Also, it's the user's long-lived credential, so it shouldn't go over Bluetooth needlessly.
- **v3 `device.model`** — legacy sends none for mobile sessions; the rewrite uses the CSV
  `deviceName`s: `AirBeam3`, `AirBeam2`, `AirBeamMini`.
- **Mini V1 records as `AirBeamMini-PM1` / `AirBeamMini-PM2.5` (`µg/m³`), model `AirBeamMini`** —
    the same streams and model as Mini V2. From legacy `ABMiniCSVLineParameterHandler`
    (`deviceName = "AirBeamMini"`, `sensorName = "$deviceName-$sensor"`); config is the shared
    AB3 path (`SyncableAirBeamConfigurator`). Not yet confirmed on hardware — check the raw-line
    log on the first run.

UI consumes the DB reactively: Room `LiveData` → repository projections → `SessionsObserver`
(diff engine) → dashboard cards. DB is the intended single source of truth.

---

## 4. Sync Mechanisms

Five distinct flows. Business logic (parsing, routing, dedup, progress math) is **KEEP**;
transports (BLE, WiFi-SoftAP, file I/O) are **PLATFORM**; HTTP is **CHANGE-LIKELY** (→Ktor).

1. **SD-card CSV sync (AB3 / Mini V1).** User-launched wizard. On BLE connect, device streams
   metadata + measurement lines over BLE; `SDCardReader` state-machines the steps
   (MOBILE / FIXED_WIFI / FIXED_CELLULAR), writes per-session CSV files, a **20%-corruption
   gate** (`SDCardCSVFileChecker`) rejects bad steps, then mobile → local insert
   (only measurements newer than DB last-time), fixed → local insert + chunked backend POST
   (~1 month/chunk). Trailing backend `sync()`, then clears the SD card. The wizard has an
   "unplug your AirBeam" step (V1 only; skipped on V2).
2. **V2 reconnect-time auto-sync.** Automatic on every V2 connect: firmware streams stored
   records over the Sync characteristic. Each chunk saved to DB immediately, geolocated from
   the tracked-locations buffer. **Always validate the device session UUID** matches the app
   session before saving (see V2 guide §9).
3. **V2 manual BLE sync (`StartBleSync 0x16`) — current path.** User-initiated
   (`V2BleSyncOrchestrator`). Reroutes Sync indications into memory, streams records,
   firmware auto-clears storage after `Ready`. Progress = bytes-received vs `file_size` from
   the `ReadyToSync` status. Mobile → insert + backend sync; fixed → binary upload + refresh.
4. **V2 WiFi-SoftAP sync — dormant.** `V2SyncOrchestrator` + `V2WifiApConnector` +
   `V2SyncFileDownloader`. Joins the device's SoftAP, `GET /sync`, parses binary blocks.
   Retained but no longer invoked (BLE path replaced it). **PLATFORM** (SoftAP join has no
   clean iOS path — likely why BLE became default).
5. **Backend session sync (`sync_with_versioning`).** `SessionsSyncService`: sends local
   session versions, backend responds with `delete` / `upload` / `download` partitions;
   uploads mobile sessions, downloads remote changes. Also: periodic (60s) fixed-session
   download, one-shot followed-session download.
   **Legacy triggers** (no WorkManager, nothing scheduled): dashboard setup on app start
   (`MainController.setupDashboard`), connectivity regained (`ConnectivityReceiver`, only while
   MainActivity lives), stop recording (`RecordingHandler.stopRecording`), session delete
   (`SessionManager.deleteSession`), pull-to-refresh (`DashboardController`). Only non-recording
   sessions; a failure waits for the next trigger.
   **Rewrite:** mobile upload is split out of sync and runs live, driven by the outbox — see
   [`API_V3.md` → When the uploader runs](./API_V3.md#when-the-uploader-runs--decided-2026-10-19).

**Dedup differs:** V1 mobile filters `time > lastMeasurementTime`; V2 relies on the DB
unique index `(session_id, stream_id, time)` + `IGNORE`. Preserve the dedup — in the rewrite
the key is `PRIMARY KEY (stream_id, time)` (see rule #5).

---

## 5. Display

UI pattern today: hand-rolled **MVC/ViewMvc + Controller** (not MVVM-per-screen), with a few
real `ViewModel`s. `LiveData` + greenrobot EventBus glue. Entire layer **CHANGE-LIKELY** →
shared ViewModels + Compose Multiplatform.

### Dashboard — 4 tabs (`SessionsTab`)
`FOLLOWING(0)`, `MOBILE_ACTIVE(1)`, `MOBILE_DORMANT(2)`, `FIXED(3)`. Each = Fragment → list
ViewMvc → RecyclerView → session card → `SessionPresenter`, driven by a Controller + a DB
observer. Pull-to-refresh triggers backend sync. Cards reorderable.

### `SessionPresenter` — **KEEP** (the prime portable presentation state)
Pure state model (no Android imports): holds session + selected stream + thresholds +
expanded/loading + chart data. Encodes per-tab display rules, stream-selection algorithm
(saved → PM2.5 → first by type), threshold resolution, classification helpers. This becomes
the core of a shared ViewModel.

### Session detail view
Graph + map + measurement table + statistics + HLU threshold slider + notes. Observes the
session; loads full history for the selected stream, last-1 for others. Note markers on the
graph/map.

### Graph — **MPAndroidChart `v3.1.0`** (PLATFORM rendering)
Mini card chart + full-screen detail graph with pinch-zoom/pan, colored **target zones**
(threshold bands), note markers, midnight limit lines. Data shaping (`GraphDataGenerator`,
`ChartData`, averaging *(read-side only — §7)*, date formatting, note hit-testing) is **KEEP**; the `LineChart`/
`CombinedChart` rendering + gestures are **PLATFORM**.

### Map — **Google Maps** + **Places** (PLATFORM rendering)
Mobile path as a colored polyline + last-measurement marker; fixed = single marker; note
markers; a custom **grid heatmap** (`AircastingHeatmap` — averages measurements per grid
square, draws colored polygons, recomputed on zoom/pan). Grid/averaging + bounding-box math
= **KEEP**; GoogleMap rendering = **PLATFORM** iOS = Google Maps iOS SDK (same as legacy iOS).
Style: legacy res/raw/map_style / map_dark_style, picked by dark theme (ContextExtensions.setMapTypeToNormalWithStyle). Shared as explore/MapStyle.kt.

### Threshold coloring — **KEEP**
`MeasurementColor` + `Measurement.getLevel(threshold)`: 5 bands from `SensorThreshold`
(veryLow…veryHigh) → 6 `Level`s (EXTREMELY_LOW … EXTREMELY_HIGH). Return semantic level from
shared code; map level→color in the UI layer.

### Search / follow remote sessions
`SearchFollowViewModel` (real MVVM, **KEEP** logic): query active fixed sessions in a
geographic region, download session+streams+measurements, trim to last 24h, apply user
thresholds, persist locally on follow. Place search = Google Places (**PLATFORM**).

### Explore map — rewrite rules (*decided 2026-10-22*)
Replaces the legacy per-sensor `/api/fixed/active/sessions.json` search with the server-clustered
`GET /api/v3/explore/map` (`API_V3.md` → *Explore map*).
- **Fixed sessions + government stations only** — mobile sessions never appear on Explore.
- **Colours come from the user's own thresholds** (local `sensor_thresholds`), never from the server —
  the user will be able to edit them in the app, and the map must follow. The endpoint returns a bare
  `value` with no thresholds anyway. Same idea as legacy `SearchFollowViewModel.follow()`, which
  overwrote a followed session's stream thresholds with the user's saved ones
  (`setSessionThresholdsAccordingToUserSettings`). Lookup key = the station's one sensor for the
  selected type: `(source, measurement type)` → sensor name per the catalogue in `API_V3.md` →
  *Explore map*. Clusters are never coloured.
- **Temperature is Fahrenheit on the wire.** Celsius is a UI-only setting, converted at display time
  (legacy `Settings.isCelsiusScaleEnabled` + `TemperatureConverter`, which also converts the HLU
  threshold bounds). Thresholds are stored in °F.
- Server default thresholds (`GET /api/thresholds/{sensor}?unit_symbol=`, read 2026-10-22), used by
  legacy to seed `sensor_thresholds` lazily from stream payloads:
  `AirBeam-PM1`/`AirBeam-PM2.5` 0·9·35·55·150 · `Government-PM2.5` 0·12·35·55·150 ·
  `AirBeam-PM10` 0·20·50·100·200 · `Government-NO2` 0·53·100·360·649 ·
  `Government-Ozone` 0·59·75·95·115 · `AirBeam-F` 15·45·75·105·135 · `AirBeam-RH` 0·25·50·75·100.
- **Starts centred on the user**, zoom 10 (legacy `SearchLocationResultFragment.onMapReady` →
    `newLatLngZoom(location, 10f)`, used there for the searched place). Location via Home's
    `LocationProvider`, looked up once per `ExploreViewModel`. No permission / no fix → stays on the
    contiguous-US default; Explore never asks for permission itself.
- place search uses a session token per keystroke run + fetch (billing), and no lookup for a blank query

### Session actions
Per-tab action bottom sheets: edit (rename/relocate), delete (per-stream or all),
share (link or CSV file), follow/unfollow, threshold alerts. Follow/unfollow is
**client-side only** (no backend endpoint) — `SessionFollower` sets `followedAt`, copies
measurements into the active-session table, adjusts a followed count.

---

## 6. Backend API

Stack: **OkHttp + Retrofit + Gson** (→ Ktor + kotlinx.serialization, **CHANGE-LIKELY**).
Base URL default `https://aircasting.org`, overridable (URL + port) via settings; **always
force-upgraded to HTTPS** (prevents OkHttp POST→GET downgrade on 301). Base URL/port
**preserved on logout**.

Auth: HTTP **Basic** — `Base64("$authToken:X")`. Token from `GET /api/user.json`, stored in
`SharedPreferences` (plaintext — **PLATFORM**, should upgrade to Keychain/EncryptedSharedPrefs).
V2 fixed measurement upload uses a **per-session `Bearer` token** (not the user token).

Interceptors: auth-header (**KEEP**), offline short-circuit synthetic-503
(`NetworkConnectionInterceptor` — **PLATFORM**), logging.

### Endpoints (method | path | purpose)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/user.json` | Login / fetch current user (returns auth token) |
| GET | `/api/user/sessions/empty.json?uuid=[&stream_measurements=true]` | Download session (opt. with measurements, mobile) |
| GET | `/api/realtime/sync_measurements.json?uuid=&last_measurement_sync=` | Download fixed measurements since cursor |
| GET | `/api/sessions/export_by_uuid.json?email=&uuid=` | Email CSV export |
| GET | `/api/fixed/active/sessions.json?q=` | Search fixed sessions in a region |
| GET | `/api/fixed/sessions/{id}.json?sensor_name=&measurements_limit=` | Single stream of a fixed session |
| GET | `/api/fixed/sessions/{id}/streams.json?measurements_limit=` | Fixed session detail w/ streams |
| GET | `/api/fixed/threshold_alerts` | List threshold alerts |
| POST | `/api/sessions` | Create mobile session (gzip+b64 params, photos) |
| POST | `/api/realtime/sessions.json` | Create fixed session (V1) |
| POST | `/api/v3/fixed_sessions` | Create fixed session (V3 / Mini V2) — returns session_token |
| POST | `/api/realtime/measurements` | Upload fixed measurements (CSV-sync path, gzip) |
| POST | `/api/v3/fixed_sessions/{uuid}/measurements` | Upload fixed measurements (**V2 binary**, Bearer token) |
| POST | `/api/user/sessions/sync_with_versioning.json` | Version-based session sync (delete/upload/download) |
| POST | `/api/user.json` | Create account |
| POST | `/api/user/sessions/update_session.json` | Update session metadata |
| POST | `/users/password.json` | Forgot/reset password |
| POST | `/api/user/settings` | Update user settings (dormant-stream alert) |
| POST | `/api/fixed/threshold_alerts` | Create threshold alert |
| POST | `/api/user/delete_account_send_code` + `/confirm` | Two-step account deletion |
| DELETE | `/api/fixed/threshold_alerts/{id}` | Delete threshold alert |

Serialization quirks (**KEEP** but re-implement): gzip+Base64-wrapped `data`/`session`
string fields with `compression=true` sibling flag (session create, CSV-measurement upload);
plain-JSON-string `data` for sync/update; V2 binary uploads are manual big-endian ByteBuffer.
Search DTOs use `@SerializedName(alternate=…)` → kotlinx `@JsonNames`.

### ⚠️ Timestamp / timezone convention — HIGH-RISK, **KEEP exactly**
Backend stores `*_local` times and measurement times as **wall-clock numerals with a literal
`"Z"` that is NOT real UTC** (`skip_time_zone_conversion` + `TimeToLocalInUTC`). App rules:
- **Uploads:** format in the **phone-local TZ** so numerals match.
- **Downloads/parsing:** pick TZ per session — outdoor/mapped → phone-local (≈ geo TZ);
  **indoor/locationless fixed on Mini V2 (version ≥ 3 / has session_token) → UTC**; older
  devices → local numerals. `beTimeZone(isIndoor)` is the canonical helper.
- V2 binary uploads send **real Unix epoch seconds**; backend applies `to_local_as_utc`.
- Mobile sessions unaffected.
Reproduce byte-for-byte with kotlinx-datetime. See V2 guide §8a for the full detail.

---

## 7. Data Model & Persistence

Store: **Room, DB version 38**, `fallbackToDestructiveMigration()` (→ SQLDelight,
**CHANGE-LIKELY**). Domain models in `data/model` are **KEEP** (drop Android `Location`/`LatLng`
couplings, `Date`→`kotlinx.datetime`).

### Tables
| Table | Notable |
|---|---|
| `sessions` | all session fields incl. `session_token`, `fixed_pm1_index`, `fixed_pm25_index`, `measurement_interval` (v37), `averaging_frequency`, `session_order` |
| `measurement_streams` | FK→sessions CASCADE; 5 threshold cols + `deleted` |
| `measurements` | FK→streams; carries `session_id`; **unique `(session_id, measurement_stream_id, time)`** (v36); `averaging_frequency` |
| `active_sessions_measurements` | rolling live buffer (max = 10h of rows); unique `(session_id, stream_id, time)` |
| `sensor_thresholds` | global per-`sensor_name` thresholds |
| `notes` | FK→sessions CASCADE; incl. `photo_location` |
| `tracked_locations` | flat lat/long/time buffer (v38) |

Notable migrations: v34_35 (session_token + fixed indices, V2 fixed support),
v35_36 (dedup + unique measurements index), v36_37 (`measurement_interval`),
v37_38 (`tracked_locations`). Missing 29_30 & 32_33 hops rely on destructive fallback.

### Repositories — **KEEP** as interfaces (only `MeasurementsRepository` is one today)
Sessions, Measurements, MeasurementStreams, ActiveSessionMeasurements, Thresholds, Notes.
`ExpandedCardsRepository` is SharedPreferences-backed (not DB) → multiplatform-settings.

### Averaging — ⛔ **DROPPED as storage mutation; display-only in the rewrite**

> **Decision (2026-10-15).** The rewrite **never averages, down-samples, or deletes stored
> measurements.** Every measurement received from the device (live or sync) or downloaded is
> kept at native resolution, exactly as recorded. Averaging exists **only on the read side**:
> the UI (graph, map, heatmap, stats) buckets raw rows into windows when it renders.
>
> Why: the legacy in-place averaging was lossy and irreversible, needed scheduling during
> recording, and caused real data loss (the sparse-interval bug that wiped whole sessions —
> V2 guide, "Sparse-interval averaging gotcha"). Storage is cheap (1s × 5 streams × 24h ≈
> 432k rows/day — trivial for SQLite); lost data is not.
>
> Consequences for the rewrite:
> - No `AveragingService`, no averaging scheduler, no `averaging_frequency` column — the
>   recording engine just saves.
> - The **native-interval gate (old rule #4) is moot** — there's nothing destructive to guard.
>   `measurement_interval` is still stored on the session: display bucketing uses it to avoid
>   choosing a window finer than the native rate, and to tell gaps from sparse sampling.
> - Read-side averaging must be **cheap on large streams** — bucket in SQL
>   (`GROUP BY time / window`) or over a time-bounded query, not by loading a 10h session
>   into memory per frame.
> - **Uploads send raw measurements.** The backend gets full resolution (the legacy app could
>   upload already-averaged rows for long sessions). Expect more v3 upload batches
>   (mobile cap: 3000 frames/batch).
>
> The window values below remain a reasonable *display* default; the mutation described
> after them is legacy behavior, kept here for reference only.

`AveragingWindow`: `ZERO(1s)`, `FIRST(5s)`, `SECOND(60s)`. `TimeThreshold`: FIRST=2h, SECOND=9h.
*Legacy:* session > 2h → 5s averages; > 9h → 60s averages. Down-samples in place (mutates
`measurements` rows, sets `averaging_frequency`, deletes the rest). **Native-interval gate:**
if `session.measurementInterval >= window.value`, averaging is skipped (prevents wiping
sparse V2 data — the reason the v37 column exists). See V2 guide §5D.

### Thresholds
Stored globally per sensor, created lazily from a stream's own threshold fields on first
sight. Initial values come from backend payloads, or hardcoded in the V2 configurator for
Mini V2 mobile. Level banding lives in `Measurement.getLevel`.

**Rewrite (*decided 2026-10-22*):** `sensor_thresholds` is **seeded with defaults at DB creation**
(unlabeled `INSERT` in `SensorThreshold.sq` → runs in `AppDatabase.Schema.create()`), so colour
coding always has a reference point; the user later edits them (sliders → `insertOrReplaceThreshold`).
Values = server defaults (*Explore map* above); `Phone Microphone` 20·60·70·80·100 from legacy
`MicrophoneReader` (server agrees). Seeded keys = every name in `streamsFor(device)` + every Explore
`sensorNameFor` name; `DefaultThresholdsTest` fails if a new sensor lands without a default.
Existing installs only get new defaults via a `.sqm` migration.
**Open decision:** keys are exact sensor names, like legacy — a slider on `AirBeam3-PM2.5` won't
move `AirBeamMini-PM2.5` or Explore's `AirBeam-PM2.5`. Decide per-sensor vs per-family key when
building the slider screen.

---

## 8. Cross-Cutting Concerns

- **DI:** Dagger 2 (+ AssistedInject), two-tier: `AppComponent` (singleton) →
  `UserDependentComponent` (`@UserSessionScope`, created at login). → **Koin**, **CHANGE-LIKELY**.
- **Permissions** (`PermissionsManager`): location (coarse/fine/background), record-audio,
  bluetooth (scan/connect S+), camera, nearby-wifi (V2 SoftAP). **PLATFORM**.
- **Location** (`LocationHelper`): FusedLocationProvider singleton, 1s HIGH_ACCURACY, emits
  `LocationChanged`; attaches coords to measurements/notes. **PLATFORM** (iOS CoreLocation).
  Legacy lifecycle: `start()` when the new-session wizard or a map opens, `stop()` only in
  `MainActivity.onDestroy` — so after the first wizard GPS ran for the rest of the app's
  life. Background access via `ACCESS_BACKGROUND_LOCATION` ("Allow all the time").
  **Rewrite:** GPS runs only while a session records (§3 "In the rewrite"). Background
  access comes from the foreground service's `location` type
  (`foregroundServiceType="connectedDevice|location"` + `FOREGROUND_SERVICE_LOCATION`) —
  no background-location permission. The type is requested only when fine location is
  granted, because Android 14 throws otherwise. iOS: `location` in `UIBackgroundModes` plus
  `allowsBackgroundLocationUpdates` — the two must ship together, or setting the property
  crashes. Verified on a OnePlus 7T (Android 12L): fixes continued with the app in the
  background, the screen off, and the phone in forced deep Doze.
- **Connectivity** (`ConnectivityReceiver`): on-connect triggers backend sync; **suppresses
  auto-sync while a V2 SoftAP sync is in progress**. **PLATFORM**.
- **Background execution:** foreground **Services** only — **no WorkManager**. Sync is
  opportunistic (connectivity + on-resume), not scheduled. Services own the connection so
  recording survives backgrounding. **PLATFORM** (iOS = background modes / state restoration).
- **Event bus:** greenrobot EventBus is the de-facto async glue everywhere → shared
  `SharedFlow`/`Channel`. **CHANGE-LIKELY.**
- **Settings** (`Settings` over one `SharedPreferences` file): auth token, profile/email,
  onboarding flag, backend URL/port, + user toggles (dark theme, keep-screen-on, 24h format,
  Celsius, crowd-map, dormant-stream alert [also synced to backend], maps-disabled,
  satellite, mic calibration). Clean wrapper seam → multiplatform-settings. **CHANGE-LIKELY.**
- **Navigation/auth/onboarding:** imperative Activity/Fragment + Wizard navigators (no
  Jetpack nav graph). Launcher routes onboarding/login/dashboard. **CHANGE-LIKELY** → KMP nav.
- **CSV export:** `CSVHelper` (OpenCSV) builds a zipped CSV, shared via `ACTION_SEND`; plus a
  server-side email-export endpoint. Row formatting **KEEP**; file/share **PLATFORM**.
- **Notes with photos:** note model carries lat/lng + `photo_location`; camera via ImagePicker,
  EXIF rotate/compress. Model/repo/API **KEEP**; capture **PLATFORM**.
- **Microphone dB sensor:** phone mic as a sensor — 44.1kHz PCM → dB via `SignalPower` +
  `CalibrationHelper`, emits `NewMeasurementEvent` 1/s, thresholds 20/60/70/80/100. Math
  **KEEP**; `AudioRecord`/foreground service **PLATFORM** (iOS AVAudioEngine).

---

## 9. Current Third-Party Dependencies (what the rewrite replaces)

| Concern | Current | KMP target |
|---|---|---|
| BLE | Nordic `no.nordicsemi.android:ble` | `expect/actual` GATT (Nordic on Android, CoreBluetooth on iOS) or a KMP BLE lib (e.g. Kable) |
| Classic BT (AB2) | `android.bluetooth` RFCOMM | Android-only or drop |
| HTTP | OkHttp + Retrofit | Ktor (OkHttp/Darwin engines) |
| JSON | Gson | kotlinx.serialization |
| DB | Room | SQLDelight |
| DI | Dagger 2 | Koin |
| Events | greenrobot EventBus | Kotlin Flow / SharedFlow |
| Async | kotlinx-coroutines | same (shared) |
| Charts | MPAndroidChart | per-platform (Compose chart / Swift Charts) |
| Maps | Google Maps + Places | per-platform (Maps SDK / MapKit) |
| Reactive UI | LiveData | StateFlow |
| UI | XML + ViewMvc/MVC (+ kotlinx.synthetic, deprecated) | Compose Multiplatform + shared ViewModels |
| CSV | OpenCSV | shared CSV writer |
| Images | Glide, ImagePicker | per-platform |
| Prefs | SharedPreferences | multiplatform-settings |

---

## 10. Load-Bearing Rules (do not lose these in the rewrite)

1. **Timestamp wall-clock-as-UTC convention** (§6) — indoor-Mini-V2 → UTC else local; V2
   binary → epoch. Getting this wrong offsets every graph. (V2 guide §8a.)
2. **HTTPS force-upgrade + port 80→443** in base-URL construction.
3. **V2 sync UUID validation** — discard sync chunks whose device session UUID ≠ app session.
4. **Raw measurements are never averaged, down-sampled, or deleted in storage** — averaging is
   display-only (§7). *Supersedes the legacy "averaging native-interval gate" rule, which only
   existed to limit damage from in-place averaging.*
5. **Measurements dedup on `(stream, time)`** — the V2 dedup strategy, and the backend's own v3
   rule. Legacy index was `(session_id, stream_id, time)`; the rewrite uses
   `PRIMARY KEY (stream_id, time)` + `INSERT OR IGNORE` (`session_id` was redundant). There is
   no separate active-session buffer table — one `measurements` table, one write path.
6. **Backend URL/port preserved on logout.**
7. **Two measurement "drain" flags for V2 finish-while-syncing** (V2 guide §9).
8. **V2→V1 fallback disconnect-path guards** — keep the service/EventBus alive across the
   failed V2 leg (V2 guide §9, Nordic disconnect hazard).
9. **Mobile measurements are stamped with a location at insert, falling back to the last
   known location** (§3 "Measurement location stamping"). A stored mobile measurement never
   has a null location.
10. **The app uploads mobile measurements only.** Fixed-session measurements are uploaded by
    the AirBeam itself (with `session_token`); the app downloads them. Single exception:
    **AB3 SD-card sync** can upload fixed measurements read off the card
    (legacy `SDCardUploadFixedMeasurementsService`).
