# Active Screen — Design Reference

> **Purpose.** Design context and decided rules for the **Active** tab (it replaced the
> Favorites tab — favourites live in My Data). Figma stays the source of truth for pixels.
>
> **File:** Figma "Air Casting app re-design" — fileKey `8AQfUtkIu9qgd4CAVNPbXB`
> **Captured:** 2026-11-08

## Figma URLs

| What | node-id | URL |
|------|---------|-----|
| Whole tab (all sections, all card states) | `461:2780` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=461-2780 |
| Card — "No data yet" | `1009:28154` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1009-28154 |
| Finish confirmation dialog | `1136:18936` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1136-18936 |
| Tab icon `sensors` | `1001:26037` | committed as `composeResources/drawable/ic_sensors.xml` |
| Fixed section (Live + Offline cards) | `1107:16828` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1107-16828 |
| Fixed session details (one dot on the map) | `1138:28153` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1138-28153 |
| Empty tab (all three sections empty) | `1137:27447` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1137-27447 |


## What the tab shows

Only the user's **own, running** sessions, in three collapsible sections with a count:
**Mobile sessions**, **Fixed sessions**, **Phone Microphone**. Not favourites, not dormant.

**All three sections are built** (Fixed added 2026-11-24). Fixed sessions are mirrored from the backend
into the DB (`FixedSessionsSync`) and the tab observes the DB, like mobile. The backend is their only
source of truth: the setup wizard writes nothing locally (`FixedSessionStarter`); the new session arrives
with the tab's next sync.

Phone Microphone — a MOBILE session with device_id = "Builtin" (legacy MicrophoneDeviceItem.DEFAULT_ID); ActiveViewModel splits these out of observeMobile(). Same card, tag PHONE MICROPHONE, one dB chip. Never DISCONNECTED. Empty card (Figma 1137:27447) opens the new-session flow.


## Card anatomy (all sections share it)

1. Header: type tag (`AIRBEAM MOBILE`), status badge (level of the selected stream — "Good"…;
   or "Connection lost"), overflow menu.
2. Name; meta row `AirBeam Mini • 1 min ago` (fixed adds `Indoor •`).
3. User tags.
4. Stream chips (`PM 1`, `PM 2.5`, `PM 10`, `Temp.`, `Hum.`); the selected one's value + unit
   with a level dot; chevron expands the chart.
5. Footer: **Finish recording** · **See details**.

## Mobile card states

| State | When | Body | Footer |
|-------|------|------|--------|
| **Live** | `RECORDING`, ≥1 stream has a reading | value + chips | Finish recording · See details |
| **No data yet** | `RECORDING`, no readings yet | spinner, "No data yet — We're gathering data from your AirBeam." | Finish recording · See details |
| **Connection lost** | `DISCONNECTED` | spinner, "Looking for your AirBeam…" | Finish & sync now · Finish and discard this data |

"Finish & sync now" needs SD-card sync — not built yet; *decided 2026-11-08*: the button is shown
and does nothing until sync exists.

**Finish recording confirms** (*decided 2026-11-08*, Figma `1136:18936`): "Finish recording
[session name]?" — "This will stop the session and move it to Past Sessions in My Data." —
**Keep recording** / **Finish session**.

## Rules (from legacy)

- **Active mobile session** = `type = MOBILE AND end_time IS NULL` (status `RECORDING` or
  `DISCONNECTED`).
- **Chip order** — Figma order: PM1, PM2.5, PM10, F (Temp.), RH (Hum.). Legacy
  `Session.streamsSortedByDetailedType()` sorts by `SensorName` ordinal (F, PM, PM1, PM2.5, PM10,
  RH), i.e. temperature first; the redesign moves it after the PMs.
- **Default selected stream** — legacy `SessionPresenter.selectStream`: the user's saved choice
  for this session → PM2.5 → the first stream in chip order.
- **Data source** — `LocalActiveSessionsRepository`: `selectActiveMobileLatestReadings` (joins
  sessions, streams, measurements) is the one change trigger; sessions and readings are then read
  in one transaction so a card never pairs a session with a stale reading list.
- **Level / colour** — the selected stream's latest value through `Thresholds.forSensor()`
  (aliases resolve `AirBeamMini-PM2.5` → `AirBeam-PM2.5`).

## Fixed sessions (*added 2026-11-24*)

