# AirCasting — Kotlin Multiplatform Architecture Plan

> **Purpose.** The target architecture for rewriting AirCasting as a Kotlin Multiplatform
> (Android + iOS) app. This is a **general architecture / scaffolding plan** — not a
> line-by-line migration. Designs may still change; the goal now is a *modular skeleton* the
> team can build features into.
>
> **Derived from** [`DOMAIN_KNOWLEDGE.md`](./DOMAIN_KNOWLEDGE.md) (what the app does) — read
> that first. The seams below come straight from the KEEP/PLATFORM/CHANGE-LIKELY tags there.
>
> **Non-goal.** Designs and some features may change (no designs yet). This doc fixes the
> *boundaries*, not the pixels. Core functionality must stay: connect AB2 / AB3 / Mini V1 /
> Mini V2, record mobile + fixed sessions, display them.

---

## 1. Guiding Principles

1. **Maximize `commonMain`.** Domain models, state machines, parsing, averaging, thresholds,
   sync orchestration, repositories (as interfaces), ViewModels, API DTOs — all shared. The
   knowledge base shows most business logic is already platform-agnostic and merely
   *entangled* with Android APIs, not dependent on them.
2. **One driver seam per hard boundary.** Device (BLE/BT), persistence, networking, location,
   background execution, secure storage each get a single interface in `commonMain` with
   `actual` implementations per platform. Nothing else touches platform APIs.
3. **Flows over EventBus.** The greenrobot EventBus is the biggest structural debt. Replace
   it with explicit `Flow`/`SharedFlow` streams and suspend calls. This alone clarifies the
   recording/measurement pipeline (which currently has 3 divergent paths).
4. **Unidirectional data flow.** DB is the single source of truth; ViewModels expose
   `StateFlow`; UI is a pure function of state. Compose Multiplatform on both platforms
   (or shared VM + native UI if CMP-on-iOS is deferred — decide early, see §7).
5. **Backward-compatible behavior, not code.** The V1 and V2 firmware paths, both session
   types, and the load-bearing rules (`DOMAIN_KNOWLEDGE.md` §10) must all survive. The *code*
   is rewritten; the *behavior* is preserved and verified against those rules.

---

## 2. Module Decomposition

```
:core            kotlinx-datetime, coroutines setup, Result types, logging, expect utils
:domain          pure models + enums + state machines + business rules   (NO platform deps)
                 └ Session/Measurement/Stream/Threshold/Note, Level banding,
                   averaging algorithm, recording state machine, threshold color levels
:data:db         SQLDelight schema (.sq) + queries + column adapters      (KEEP + PLATFORM driver)
:data:network    Ktor client + kotlinx.serialization DTOs + API service   (KEEP + PLATFORM engine)
:data:settings   multiplatform-settings wrapper (auth token, prefs)       (PLATFORM secure storage)
:data:repo       repository impls over db+network (interfaces in :domain)
:ble             SensorDriver interface + 4 device drivers                (KEEP protocol + PLATFORM GATT)
:recording       shared recording engine (state machine over driver+repo) (KEEP)
:sync            sync orchestrators per device/transport                  (KEEP + PLATFORM)
:location        LocationProvider interface                               (PLATFORM)
:presentation    shared ViewModels (StateFlow), presentation state        (KEEP)
:app-android     Android entry, Compose UI, actual drivers, foreground services
:app-ios         iOS entry, SwiftUI/CMP, actual drivers, background modes
```

Dependency direction (arrows point to dependencies):
```
app-* ──▶ presentation ──▶ recording ──▶ ble
                    │            │          │
                    └──────▶ data:repo ◀────┘
                                 │
                    ┌────────────┼────────────┐
                 data:db     data:network   data:settings
                    └────────────┴────────────┘
                                 ▼
                              domain ──▶ core
```
`:domain` and `:core` depend on nothing platform-specific. Everything else layers on top.

---

## 3. The Device Seam (the modular crux)

This is what makes AB2 / AB3 / Mini V1 / Mini V2 pluggable — the whole point of the rewrite.
Today it's split across `AirBeamConnector` (lifecycle) + `AirBeamBleConfigurator` (driver) +
Nordic `BleManager` inheritance. Invert that: **protocol logic sits above a thin transport.**

```kotlin
// :ble commonMain — one interface, four implementations
interface SensorDriver {
    val deviceType: DeviceType
    fun connect(device: DeviceDescriptor): Flow<ConnectionState>
    suspend fun sendAuth(config: AuthConfig)                 // no-op for V2
    suspend fun configureSession(config: SessionConfig)
    val measurements: Flow<RawMeasurement>                   // live stream
    fun sync(request: SyncRequest): Flow<SyncProgress>       // historical/manual
    suspend fun setTime(epoch: Long)
    suspend fun discardSession()
    fun disconnect()
}
```

