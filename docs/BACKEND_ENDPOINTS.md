# AirCasting — Backend Endpoints (Home-screen focus)

> **Purpose.** Concrete backend endpoint + payload reference, mined from the **legacy Android
> app** (`/Users/kirillgetmanskii_1/AndroidStudioProjects/AircastingAndroid`, `data/api/`). `DOMAIN_KNOWLEDGE.md` §6 lists the full
> endpoint table at a high level; this doc drills into the **exact request/response shapes**
> for the endpoints the **redesigned Home screen** needs, so we can build DTOs + a repository
> without guessing.
>
> **Captured:** 2026-07-20 from legacy `ApiService.kt`, `ApiConstants.kt`,
> `ActiveFixedSessionsInRegionRepository.kt`, and the `data/api/response/search/*` DTOs.
> **Companion:** [`HOME_SCREEN.md`](./design/HOME_SCREEN.md) (design) · [`DOMAIN_KNOWLEDGE.md`](./DOMAIN_KNOWLEDGE.md) §6 (full API).

---

## Base + auth
- **Base URL:** `https://aircasting.org` (overridable URL+port via settings; force-HTTPS).
- **Auth:** HTTP **Basic** `Base64("$authToken:X")` for user endpoints. The region-search +
  fixed-session-detail endpoints Home uses are **`@NonAuthenticated`** (no token needed).
- Legacy stack: OkHttp+Retrofit+Gson → **KMP target: Ktor + kotlinx.serialization.**

---

## The 2 endpoints Home actually needs

Everything on the redesigned Home (current AQ, nearby stations, weekly chart) is built from
**fixed government/community stations**, via just two GET endpoints.

### A. Active fixed sessions in a region — nearby stations + current reading
```
GET /api/fixed/active/sessions.json?q={jsonString}
```
- **`q` is a JSON string** (hand-built in legacy; build via kotlinx.serialization in KMP):
  ```json
  {
    "time_from": "<epoch: start of today, 1 year ago>",
    "time_to":   "<epoch: end of today>",
    "tags": "", "usernames": "",
    "west": <lng>, "east": <lng>, "south": <lat>, "north": <lat>,   // bounding box
    "sensor_name": "government-pm2.5",          // ONE sensor per call (see sensor table)
    "unit_symbol": "µg/m³",
    "measurement_type": "Particulate Matter"
  }
  ```
- **One call = one sensor.** To fill the Figma AQ card's 3 chips (PM2.5 / NO₂ / Ozone) you make
  **3 calls** (one sensor triple each) and merge — legacy does the same pattern (it merges
  AB2+AB3 responses via `combineResponses`).
- **Response** `SessionsInRegionsResponse`:
  ```
  fetchableSessionsCount: Int
  sessions: [ SessionInRegionResponse {
      id: Long, uuid: String, title: String,          // title = station name
      type: String, username: String,
      latitude: Double, longitude: Double,             // → client-side distance (haversine)
      is_indoor: Boolean,
      last_hour_average: Double,                       // ← the headline current value
      start_time_local: String, end_time_local: String,
      streams: { sensor: Sensor { …thresholds…, last_measurement_value, measurements? } }
  } ]
  ```
- **Home uses this for BOTH:**
  - **Nearby stations carousel** — the full `sessions` list (each → a `StationCard`).
  - **Current AQ card** — the **nearest** session (min haversine from device location) →
    headline value = `last_hour_average`; station name = `title`; distance computed client-side.

### B. Fixed session detail w/ streams + measurements — weekly exposure chart
```
GET /api/fixed/sessions/{sessionID}/streams.json?measurements_limit=N
```
- `measurements_limit`: legacy default `MEASUREMENTS_IN_HOUR * 24` (1 day). For the **7-day
  weekly chart** request ~`24 * 7` hourly points (confirm the API returns hourly, not raw).
- **Response** `SessionWithStreamsAndMeasurementsResponse` → per-stream `Stream`:
  ```
  sensor_name, sensor_unit, unit_name, measurement_type, measurement_short_type,
  last_measurement_value: Double,
  threshold_very_low, threshold_low, threshold_medium, threshold_high, threshold_very_high: Int,
  measurements: [ { value: Double, time: Long(epoch), latitude, longitude } ],
  min/max_latitude, min/max_longitude, stream_id
  ```
- **Home uses this for:** the weekly bars (`measurements` → daily buckets), avg / peak / good-days
  (client-side aggregation), and AQI banding (the 5 `threshold_*` values → `AqiLevel`).
- Sibling endpoint `GET /api/fixed/sessions/{sessionID}.json` (`getStreamOfGivenSession`,
  `sensor_name` + `measurements_limit`) returns a **single** stream — cheaper when only one
  pollutant series is needed.

---

## Sensor identity table (the `sensor_name` / `unit` / `type` triples)

Fixed-station queries key off these exact strings (legacy `StringConstants` + `SensorInformation`):

| Pollutant | source | `sensor_name` | `measurement_type` | `unit_symbol` |
|-----------|--------|---------------|--------------------|---------------|
| PM2.5 | government | `government-pm2.5` | `Particulate Matter` | `µg/m³` |
| PM2.5 | AirBeam | `airbeam-pm2.5` | `Particulate Matter` | `µg/m³` |
| NO₂ | government | `government-no2` | `Nitrogen Dioxide` | `ppb` |
| Ozone | government | `government-ozone` | `Ozone` | `ppb` |

