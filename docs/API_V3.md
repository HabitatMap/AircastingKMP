# AirCasting — `/api/v3` (current API surface)

> **Source of truth:** <https://experimental.aircasting.org/api-docs/index.html> — the OpenAPI spec
> ("AirCasting Mobile & Web API"), **ahead of prod** <https://aircasting.org/api-docs/index.html>.
> The Swagger UI is JS-rendered, so fetch the raw spec instead:
> `curl -sL https://experimental.aircasting.org/api-docs/swagger.yaml`
> (rswag; source in GitHub `HabitatMap/AirCasting` at `swagger/swagger.yaml`).
>
> **Captured:** 2026-10-15 from the experimental spec. When this doc and the spec disagree, the spec wins.
>
> **Companion:** [`BACKEND_ENDPOINTS.md`](./BACKEND_ENDPOINTS.md) — legacy endpoints mined from the old
> Android app; most of it is now the **deprecated** pre-v3 surface.

---

## Read this first

The spec says it outright:

> Tags marked **[DEPRECATED]** are served only for already-released mobile builds — **build new
> clients against the `/api/v3` endpoints.**

So: **this app targets `/api/v3`.** The pre-v3 `/api/...` paths in `BACKEND_ENDPOINTS.md`
(`/api/realtime/*`, `/api/sessions`, `/api/user/sessions/*`, `/api/fixed/sessions/*`) exist only to keep
shipped builds alive.

Endpoints are tagged **by client** — `Mobile app:` (iOS/Android) vs `Web app:` (web frontend).
**No endpoint is used by both.** We build against the `Mobile app:` tags; the `Web app:` v3 reads are
listed below only so we recognise them when scanning the spec.

### Auth

| Scheme | Header | Used by |
|---|---|---|
| `bearer_auth` | `Authorization: Bearer <user_token>` | **all `/api/v3`** — use this |
| `basic_auth` | `Authorization: Basic base64("<user_token>:X")` | legacy pre-v3 `/api` only |

`<user_token>` comes from sign-in (`GET /api/user.json`). Note the **session token** returned by session
creation is a *different* token, handed to the AirBeam over BLE for its own uploads.

### ⚠️ ALPHA

Everything below marked **[A]** is flagged `[ALPHA]` in the spec — the shape can still move.
Re-read the spec before building against one.

---

## Mobile sessions — tag `Mobile app: Mobile sessions`

AirBeam mobile (moving) sessions.

| Method | Path | Query / body | |
|---|---|---|---|
| GET | `/api/v3/mobile_sessions` | `page`, `per_page` | list own sessions **[A]** |
| POST | `/api/v3/mobile_sessions` | JSON body (below) | create |
| GET | `/api/v3/mobile_sessions/{uuid}` | — | show **[A]** |
| PATCH | `/api/v3/mobile_sessions/{uuid}` | partial JSON; empty `tag_list` clears tags | update **[A]** |
| DELETE | `/api/v3/mobile_sessions/{uuid}` | — | delete **[A]** |
| POST | `/api/v3/mobile_sessions/{uuid}/finish` | — | stamps `finished_at` **[A]** |
| GET | `/api/v3/mobile_sessions/{uuid}/measurements` | `sensor_name` (req), `start_time`, `end_time` | read **[A]** |
| POST | `/api/v3/mobile_sessions/{uuid}/measurements` | **binary** (below) | upload **[A]** |
| GET · POST | `/api/v3/mobile_sessions/{uuid}/notes` | — · JSON | list · add **[A]** |
| PATCH · DELETE | `/api/v3/mobile_sessions/{uuid}/notes/{id}` | JSON · — | edit · delete **[A]** |

**Create body** — required: `uuid` (canonical UUID), `title`, `time_zone` (IANA), `contribute` (no server
default, send it), `device { mac_address, model, name? }`, `streams[]` (min 1).
Optional: `tag_list`, `latitude`, `longitude`.

A stream is `{ sensor_name, unit_symbol }` for **known sensors** (AirBeam PM1/PM2.5/PM10/RH/F and
`Phone Microphone`). A **custom sensor** must also send `measurement_type`, `measurement_short_type`,
`unit_name` and `thresholds { very_low, low, medium, high, very_high }` (ascending); its fields cap at 64
chars and it may not reuse a built-in `sensor_name`.

Each sensor **type** may appear once per session — `AirBeamMini-PM2.5` and `AirBeam2-PM2.5` are the same
type (`AirBeam-PM2.5`). Built-in `sensor_type_id`s are **1–99**, globally stable; custom sensors get
**100–255**, unique within the session.

Call create once at the start of a recording, then stream measurements to `.../measurements`.

---

## Fixed sessions — tag `Mobile app: Fixed sessions`

AirBeam fixed (roof-mounted) sessions — the AirBeamMini path this project is building.

| Method | Path | Query / body | |
|---|---|---|---|
| GET | `/api/v3/fixed_sessions` | `page`, `per_page` | list own sessions **[A]** |
| POST | `/api/v3/fixed_sessions` | JSON body (below) | **create AirBeamMini fixed session** |
| GET | `/api/v3/fixed_sessions/{uuid}` | — | show **[A]** |
| PATCH | `/api/v3/fixed_sessions/{uuid}` | partial JSON; empty `tag_list` clears tags | update **[A]** |
| DELETE | `/api/v3/fixed_sessions/{uuid}` | — | delete **[A]** |
| POST | `/api/v3/fixed_sessions/{uuid}/finish` | — | decommission, stamps `finished_at` **[A]** |
| GET | `/api/v3/fixed_sessions/{uuid}/measurements` | `sensor_name` (req), `start_time`, `end_time` | read **[A]** |
| POST | `/api/v3/fixed_sessions/{uuid}/measurements` | **binary** (below) | upload |