- **Implementations** (in `:ble`, `commonMain` where possible):
  - `AirBeam2Driver` — classic SPP. **Android-only** (`actual` stub / unsupported on iOS).
  - `AirBeamV1BleDriver` — AB3 + Mini V1 (ASCII-hex protocol; `HexMessagesBuilder` +
    `ResponseParser` move to `commonMain` verbatim minus `android.util.Base64`).
  - `AirBeamMiniV2Driver` — binary LE (parse/build logic to `commonMain`; ByteBuffer →
    `kotlinx-io`/okio).
- **Transport is the only `expect/actual`:**
  ```kotlin
  expect class GattTransport {          // Android: Nordic/Kable · iOS: CoreBluetooth/Kable
      fun connect(id: String): Flow<GattEvent>
      suspend fun write(char: Uuid, bytes: ByteArray)
      fun observe(char: Uuid): Flow<ByteArray>   // notify + indicate
      suspend fun requestMtu(mtu: Int)
  }
  expect class BleScanner {             // service-UUID-filtered scan (fixes today's gap)
      fun scan(serviceUuids: List<Uuid>): Flow<ScanResult>
  }
  ```
- **Factory** routes by device type + scanned service UUID (replaces
  `AirBeamConnectorFactory` + the brittle `AirBeamMiniFallbackConnector`): scan with a
  service-UUID filter, so Mini V1 vs V2 is known *before* connect — no connect-then-fallback.