- Matches the Figma AQ chips exactly: **PM 2.5 µg/m³**, **NO₂ ppb**, **Ozone ppb**.
- Response `Sensor.sensor_name` has Gson `alternate` aliases (OpenAQ / PurpleAir / AB2 / AB3
  names) → in kotlinx.serialization use `@JsonNames`.
- The **"GOV MONITOR" tag** in Figma = `sensor_name` starts with `government-`; **AirBeam** source
  = `airbeam-…`.

---

## AQI level / color — client-side, from thresholds

There is **no backend "Good/Moderate" status field.** The Figma status ("Good", green) and bar
colors are derived on-device:
- 5 thresholds per stream (`threshold_very_low … threshold_very_high`) band a value into a
  semantic `AqiLevel` (`DOMAIN_KNOWLEDGE.md` §5 "threshold coloring", `Measurement.getLevel`).
- **Shared code returns the level; the Compose theme maps level → color** (green `#006E02`, etc.).

---

## Full endpoint catalog (legacy → KMP)

Path constants from `ApiConstants.kt`. **Bold** = Home-relevant.

| Method | Path | Purpose |
|---|---|---|
| **GET** | **`/api/fixed/active/sessions.json?q=`** | **Active fixed sessions in region (nearby + current)** |
| **GET** | **`/api/fixed/sessions/{id}/streams.json?measurements_limit=`** | **Fixed session detail w/ streams+measurements (weekly chart)** |
| GET | `/api/fixed/sessions/{id}.json?sensor_name=&measurements_limit=` | Single stream of a fixed session |
| GET | `/api/user.json` | Login / current user (auth token) |
| GET | `/api/user/sessions/empty.json?uuid=[&stream_measurements=]` | Download a session (opt. measurements) |
| GET | `/api/realtime/sync_measurements.json?uuid=&last_measurement_sync=` | Download fixed measurements since cursor |
| GET | `/api/sessions/export_by_uuid.json?email=&uuid=` | Email CSV export |
| GET | `/api/fixed/threshold_alerts` | List threshold alerts |
| POST | `/api/sessions` | Create mobile session (gzip+b64) |
| POST | `/api/realtime/sessions.json` | Create fixed session (V1) |
| POST | `/api/v3/fixed_sessions` | Create fixed session (V2/V3) → session_token |
| POST | `/api/user/sessions/sync_with_versioning.json` | Version-based session sync |
| POST | `/api/realtime/measurements` | Upload fixed measurements (CSV path, gzip) |
| POST | `/api/v3/fixed_sessions/{uuid}/measurements` | Upload fixed measurements (V2 binary, Bearer) |
| POST | `/api/user.json` | Create account |
| POST | `/api/user/sessions/update_session.json` | Update session metadata |
| POST | `/users/password.json` | Reset password |
| POST | `/api/user/settings` | Update user settings |
| POST | `/api/fixed/threshold_alerts` | Create threshold alert |
| POST | `/api/user/delete_account_send_code` · `/confirm` | Two-step account deletion |
| DELETE | `/api/fixed/threshold_alerts/{id}` | Delete threshold alert |

---

## Home data map (field → source) + gaps

| Home need | Endpoint | Notes / client-side work |
|---|---|---|
| Current AQ headline value + level | A (nearest session, `last_hour_average` + `sensor` thresholds) | pick nearest by haversine; band value → `AqiLevel` |
| AQ metric chips (PM2.5/NO₂/Ozone) | A × **3 calls** (one per `sensor_name`) | merge by nearest station |
| AQ station name + distance | A (`title`, `latitude/longitude`) | distance = haversine(device, station), formatted in UI |
| Nearby stations carousel | A (`sessions` list) | each → `StationCard`; source tag from `sensor_name` prefix |
| Station "updated X ago" | A (`end_time_local`) — ⚠️ verify freshness field | relative-time formatting in UI (state carries `Instant`) |
| Weekly bars + avg/peak/good-days | B (`measurements`) | bucket per day, aggregate client-side |
| Weekly AQI banding / bar colors | B (`threshold_*`) | level → color in theme |
| **Location services on/off** | — none (PLATFORM) | `LocationProvider` seam (arch §5), drives `NoLocationServices` |
| **"Learn more" content cards** | — **none** | NEW endpoint or static/CMS; hardcode for now |

### Gaps / decisions for the redesign
1. **No single "nearest-station snapshot" endpoint.** Home's AQ card is assembled from **3+
   region calls + client-side nearest/haversine/banding**. A new backend endpoint (one call →
   nearest station + all pollutants + level) would simplify Home a lot — flag for backend team.
2. **`q` is a brittle hand-built JSON string** in legacy. In KMP, model it as a `@Serializable`
   request object.
3. **Bounding box, not radius.** "Nearby" = a lat/lng square around the device; there's no
   distance/radius param. Home must build the box (and the "0.4 mile away" label is client-side).
4. **No "Learn more" API.** Confirm whether this is CMS-driven or static content.
5. **Weekly granularity unconfirmed** — verify `/streams.json` returns hourly averages over 7
   days (vs raw); may need a dedicated summary endpoint for avg/peak/good-days.
