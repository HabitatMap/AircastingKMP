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
| POST | `/api/v3/mobile_sessions/{uuid}/finish` | JSON `{ finished_at }` | sets `finished_at` to the client's stop time **[A]** |
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

**All streams go in the create call** — *decided 2026-10-16.* v3 has no endpoint to add a stream to an
existing session, and a frame needs the `sensor_type_id` that only the create response hands out. So
the recording flow must know every stream before create (the AirBeam reports its sensors on connect)
and never create one lazily on a sensor's first measurement. A local stream without `sensor_type_id`
can never be uploaded: the uploader skips its rows (they stay in the outbox) and logs an error.

**Create body is flat** — the fields above sit at the top level, not under a `session` key.
**`201` response:** `{ share_url, streams[] { sensor_name, sensor_type_id } }` — write each
`sensor_type_id` back onto the local stream. A `409 session_uuid_taken` on a retry usually means an
earlier create landed and its response was lost: read the ids back from `GET .../{uuid}`, whose
`streams` is an object keyed by `sensor_name`, each with a nullable `sensor_type_id`.

**`/finish` takes the client's stop time** — *agreed with BE 2026-10-16, in the spec since
2026-10-19.* Body `{ "finished_at": <epoch ms, UTC> }`; omit it or send `null` and the server stamps
its own clock. A `finished_at` **in the future** (by the server's clock) is `400 validation_error` —
so a phone clock running ahead can get a `/finish` sent right after stopping rejected. That heals
itself: the next sync pass retries, and by then the server clock has caught up.

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

### ⚠️ Gotchas for whoever writes the uploader

The two uploads look symmetric and are not. Three traps, in the order they bite:

1. **`200` does not mean "everything landed."** It means "nothing was malformed." Frames can be
   dropped individually and still answer `200` — see 2 and 3. Don't treat the response as an ack
   for the batch; a client that clears its outbox on `200` can lose measurements silently.
2. **Out-of-range timestamps diverge.** A frame with epoch 0, before 2020-01-01 UTC, or more than 24 h
   ahead of server time is **silently dropped by fixed** (`200`, the rest of the payload is stored) but
   **rejected by mobile** (`400 invalid_epoch`). So the same clock bug surfaces as data loss on one path
   and a hard failure on the other. *We don't pre-check this client-side* — see outbox rule 3.
3. **A batch straddling `finished_at` is indistinguishable from a clean one.** Frames at or before
   `finished_at` are stored, later ones dropped, response `200` either way, with no count of what was
   kept. The only way to know is to **read `finished_at` off the session** and partition locally before
   sending.
   *Mobile:* defused once the client sends its own `finished_at` — see outbox rule 2 below.

Everything else is forgiving: resends are free, so retry on any transport error rather than trying to
reason about what got through.

### Our outbox — the local `pending_uploads` table

Upload state is **not** stored on `measurements`. It lives in a separate outbox table holding only
keys — `pending_uploads(stream_id, time)`, composite FK → `measurements` `ON DELETE CASCADE`. Each
measurement is stored once, in `measurements`; an outbox row is just "this one hasn't reached the
server yet". Written by `MeasurementRepository.insertRecorded`, read via `UploadOutbox.pending`,
cleared via `UploadOutbox.acknowledge` (a `DELETE` — the data row is untouched; `measurements` stays
insert-only, per `DOMAIN_KNOWLEDGE.md` rule #4). The traps above shape how it may be used:

1. **Only data recorded on this phone is enqueued.** Fixed sessions are uploaded by the AirBeam itself
   (with `session_token`) — except the AB3 SD-card sync edge case, which is not built yet and would
   need the 9-byte fixed frame; downloaded, followed or Explore-saved sessions belong to the server (often
   another user). Those paths call `insertAll`, which never touches the outbox — so the safe default is
   "not uploaded". Only the recording path calls `insertRecorded`, which stores **and** enqueues in one
   transaction, enqueuing only rows actually newly stored (an `OR IGNORE`-skipped duplicate was
   enqueued when first recorded). Every enqueued mobile measurement already has a location — it is
   stamped at insert with a last-known-location fallback (`DOMAIN_KNOWLEDGE.md` §3) — so the uploader
   never invents one.
2. **Acknowledged means "accepted in a `200` batch", not "stored".** See trap 1. So it is only truthful
   if the uploader **never sends a frame the server will silently drop**:
   - **The client owns `finished_at`** (trap 3) — *decided 2026-10-16; the BE change landed 2026-10-19.*
     Stopping is a user action: the app stores the stop locally (`sessions.end_time`) and sends that same
     instant to `POST .../finish`. Every recorded frame is stamped by the same phone clock at or before
     `end_time`, so none can land after `finished_at` — **upload and `/finish` may happen in any order**,
     and the uploader never reads `finished_at` back. (With server-side stamping, a phone clock running
     ahead of the server's would silently drop the last frames; sending our own time removes that.)
3. **No client-side clock checks** (trap 2) — *decided 2026-10-16.* Phone clocks are network-synced,
   and the legacy app never checked timestamps against server time without trouble. So the uploader
   doesn't sync the clock or filter frames by the server's bounds. If a broken clock ever produces an
   `invalid_epoch` batch, the request throws like any other failure, the rows stay queued, and the
   error is surfaced to the user and to crash reporting — a visible stuck upload, not silent loss.
4. **Track per row `(stream_id, time)`, never a time cursor.** Mini V2 reconnect-sync backfills
   *older* timestamps after newer ones were already uploaded; a per-session "uploaded up to T" cursor
   would treat them as done and lose them.
5. **Retry is otherwise blind and safe.** On transport errors, timeouts and `5xx`, leave the outbox
   rows in place — they go out again next pass. Resends are deduped server-side on
   `(stream, timestamp)`. State lives in SQLite, so it survives process death.

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