- **Consider [Kable](https://github.com/JuulLabs/kable)** (KMP BLE) to get `GattTransport` +
  `BleScanner` for free on both platforms, instead of hand-writing `expect/actual` GATT.

**AB2 decision required (§7):** SPP has no iOS path. Keep it behind an Android-only driver
that returns `DeviceType.unsupported` on iOS, or drop AB2 from the rewrite.

---

## 4. Recording Engine (biggest structural win)

Today recording logic is scattered across `RecordingHandlerImpl`, `AirBeamConnector`,
`SessionManager`, foreground services, and EventBus — with **three divergent measurement
paths** (V1-observer, V2-direct-DB, fixed-download). Collapse to **one shared engine**:

```kotlin
// :recording commonMain
class RecordingEngine(
    private val driver: SensorDriver,
    private val sessions: SessionRepository,
    private val measurements: MeasurementRepository,
    private val averaging: AveragingService,
    private val scope: CoroutineScope,
) {
    val state: StateFlow<RecordingState>            // NEW→RECORDING→DISCONNECTED→FINISHED
    suspend fun start(config: SessionConfig)
    suspend fun stop()
    // driver.measurements + driver.sync(...) both funnel into one save pipeline
}
```

- **Unify the measurement pipeline:** live + sync + (for fixed) downloaded measurements all
  flow through one `saveMeasurements()` path with the dedup rule (`INSERT … IGNORE` on the
  unique index). No more V1-vs-V2 fork.
- **State machine** is `Session.Status` transitions, pure `commonMain`.
- **Averaging** (`AveragingService` + windows) ports as-is — pure algorithm; inject a
  dispatcher instead of `Dispatchers.Default`; keep the native-interval gate.
- **Background execution** is the one platform escape hatch: recording must survive
  backgrounding. `expect` a `BackgroundSessionHost` — Android = foreground service
  (`FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`), iOS = Core Bluetooth background mode +
  state restoration. The engine itself stays platform-neutral; the host just keeps the
  process alive and holds the engine's scope.
- **Fixed sessions** don't record locally — the engine for fixed just creates the backend
  session, configures the device, and hands off to the download service (§5).

---

## 5. Data Layer

### Persistence — SQLDelight (`:data:db`)
- Port the Room schema (v38) to `.sq` files. Preserve: the unique measurements index
  (`session_id, stream_id, time`), `measurement_interval`, `session_token` + fixed indices,
  the active-measurements rolling window (>998-row guard → `transaction {}`).
- Column adapters for `Date`→`Instant`, tags list, `Session.Status/Type`, `DeviceType`.
- `expect` the driver: `AndroidSqliteDriver` / `NativeSqliteDriver`.
- DAOs → SQLDelight queries returning `Flow` (`.asFlow().mapToList()`). The
  `SessionWith*DBObject` relation projections become explicit join queries.
- Migrations → `.sqm`. Decide whether to carry the old Room migration history or start the
  KMP DB fresh at a new baseline (recommended: fresh baseline = current schema, since the
  rewrite is a new install target — but confirm the upgrade story for existing users).

### Networking — Ktor + kotlinx.serialization (`:data:network`)
- One `HttpClient` (OkHttp engine Android, Darwin iOS) with `ContentNegotiation(json)`,
  `Logging` (level gated on debug), and an auth plugin injecting the Basic header.
- Port every DTO to `@Serializable` (`@SerialName`, `@JsonNames` for the `alternate` cases).
- **Preserve the HTTPS force-upgrade** and base-URL/port construction logic.
- **gzip+Base64 params:** `expect` the gzip/base64 (okio has both, may not need expect).
- **V2 binary upload:** ByteBuffer → `kotlinx-io` `Buffer`; `Bearer` per-session token.
- **Offline handling:** a Ktor plugin / connectivity `expect` replacing the synthetic-503
  interceptor.

### Timestamp handling — the highest-risk port
Reproduce the wall-clock-as-UTC convention (`DOMAIN_KNOWLEDGE.md` §6, V2 guide §8a) with
`kotlinx-datetime`: `beTimeZone(isIndoor)` → `TimeZone.currentSystemDefault()` vs
`TimeZone.UTC`; V2 binary → raw epoch seconds. **Write round-trip tests first** — this is the
easiest thing to silently break.

### Settings — multiplatform-settings (`:data:settings`)
Keep the `Settings` wrapper API; swap the backing store. **Upgrade auth token to secure
storage** (`expect`: EncryptedSharedPreferences/Keystore on Android, Keychain on iOS) — it's
plaintext today. Preserve backend URL/port on logout.

### Repositories (`:data:repo`)
Interfaces in `:domain`, impls here. Extract interfaces for all current repos (only
`MeasurementsRepository` is one today). Business rules (`getLevel`, threshold find-or-create,
rolling-window sizing, status transitions) move into `:domain`.

---

## 6. Presentation

- **Shared ViewModels** in `:presentation` exposing `StateFlow<UiState>` + intent functions.
  Anchor on the already-portable pieces: `SessionPresenter` (per-tab display state + stream
  selection), `SessionsObserver` diff logic, `SearchFollowViewModel`, chart/heatmap data
  shaping, `MeasurementColor` level math.
- `LiveData` → `StateFlow`; EventBus → collected `Flow`s.
- **Charts & maps stay platform-native** — share the *data* (entries, target zones, heatmap
  grid, bounding box), render per platform (Compose chart lib / Swift Charts;
  Maps SDK / MapKit). Return semantic threshold **levels** from shared code; resolve colors
  in the UI.
- Navigation: on iOS, navigation is **native** (SwiftUI `NavigationStack`/`TabView`) — see §6.1.
  On Android, Compose navigation (Navigation-Compose or Decompose). Nav is the one UI layer
  that is *not* shared under the chosen strategy.

### 6.1 iOS UI Strategy — RESOLVED

**Decision: native SwiftUI *shell* + shared Compose Multiplatform screens/components, with
SKIE used only where needed.** Not full-native SwiftUI, and not CMP-everywhere-with-its-own-nav.

Structure:
```
SwiftUI shell (iOS)              ← TabView / NavigationStack — nav chrome only, holds NO domain data
  └─ ComposeUIViewController { }  ← each screen is shared Compose (M3), reads its own shared VM
```

**Why this fits the team** (experienced Android/Compose devs, new to KMP, minimal SwiftUI):
1. **Skill match.** SwiftUI surface is confined to the shell (`TabView`, `NavigationStack`,
   `UIViewControllerRepresentable`, a nav coordinator). Small, bounded, learnable — a good
   SwiftUI on-ramp without betting the app on Swift. Screens stay in Compose (team strength).
2. **Write UI once.** Full-native would duplicate every screen (Compose + SwiftUI) forever.
   Shell approach: UI written once in Compose, shell written once. One place to fix bugs.
3. **Minimal interop.** Full-native routes *every* screen's VM state across the Kotlin↔Swift
   boundary → pervasive SKIE. The shell approach crosses far less (see data flow below).
4. **Escape hatch.** The shell already hosts native. If one screen's CMP feel/perf is
   unacceptable, rewrite *that screen* in SwiftUI later — localized, not pre-paid app-wide.
5. **Learning goal.** The shell is small enough for the team to *write and understand* the
   SwiftUI themselves. Principle: **learn the shell, do not outsource it to a code agent** —
   an agent-written native UI is an iOS half the team can't fluently debug, review, or
   maintain, which defeats the point of the rewrite.

**Data flow — one thin seam, and it carries navigation, not data:**
```
Shared VM (Kotlin) ──StateFlow<UiState>──▶ Compose screen (Kotlin)   [same language, no bridge]
       ▲                                          │
       └──────── intent fns (Kotlin call) ────────┘
                          ▲
                          │  ONLY route objects cross Kotlin↔Swift here
                          ▼
                 SwiftUI shell (tab + nav path; knows zero domain data)
```
- Domain data is **Kotlin → Compose**, never touches Swift. No double translation.
- The shell hears only: "navigate to route X", "go back", "tab changed". Routes are
  id-carrying enums made `Hashable`/`Identifiable`.
- **Feed native chrome (nav-bar title/subtitle) from route metadata, not live VM state** — so
  Swift stays data-free.
- **SKIE** (preferred over KMP-NativeCoroutines: zero-annotation, and it bridges our many
  sealed/enum states to exhaustive Swift `switch`) is needed **only** if a *live* value must
  feed native chrome (e.g. live AQI in a glass toolbar). Otherwise it is not required.

**Liquid Glass (iOS 26) — deferred, two viable paths when we want it:**
- **Native shell path:** native `TabView`/`NavigationStack` apply real system Liquid Glass
  for free (iOS 26 only; fallback to full-Compose nav below 26). Cost: a navigation-bridge
  (forward Compose `NavHost` routes to SwiftUI via callbacks; hide Compose chrome via a
  `LocalUseNativeNavigation` CompositionLocal). Glass on iOS only; Android stays flat.
- **Shared-shader path** (e.g. `Kyant0/AndroidLiquidGlass`, `kmp` branch — AGSL on Android,
  SkSL/Skia on iOS): real refraction/highlights, **glass on both platforms**, one codebase,
  no native nav split. Trade-offs: no fluid merge/morph animation, self-owned accessibility
  (Reduce Transparency/Motion), GPU cost, you build the components. ⚠️ **Perf caveat:**
  benchmark a shader'd nav/tab bar *during an active recording session on the oldest target
  device* (BLE + 1Hz GPS + live charts already running) before committing.
- **Revisit trigger:** only pursue real Liquid Glass when it becomes a product requirement.
  Default UI is plain M3 on both platforms until then.

---

## 7. Decisions to Make Before Building (open questions)

These change the skeleton — resolve early:

1. ~~**UI strategy.**~~ **RESOLVED — see §6.1:** native SwiftUI shell + shared Compose
   screens + SKIE-when-needed; iOS nav native, Android nav Compose; Liquid Glass deferred.
2. **AB2 fate:** keep as Android-only, or drop? (No iOS SPP path.)
3. **BLE library:** hand-rolled `expect/actual` GATT, or adopt Kable (KMP BLE)?
4. **DB migration:** carry Room migration history, or fresh SQLDelight baseline at current
   schema? (Depends on whether existing installs upgrade in place.)
5. **iOS background recording:** confirm Core Bluetooth background mode + state restoration
   covers the "record while backgrounded" requirement the way Android foreground services do.
6. **DI:** Koin (recommended, mature KMP) vs manual DI.

---

## 8. Suggested Bring-Up Order

Vertical slices, each provable end-to-end, lowest-risk first:

1. **Foundation:** `:core` + `:domain` (models, enums, state machine, averaging, thresholds)
   with unit tests. No platform code — proves the shared logic compiles KMP.
2. **Persistence:** `:data:db` (SQLDelight schema + queries) on both platforms.
3. **Networking:** `:data:network` (Ktor + DTOs) + **timestamp round-trip tests** + auth/login.
4. **One device, one session:** `:ble` `GattTransport` + `AirBeamMiniV2Driver` + `:recording`
   engine → record a **V2 mobile** session end-to-end (the newest, cleanest protocol first).
5. **Broaden devices:** add `AirBeamV1BleDriver` (AB3 + Mini V1), then AB2 (if kept).
6. **Fixed sessions + sync:** backend session creation, download service, the sync
   orchestrators.
7. **Presentation:** ViewModels + dashboard/detail/graph/map on each platform.
8. **Cross-cutting:** location, connectivity-driven sync, notes/photos, mic sensor, settings,
   onboarding/auth, CSV export.

Verify each slice against the load-bearing rules in `DOMAIN_KNOWLEDGE.md` §10.

---

## 9. Traceability

Every claim here traces to the domain knowledge base and the V2 guide:
- Device/protocol facts → `DOMAIN_KNOWLEDGE.md` §1, `.claude/ble_mobile_app_guide.md`.
- Recording/measurement paths → §2–3.
- Sync flows → §4.
- Display seams → §5.
- Backend/timestamps → §6.
- Persistence/averaging → §7.
- Cross-cutting → §8.
- Load-bearing rules to preserve → §10.

Keep both docs updated as designs firm up and features change.
