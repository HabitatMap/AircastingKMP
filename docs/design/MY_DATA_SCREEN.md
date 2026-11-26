# My Data Screen — Design Reference

> **Purpose.** Design context and decided rules for the **My Data** tab and its **Past sessions**
> list. Figma stays the source of truth for pixels.
>
> **File:** Figma "Air Casting app re-design" — fileKey `8AQfUtkIu9qgd4CAVNPbXB`
> **Captured:** 2026-11-23

## Figma URLs

| What | node-id | URL |
|------|---------|-----|
| My Data, filled (Favorites + Past Sessions rows) | `925:18415` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=925-18415 |
| My Data, empty | `925:19406` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=925-19406 |
| Past sessions list, big cards ("View all") | `925:21111` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=925-21111 |

## What the tab shows

Search bar, then two sections, each a title + count badge + "View all ›" and a horizontal row of
cards: **Favorites** (followed stations) and **Past Sessions** (the user's own finished sessions —
legacy's `MOBILE_DORMANT` tab).

**Built (2026-11-23):** Past Sessions only — section, empty card, "View all" list, See details.
**Parked:** Favorites, search, Filters, the list/card view toggle, the card's ⋯ menu
(edit/delete/share — next slice).
**Added (2026-11-25):** finished fixed sessions in Past Sessions — see *Fixed sessions* below.

## Decided rules

- **Which sessions:** finished rows (`end_time IS NOT NULL`) — mobile (AirBeam and phone mic) and
  fixed alike — newest start first. Phone mic = `device_id = "Builtin"`, tagged PHONE MICROPHONE.
- **Overview card** shows the selected stream's **session average**; the **list card** shows
  **Avg / Min / Max** over the whole session. Each value gets its own threshold colour; no
  thresholds / no value = grey dot and `--`.
- **Chips:** same order and default as Active (PM1, PM2.5, PM10, F, RH; saved → PM2.5 → first).
  Selection is in memory per screen.
- **Time range:** `<date>, <time> - <date>, <time>`, date and clock (US `1:34 PM`, others `13:34`) in the user's Regional format with a
  short year (UK `23/11/26`, US `11/23/26` — see *User-facing dates* in `KMP_ARCHITECTURE.md` §6),
  both dates always (*decided 2026-11-23*): a
  mobile session can cross midnight, and one format reads the same on every card.
  On the session's own time zone, like the details screen.
- **See details** opens the existing session details; the track map frames the whole track
  (`live = false`) instead of following the newest point. Its stats read **AVG. · MIN. · MAX.**
  (no NOW: nothing is current any more; legacy `*MobileDormantMvcImpl` hides stats altogether),
  the level badge follows AVG., and the meta row drops "x d ago" (*decided 2026-11-23*,
  `DetailsStats.kt`). The time range has its own line — see `ACTIVE_SCREEN.md` → Session details.
- **Live-update cost:** the list re-queries only when the `sessions` table changes. The stats
  aggregate scans every past measurement, and an active recording writes each second — see
  `LocalPastSessionsRepository`. SD-card sync will need to re-trigger it.

## Fixed sessions (*added 2026-11-25*)

- **Only when the backend says finished:** a fixed row has an `end_time` only from the backend's
  `finished_at` (see `ACTIVE_SCREEN.md` → *Fixed sessions*), so Past Sessions lists exactly those. Legacy
  fixed sessions all have `finished_at: null`: none show here until the backend finishes them.
- **No sync of its own** (*decided 2026-11-26*; the every-page walk of 2026-11-25 was removed as a
  workaround): Past lists the finished fixed sessions already in the DB — finished in the app, reported
  finished in Active's list sync (newest 500), or finished when their details were opened. Older ones
  appear once the proposed change feed exists — [`SYNC.md` §4](../SYNC.md#4-target--a-change-feed-of-the-users-sessions-proposed-2026-11-25-waits-on-backend).
- **Card values: `--`** (*decided 2026-11-25*): whole-session Avg / Min / Max need a server aggregate
  (`/fixed_sessions` sends only the last reading; locally we keep at most 24 h), so a fixed card's
  aggregates are null — grey dot, `--`. The stats query doesn't join fixed streams' readings at all.
- **Tag** AIRBEAM FIXED; **See details** opens the fixed session details (`FixedSessionDetailsRoute`),
  whose stats are AVG. · MIN. · MAX. as `--` once finished.
