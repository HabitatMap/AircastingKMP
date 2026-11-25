# AirCasting — Session sync & fetch policy (server → phone)

How the phone keeps its copy of the user's **sessions** (metadata) and **measurements** up to date
with the backend: what legacy did, what the rewrite does today, and the target design that waits on
backend work. Upload (phone → server) is a separate mechanism — the outbox, see
[`API_V3.md` → When the uploader runs](./API_V3.md#when-the-uploader-runs--decided-2026-10-19).

Related: [`DOMAIN_KNOWLEDGE.md` §4](./DOMAIN_KNOWLEDGE.md#4-sync-mechanisms) (all sync flows, incl. device
sync), [`design/ACTIVE_SCREEN.md`](./design/ACTIVE_SCREEN.md), [`design/MY_DATA_SCREEN.md`](./design/MY_DATA_SCREEN.md).

---

## 1. Measurements — fetched lazily (*decided 2026-11-25*)

**Legacy fetched eagerly.** `PeriodicallyDownloadFixedSessionMeasurementsService` sends one request per
fixed session in the local DB every 60 s while the app is in the foreground (it is paused in the
background). That covers `SessionDao.byType(FIXED)`: every own session the backend sync stored (active
**and finished**, opened or not) plus every followed one. `DownloadFollowedSessionMeasurementsService`
downloads the followed ones once more on each return to the foreground (`SessionManager.onAppToForeground`).
`SessionsController.onExpandSessionCard` downloads the one card the user expands. Each request is capped by
`LastMeasurementSyncCalculator`: the last 24 h on first fetch, then incremental, so a finished session
costs one empty request a minute indefinitely, and a fresh login pulls 24 h × every stream × every fixed
session the user ever owned. The legacy commits (`238e785eb`, 2020-09; `fec30f586`, 2021-03) give no
reason. The likely one: the pre-v3 session list carried no latest value, so the card's NOW value and
colour could only come from local measurements. Threshold alerts were never a reason; they are
server-side (`api/fixed/threshold_alerts`).

The rewrite doesn't need that, because the list payload carries each stream's newest reading. So:
- **Own active, card:** the list sync (`FixedSessionsSync.syncList`) re-runs every 60 s while the Active
  tab is subscribed (`LocalActiveFixedSessionsRepository`). One request for all sessions, not one per
  session.
- **Own session, graph:** fetched only when its details open (`FixedSessionsSync.syncMeasurements`; the
  24 h cursor rules are in [`DOMAIN_KNOWLEDGE.md` §4 item 5](./DOMAIN_KNOWLEDGE.md#4-sync-mechanisms)).
  *Open:* polling it while the details of an active session stay on screen.
- **Own finished:** fully lazy; the cursor means a revisit fetches nothing new.
- **Favourites (not built):** eager for the **latest value only**, refreshed while the Favourites screen
  is visible, routed by what the favourite is. *Own active:* the same `syncList` as Active, one request
  for all of them. *Own finished:* nothing, because the value never changes. *Someone else's AirBeam or a
  government station:* the Explore station detail's `streams[].last_measurement`, one request per
  station. Never the measurements endpoints: they take one stream per request and return readings, not a
  latest value. They serve only the graph, lazily, when a favourite's details open. A favourited own
  session is a flag on its existing row, not a copy (legacy `SessionFollower` copied readings into
  `active_session_measurements`). The list stays local (no backend endpoint).
- **Background:** nothing, as in legacy.
- **Never full history.** At ~5 streams × 1 reading/min, one AirBeam is ~7,200 rows/day (~2.6 M/year),
  and the v3 endpoint's 12 h window cap means ~730 requests per stream per year. No screen shows more
  than 24 h. A long-range chart, if wanted, needs a server-side aggregated endpoint (hourly/daily
  averages), not raw readings synced to the phone. Mobile sessions are the exception: they are recorded
  on the phone, so they are local in full anyway.

---

## 2. Session metadata — legacy: `sync_with_versioning`

`SessionsSyncService.sync()` POSTs a **manifest** of every local session except the recording one —
`[{ uuid, deleted, version }]` (`SyncSessionParams`) — to `/api/user/sessions/sync_with_versioning.json`.
The server diffs it against its own copy and answers three lists:
- `deleted` — delete locally. Also how the phone's own deletes went out: `SessionManager.deleteSession` →
  `markForRemoval` (soft delete) → `sync()`.
- `upload` — the server lacks these; the phone sends the whole session.
- `download` — the server's `version` is newer; the phone fetches the session (`SessionDownloadService`:
  session + streams, no measurements — those come from §1's poller).

Edits never went through it: `update_session.json` was called directly. Triggers: see
`DOMAIN_KNOWLEDGE.md` §4 item 5.

**Not copied** (*decided 2026-11-25*):
- It bundles three jobs. Upload already left it (the outbox, *decided 2026-10-19*). Outgoing edit/delete
  are plain REST on v3 (`PATCH` / `DELETE /…/{uuid}`).
- It compares **state**: every sync sends the whole manifest, so its cost grows with the user's total
  session count. Real accounts have **1000+ sessions**. A client-side diff of an ids-only list has the
  same problem (and the v3 lists are paginated anyway).
- v3 has no equivalent endpoint.

Only the incoming direction (changes made elsewhere: web, another phone, backend jobs) needs a
mechanism. That's §4.

---

## 3. Today — TEMPORARY client workarounds

- **Active:** `syncList` takes page 1 of the newest 50 from `GET /fixed_sessions` and keeps the
  unfinished ones (`ACTIVE_SCREEN.md`). An active session older than the newest 50 is missed.
- **Past:** `syncFinished` walks **every page** on each visit and stores only finished entries
  (`MY_DATA_SCREEN.md`) — ~160 legacy sessions downloaded just to be skipped.
- **Deletions made elsewhere never reach the phone.** Plain paging can't report them.
- **Finished mobile sessions** in Past come from the local DB only; nothing pulls server-side changes
  to them.
- Legacy fixed sessions all have `finished_at: null` until the backend finishes them itself.

---

## 4. Target — a change feed of the user's sessions (*proposed 2026-11-25, waits on backend*)

Read **changes since a cursor**, not the whole state: the cost grows with what changed (usually 0–2
sessions per visit), not with the account's size.

### Who stores what
**Server, per session row** (its own bookkeeping; nothing per phone):
- `updated_at` — set to *now* by the server on every **metadata** change, whoever made it: create, edit
  (title, tags, notes), finish (incl. auto-finish), delete. **Not** on new measurements — otherwise every
  active fixed session changes every minute.
- `deleted_at` — set instead of removing the row (soft delete). Normal endpoints hide such rows; the
  feed returns them as **tombstones** `{ uuid, deleted: true }`.

**Phone:** one `cursor` value in the DB = "I've seen every change up to here". The server never learns
it; the phone sends it as a query parameter. Written only after the page it came with is stored, **in
the same transaction**, so a crash mid-walk resumes where it stopped.

### Rules
- **Mobile sessions appear only once finished.** Until then the recording phone is the only owner; the
  echo of a still-uploading session would carry a server copy behind the phone's (e.g. no `end_time`).
  Filtering them out doesn't lose them: `/finish` bumps `updated_at` past any cursor, so the session
  shows up then. Fixed sessions appear in any status.
- **Tombstones for every deletion**, finished or not — a session deleted on the web while still
  uploading must still reach the phone.
- **Cursor = `(updated_at, id)`, opaque to the client.** A bulk job (e.g. auto-finishing ~160 legacy
  sessions) stamps many rows with the same `updated_at`; a time-only cursor either skips the rest of the
  group at a page boundary (`>`) or loops on it (`>=`).
- **Every apply is idempotent:** upsert by uuid, delete-if-present. Our own changes come back through the
  feed (we `PATCH`, the server bumps `updated_at`); applying them again changes nothing. Duplicates are
  harmless; only a missed change would hurt.
- **Upserts write only server-owned columns.** Local-only ones (`is_finished_on_server`, outbox state,
  local file paths) are never touched.
- **Expired cursor → full resync.** If tombstones are hard-delete log entries that get pruned, a cursor
  older than the log gets `410 cursor_expired`: clear the cursor, walk the full feed, delete stored rows
  that didn't appear. With soft deletes this never happens.
- **Uploader meets a deleted session:** `404 session_not_found` on an upload → the session is gone; drop
  its outbox rows instead of retrying forever.
- **Active keeps the list endpoint.** The active set is small and Active needs each stream's latest value
  every 60 s, which the feed deliberately doesn't carry: `GET /fixed_sessions?status=active`.

### Flows
Sessions A, B finished; C fixed, active.

**10:00 — Past opened, first time.** The screen shows the DB at once (empty on a fresh install); the
sync runs alongside and the DB flow updates the list.
`GET /sessions/changes` (no cursor) → `A (09:00)`, `B (09:30)`, `next_cursor = B@09:30`, `has_more: false`.
Store A, B + cursor in one transaction. With 1000+ sessions this walks many pages — **once per install**.

**Elsewhere:** 11:00 A renamed on the web (`A.updated_at = 11:00`) · 12:00 B deleted on the web
(`B.deleted_at = B.updated_at = 12:00`, row kept) · 12:30 C finished (`C.updated_at = 12:30`).

**13:00 — Past opened again.** `GET /sessions/changes?cursor=B@09:30` →
`A (11:00)` → update the title · `{ B, deleted }` → delete B · `C (12:30)` → store C, now finished, so it
joins Past · `next_cursor = C@12:30`. One request, three entries, whatever the account's size.

**13:05 — again.** `cursor = C@12:30` → empty. One tiny request.

**14:00 — C renamed in the app.** `PATCH /fixed_sessions/C`; the server bumps `C.updated_at = 14:00` as
for any edit; the phone updates C from the response. **14:10** the feed returns `C (14:00)` — our own
change echoed; the upsert changes nothing; cursor → `C@14:00`. A `DELETE` works the same: row removed
locally at once, the later tombstone is a no-op.

**Another phone** keeps its own cursor and gets the same changes on its next visit.

---

## 5. The backend ask (*drafted 2026-11-25, not yet agreed*)

### 5.1 New: `GET /api/v3/sessions/changes` — bearer, own sessions

| Query | |
|---|---|
| `cursor` (opt) | opaque, from the previous answer's `next_cursor`. Absent → from the beginning |
| `per_page` (opt) | default 100, max 500 |

```json
{
  "changes": [
    {
      "uuid": "6f1c…",
      "type": "FixedSession",
      "updated_at": 1793282400000,
      "deleted": false,
      "session": {
        "uuid": "6f1c…",
        "title": "Backyard",
        "tag_list": "home",
        "is_indoor": false,
        "time_zone": "Europe/Warsaw",
        "start_time": 1790000000000,
        "finished_at": 1793280000000,
        "latitude": 50.06,
        "longitude": 19.94,
        "device": { "model": "AirBeamMini" },
        "streams": {
          "AirBeamMini-PM2.5": {
            "sensor_name": "AirBeamMini-PM2.5",
            "unit_symbol": "µg/m³",
            "stats": { "avg": 8.4, "min": 0.0, "max": 61.0 }
          }
        }
      }
    },
    {
      "uuid": "a90e…",
      "type": "MobileSession",
      "updated_at": 1793286000000,
      "deleted": true
    }
  ],
  "next_cursor": "eyJ1IjoxNzkzMjg2MDAwMDAwLCJpZCI6NDcxMn0",
  "has_more": false
}
```

- **Order** `(updated_at, id)` ascending. `next_cursor` is always present (also on the last page — the
  phone saves it for next time); `has_more: true` → request again with it.
- **`session`** = the same shape as `GET /{fixed|mobile}_sessions/{uuid}`, **without** measurements and
  without fixed streams' `last_measurement` (that changes without bumping `updated_at`, so it would be
  stale; Active gets it from 5.2). Absent on tombstones.
- **Includes:** fixed sessions in any status; mobile sessions only once finished; a tombstone for every
  deleted session.
- **`stats`** (whole-session `avg`/`min`/`max` per stream, computed once at finish) only on finished
  sessions — and only if design confirms the details stats mean the whole session, not the shown 24 h
  (the latter the phone computes from what it already fetches).
- **`410`** `{ "error_code": "cursor_expired", "message": "…" }` if the cursor predates the retained
  deletions — only needed if deletes are hard deletes.

### 5.2 Changed: `GET /api/v3/fixed_sessions?status=active|finished`
A list filter (`finished_at` null / set), still paginated as today. Active calls `status=active` every 60 s
and drops the TEMPORARY newest-50 cap and the client-side split. Answer unchanged
(`{ sessions, meta }`, streams with `last_measurement`).

### 5.3 Data rules
- `updated_at` bumped on create, edit, notes changes, finish (incl. auto-finish), delete — not on
  measurements.
- `start_time` non-null in the spec (the backend sets it at creation; today the client skips entries
  without one).
- Legacy fixed sessions auto-finished by the backend's own criteria — otherwise `status=active` returns
  all of them.

### Open questions for the backend
1. Are sessions soft-deleted (`deleted_at`)? If not, how long can deletions be kept?
2. Can the cursor be opaque (encoding `(updated_at, id)`)?
3. Does any existing write path change session metadata without touching `updated_at`?

### Open on our side
- Should mobile sessions recorded on another phone (or in the legacy app) show in Past? The feed makes
  it possible; it's a product call. If not, the client ignores entries for unknown mobile uuids and
  still applies tombstones.
- Polling the details graph while an active session's details stay open (§1).