**Create body** — required: `uuid`, `title`, `latitude`, `longitude`, `contribute`,
`device { mac_address, model, name? }`, `streams[] { sensor_name, unit_symbol }` (min 1).
Optional: `is_indoor` (default false), `time_zone` (IANA — **required for indoor sessions**, which send
placeholder coords `200, 200`; otherwise derived from lat/lng).

- `device.mac_address` is "any stable device id — not necessarily a hardware MAC".
- Shipped app versions may still send the device object under the key `airbeam`.

**`201` response:**

| Field | Meaning |
|---|---|
| `session_token` | Bearer token **for the AirBeam's own measurement uploads** — the app passes it to the device over BLE |
| `streams[].sensor_type_id` | compact numeric id the device puts in each binary frame |
| `streams[].sensor_name` | e.g. `AirBeam-PM2.5` (canonical type, not the `AirBeamMini-…` request name) |
| `share_url` | `<host>/s/<token>` — **append `?sensor_name=<stream>` before sharing**, the link only resolves with it |
| `location` | legacy alias of `share_url`, kept indefinitely for shipped builds — new clients use `share_url` |

Creating the same `uuid` **concurrently** yields one session and both callers get it; a `uuid` that
already existed is `session_uuid_taken` (409) — stop retrying, continue with the existing session.

---

## Binary measurement upload

Both `POST .../measurements` endpoints take a raw binary body. Same envelope, **different frame size**.

```
0          2     uint8[2]     Magic bytes: 0xAB 0xBA
2          2     uint16 BE    Measurement count N
--- repeated N times ---
+0         4     uint32 BE    Unix timestamp (seconds, UTC)
+4         1     uint8        sensor_type_id (from session creation)
+5         4     float32 BE   Sensor value
+9         8     float64 BE   Latitude      } MOBILE ONLY
+17        8     float64 BE   Longitude     } MOBILE ONLY
--- end repeat ---
4+N*F      1     uint8        XOR checksum of all preceding bytes
```

| | Fixed | Mobile |
|---|---|---|
| frame size `F` | **9 bytes** | **25 bytes** (adds lat/lng) |
| max frames | **6000** | **3000** |
| max body | **54005 B** | **75005 B** |

Shared rules:

- **Resends are free** — a frame whose `(stream, timestamp)` is already stored is skipped, no duplicate,
  aggregates don't double-count. Mobile also dedupes *within* one payload (first occurrence wins) and
  ignores order, so chunk however you like.
- **Time sync:** an empty body returns `200` immediately; read the current server time from the
  **`X-Server-Time`** response header (Unix epoch, UTC).
- **Timestamps** carry no milliseconds and must sit between 2020-01-01 UTC and 24 h ahead of server time.
  Fixed *drops* out-of-range frames silently (`200`, rest still stored); mobile *rejects* them
  (`invalid_epoch`).
- **Finished sessions still accept a backlog:** frames at or before `finished_at` are stored, later ones
  dropped — and the response is `200` either way, so a straddling batch looks identical to a clean one.
  **Read `finished_at` on the session to know.**

### Error codes

All v3 errors share `{ error_code, message, fields? }` (`fields` only when the request shape is wrong).

| `error_code` | HTTP | Client should |
|---|---|---|
| `unauthorized` | 401 | re-authenticate |
| `session_not_found` | 404 | — |
| `validation_error` | 400 | don't retry unchanged |
| `session_uuid_taken` | 409 | stop retrying, use the existing session |
| `unsupported_sensor_type` | 400 | unknown sensor; on upload = re-read the session's streams and resend |
| `payload_too_short`, `invalid_magic_bytes`, `empty_measurement_count`, `payload_size_mismatch`, `invalid_checksum`, `invalid_epoch`, `invalid_value`, `invalid_location` | 400 | malformed payload — fix it |
| `payload_too_large` | 413 | nothing stored — resend in smaller batches |
| `try_again_later` | 503 | retry after `Retry-After` (seconds) |
| `internal_error` | 500 | retry with backoff |

---

## `Web app:` v3 reads (not ours, for orientation)

`GET /api/v3/fixed_streams/{id}` · `/fixed_measurements?stream_id&start_time&end_time` ·
`/fixed_stream_daily_averages?stream_id&start_date&end_date` · `/station_streams/{id}` ·
`/station_streams/export?station_stream_ids[]&email` · `/station_measurements?station_stream_id&…` ·
`/station_stream_daily_averages?…` · `/timelapse?q`.

"Station" = government (EEA/EPA) data in the newer model.

---

## Still **not** on v3

No v3 equivalent yet — these stay on legacy `/api` with **basic auth**:

- **Account & auth** — `GET/POST /api/user.json`, `POST /api/user/settings`,
  `POST /api/user/delete_account_send_code` · `_confirm`, `POST /users/password.json`
- **Threshold alerts** — `GET/POST /api/fixed/threshold_alerts`, `DELETE /api/fixed/threshold_alerts/{id}`
- **CSV export** — `GET /api/sessions/export_by_uuid.json`
