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
| Empty tab (all three sections empty) | `1137:27447` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1137-27447 |


## What the tab shows

Only the user's **own, running** sessions, in three collapsible sections with a count:
**Mobile sessions**, **Fixed sessions**, **Phone Microphone**. Not favourites, not dormant.

**Scope now: Mobile and Phone Microphone.** Fixed (needs `GET /api/v3/fixed_sessions` — nothing stored locally
for fixed, see `FixedSessionStarter`) and Phone Microphone (no mic recording yet) are parked.

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

## Empty state

Each section keeps its header (count `0`, grey `surfaceContainerHigh` badge) and shows one card:
title, one line, outlined **Start … session** button. Mobile's button opens the new-session flow
(`NewSessionRoute`) at the method step. Fixed / Phone Microphone empty cards wait with their sections.

## Open (parked) questions

- Fixed: when is a session "Offline"? Network-only vs local cache?
- Fixed "Reconnect": hidden, disabled or no-op?
- Process death leaves a `RECORDING` row with nothing feeding it — check legacy handling.