- **Data source** (*persisted 2026-11-24*) — `LocalActiveFixedSessionsRepository` reads the DB
  (`selectActiveFixed` + `selectActiveFixedLatestReadings`, same shape as mobile), so the stored list shows
  at once, offline or after a restart. While the tab is subscribed it runs `FixedSessionsSync.syncList`:
  `GET /api/v3/fixed_sessions` (user token) at once, every 60 s (the AirBeam uploads about once a minute)
  and on `refresh()`. The sync writes the session row (`type = FIXED`, `last_reported_at` =
  `last_measurement_at`), its streams and each stream's newest reading. Signed out → no request. A failed
  sync is only logged; the stored rows stay. **One page of the newest 50** (*decided 2026-11-24*): 500 took
  11 s for an account with 212 fixed sessions, past the client's 10 s timeout. Interim until the backend
  adds a dedicated active-fixed-sessions endpoint (TODO in `FixedSessionsSync.syncList`).
- **Rows are never deleted** (*decided 2026-11-24*): a session the server reports with `finished_at` is
  stored `FINISHED` with that end; an active row missing from the answer is marked `FINISHED`, ending at
  its `last_reported_at` (now if it never reported). No migration for the new columns: testers reinstall.
- **Sign-out** (*decided 2026-11-24*): `FixedSessionsSync.clearWhenSignedOut` (started with Koin, runs for
  the process's life) deletes every fixed row, cascading to streams and measurements, whenever no one is
  signed in — at launch too — so the next account never sees them. Mobile rows stay. Legacy
  `LogoutService` clears the whole DB.
- **Start time is never null** (*decided 2026-11-24*): the backend sets `start_time` when the session is
  created, before any measurement, so `sessions.start_time` stays NOT NULL. (The spec marks it nullable
  without saying when; a `/fixed_sessions` entry without one is skipped by `FixedSessionsSync`, logged.)
  A created session with no measurements yet — older AirBeams don't send one during setup — is stored and
  shows **No data yet**.
- **Active fixed session** = `finished_at == null` on the backend, the only source of truth. It stays on Active, Offline or not, until the user finishes it (*decided 2026-11-24*). Legacy
  has no "finish" for fixed, so every legacy-created session has `finished_at: null` — the backend is to
  clean those up (finish by inactivity) and offer an active-only endpoint; until then old sessions among
  the newest 50 show as Offline.
- **Card states** (`activeFixedSessionCard`):

| State | When | Body | Footer |
|-------|------|------|--------|
| **Live** | heard from within `FixedOfflineAfter` | value + chips | Finish recording · See details |
| **No data yet** | never reported (`last_measurement_at` and every reading null) — e.g. an older AirBeam's session, created on the backend, before its first upload | spinner, "No data yet" | Finish recording · See details |
| **Offline** | `now - last_measurement_at > FixedOfflineAfter` | spinner, "Reconnecting to your AirBeam", **Reconnect** | Finish recording anyway |

- **Offline threshold** — `FixedOfflineAfter = 10 min` in `ActiveSessionCard.kt` (*decided 2026-11-24*, a
  TWEAK HERE constant). Not from legacy (it has no such state). The backend's own `is_active`
  (Explore station details) means "reported in the last 24 h and not finished" — too slow for the
  "lost its Wi-Fi" card, and `/fixed_sessions` doesn't expose it anyway. Judged on `last_measurement_at`
  (when the AirBeam last reached the server), falling back to the newest reading's time.
- **Reconnect** (*decided 2026-11-24*): re-configuring Wi-Fi over BLE isn't built, so it re-syncs the list
  now (`ActiveViewModel.reconnect`); a station that came back flips to Live without waiting for the poll.
- **Finish** (*added 2026-11-24*): same confirmation dialog, then `POST /api/v3/fixed_sessions/{uuid}/finish`
  (server stamps `finished_at`, drops later uploads). The answer's `finished_at` marks the row `FINISHED`,
  so the DB flow drops the card — no re-fetch. Nothing to stop locally. A failed call is only logged; the card stays and Finish can be tapped again. Note the
  dialog's "moves it to Past Sessions" copy is mobile-only today: My Data doesn't list fixed sessions yet.
- **Meta row** leads with the placement: `Indoor • AirBeam Mini • 1 min ago` (Outdoor likewise).
- **Time row** — start - newest reading, like mobile; omitted while `start_time` is null.

## Empty state

Each section keeps its header (count `0`, grey `surfaceContainerHigh` badge) and shows one card:
title, one line, outlined **Start … session** button. Every section's button opens the new-session flow
(`NewSessionRoute`) at the method step.

## Session details ("See details")

Figma `859:7216` — https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=859-7216

**Reuses the Explore station details screen** (`StationDetailsScreen` + `StationDetailsViewModel`)
rather than a copy (*decided 2026-11-14*):
- `StationDetails` carries `type: SessionType` (fixed/mobile) and `source: DataSource`
  (AirBeam / Government / PhoneMic) — the tag is their combination (`AIRBEAM MOBILE`,
  `PHONE MICROPHONE`, …), like legacy `Session.infoString`. Explore's own filter enum is
  `ExploreSource`.
- The ViewModel takes a `Flow<StationDetails?>`: Explore passes a one-shot network flow, our
  session `SessionDetailsRepository.observe(uuid)` — a live DB flow, so values update every reading
  while the chosen chip stays. `null` (session gone) → Error.
- The graph is a slot: Explore → `StationGraph` (24 h, network), session → `SessionGraph`
  (whole session, live DB). Both render through `GraphContent`.
- Koin: the session's ViewModel is the `SessionDetailsQualifier` definition.

Header shows user tags and Indoor/Outdoor; stats are **Now · Avg. · Min. · Max.** over the whole
session (rule in `DOMAIN_KNOWLEDGE.md` → *Own mobile session details*); Explore keeps
NOW · MIN. 24H · MAX. 24H, a finished session drops NOW — see `DetailsStats.kt`.

**Time line under the title** (*decided 2026-11-23, end rule changed 2026-11-24*), Figma
`1138:28732` — the "Location name" slot, bodyLarge / onSurface. Live and finished own sessions
both show **start - newest measurement** (max `measuredAt` over the streams), e.g.
`23/11/26, 13:34 - 23/11/26, 19:34` (UK shown; the date follows the user's Regional format); just
the start until the first measurement. This departs from legacy `DurationStringHelper` (start only
while live, finish tap as the end). The **Active tab card** shows the same line as its own row
under the name (bodySmall / onSurfaceVariant), ending at the card's `lastSeen`.
The meta row's "1 min ago" stays while live, is dropped once finished. Explore stations have no
such line (their location name needs reverse geocoding). Not built yet, like on the
station screen: Map tab, app-bar actions (favourite, alert, notes), location name.

**Track map** (`TrackMap` → `MapDotsOverlay`): while the session is live the newest dot — the
current measurement — gets a 2 dp white ring (*decided 2026-11-24*). The path is 4 dp wide at
50 % alpha `Primary Container` — half Figma's 8 dp opaque stroke (*decided 2026-11-24*).

### Fixed session details (*added 2026-11-24*, Figma `1138:28153`)

Same `StationDetailsScreen`, its own route `FixedSessionDetailsRoute` (one dot on a `StationMap` instead of
a track, and each subscription also syncs). Koin definitions under `FixedSessionDetailsQualifier`:
- Header (*from the DB since 2026-11-24*): `FixedSessionDetailsRepository` = the mobile
  `LocalSessionDetailsRepository` (rows map straight to `StationDetails`, so a fixed row without
  `start_time` works) + one `FixedSessionsSync.syncSession` → `GET /api/v3/fixed_sessions/{uuid}` per
  subscription. Stored values show at once; the synced ones arrive through the same DB flow. A failed sync
  is only logged. Streams in chip order, ids are the DB's.
- Graph (*from the DB since 2026-11-24*): `SessionGraph` with that qualifier — the stored readings, like
  mobile, plus one `FixedSessionsSync.syncMeasurements` per subscription: `GET /fixed_sessions/{uuid}/measurements`
  only after the stream's previous graph sync, at most the 24 h before its newest reading, then older
  readings are deleted (legacy rule, see `DOMAIN_KNOWLEDGE.md` → *Own fixed sessions' measurements*).
- Map: `StationMap`, one dot at the session's location. Indoor (placeholder `200, 200`) → no location
  → no Map tab.
- **Stats: NOW only** for now. Figma shows Now · Avg · Min · Max, but `/fixed_sessions` sends only the
  last reading and a fixed session can run for months, so whole-session avg/min/max need a server
  aggregate — open question for the backend.

## Open (parked) questions

- Process death leaves a `RECORDING` row with nothing feeding it — check legacy handling.
