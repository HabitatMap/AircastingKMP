# Explore Screen — Design Reference

> **Purpose.** Structure and behaviour of the redesigned **Explore** tab, so planning doesn't need
> to re-read Figma every time. Figma stays the source of truth for pixels/tokens — re-fetch with
> `get_screenshot` / `get_design_context` using the node-ids below.
>
> **File:** Figma "Air Casting app re-design" — fileKey `8AQfUtkIu9qgd4CAVNPbXB`
> **Captured:** 2026-10-23

---

## Figma URLs (source of truth)

| What | node-id | URL |
|------|---------|-----|
| **Map section** (4 states) | `1717:1733` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1717-1733 |
| **Filters bottom sheet** | `780:1663` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=780-1663 |
| **Session details** (tap a station) | `1839:1551` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=1839-1551 |

---

## Session details (captured 2026-10-29)

Full screen, **no bottom nav**. Frames: chart (`693:1565`), map (`693:1735`, `693:1925`), map with a
point tooltip, map full-screen; app-bar variants "already in favourites" (`872:7751`, filled heart) and
"notification active" (`859:7445`, ringing bell).

Top → bottom:
- **App bar:** back · export · share · favourite (heart) · threshold alert (bell+).
- **Badges:** source (`AIRBEAM FIXED` / gov) + level of the selected stream's NOW value (`Moderate`).
- **Title**, "Location name", meta row `Outdoor · AirBeam Mini · 1 min ago`.
- **Stream chips** (`PM 1`, `PM 2.5`) — pick the stream everything below shows.
- **Stats:** `NOW` · `MIN. 24H` · `MAX. 24H`, each value with a level dot + unit. (Frame 1 and 3 label
  the third one `MIN. 24H` — a design typo; frame 2 says `MAX. 24H`.)
- **Graph | Map** segmented toggle.
  - *Graph:* line over threshold-coloured bands, hours on the x-axis, tap → tooltip (`01:00 · 13 µg/m³`).
    Graph frame `720:867`: full-bleed, 406 dp plot. Bands use their own light palette (red `#E85E60`,
    orange `#FCA443`, yellow `#FDD961`, green `#96D788`; not Figma variables, no dark variant) →
    `AqBandColors`. Tooltip = `Background` card with `M3/Elevation Light/1`, time in Label Small, level
    dot + value in Title Small Emphasized + unit; white 1 dp guideline; 12 dp `Primary Container` dot.
  - *Map:* track of coloured points + expand-to-full-screen button.
- **Threshold slider** (5 handles, e.g. 0·12·32·55·150) + `Reset to default`.

Endpoints: `API_V3.md` → *Explore session details*.

### Decisions (2026-10-30)

- **Missing fields:** build with what the API gives, and hide what it doesn't. Raise the gaps below
  with the BE in parallel.
- **Map tab:** keep it as a **stationary** map showing one marker at the station's location, at a
  fixed zoom (level to be picked against legacy). There is no track, because a fixed session doesn't move.
- **Thresholds:** colour with the user's **local** thresholds, the same as the Explore map. Defaults
  and server-side thresholds (`threshold_sets`) come in a later slice, together with the graph.
- **Navigation:** ~~the route carries the tapped station's data~~ — superseded 2026-10-30 by the BE's
  `GET /api/v3/explore/stations/{uuid}` (`API_V3.md` → *Explore station details*). The type-safe route
  carries **only the `uuid`**; the details ViewModel fetches the rest. Still no shared in-memory cache
  of the last map response.
- **Indoor/outdoor:** always "Outdoor". Explore only returns outdoor sessions.

### Design ↔ data

**Provided as-is**
- Source badge ← map `source`. Title ← map `title`. Owner/agency ← map `provider`.
- "N min ago" ← map `last_measurement.time` (true UTC).
- NOW value + unit ← map stream `last_measurement.value`, `unit_symbol`.
- Stream chips (only the types the map sent) ← map `streams[]`.
- Stationary map ← map `latitude`/`longitude`.
- 48 h of measurements and 3 months of daily averages ← `fixed_streams/{id}` / `station_streams/{id}`.
- Export ← `export_by_uuid.json` (AirBeam) / `station_streams/export` (gov). Both email a CSV.
- Threshold alert CRUD ← `/api/fixed/threshold_alerts` (AirBeam; basic auth).
- Default thresholds for "Reset to default" ← `threshold_sets`.

**Derivable: needs work on our side**
- *Level badge + level dots:* value → band, via thresholds. The band lookup must go through
  `threshold_sets` `aliases`, because stream endpoints send raw names like `AirBeamMini-PM1`.
- *MIN / MAX 24 h:* compute from `measurements[]`. Sort it first (live order contradicts the spec),
  then cut a 24 h window ending at the newest point. **Open:** raw points or hourly averages?
  AirBeam NOW is an hourly average, but its raw points come every minute.
- *Graph:* sort the points and plot them **raw, not hourly**. The chart frame (`693:1565`) draws a
  dense, jagged line, so it shows every reading (AirBeam: 1-minute data, ~1 440 points per 24 h; gov:
  hourly). Axis labels come from local-as-UTC times, so format them as UTC and don't convert again.
  **Open (design):** the frame's x-axis spans only ~8 h (19:00–03:00). Is it a pannable 24 h, or
  does it show only the last ~8 h?
  *Zoom:* pinch zooms in to at most 2 min, but never past the zoom where the whole series fits. 
  *Tooltip:* follows the finger until the chart starts panning (48 dp sideways) or the page scrolls.
   Fully zoomed out there is nothing to pan, so it follows the finger across the whole width
   (`ScrubUntilPanController`, `canPan` = `VicoScrollState.maxValue ≥ 1 px`).
  A quiet station can send a series only seconds long, and that fits without any zoom. Vico throws
  if `maxZoom < minZoom`, so `MeasurementChart` uses `maxZoom = Zoom.max(Zoom.x(2 min), Zoom.Content)`.
  A series shorter than 2 min therefore can't be zoomed.
- *Every stream of an AirBeam (PM1, PM10, and all types regardless of the active filter):* chain
  `fixed_streams/{id}` → `session_id` → deprecated `/api/fixed/sessions/{id}/streams.json`. This is
  what legacy does.
- *Every catalogue stream of a station regardless of filter (gov included):* re-query
  `/explore/map` with a tight bbox around the station, no `measurement_types`, zoom ≥ 16, and
  match it by `uuid`.
- *Device model (`AirBeam Mini`):* parse the model prefix of a raw `sensor_name` (`AirBeamMini-…`,
  `AirBeam3-…`) from the stream endpoint. Gov stations have no device, so show `provider` there.
- *"Location name":* reverse-geocode lat/lon on the device (Android `Geocoder` / iOS
  `CLGeocoder`, expect/actual).
- *Favourite:* local only, as in legacy follow. It needs a local table.
- *Share link:* build the web map URL and shorten it with `POST /api/short_url`. This needs the
  web app's URL format from the web team.

**Not provided: BE gap** *(first two: answered by `/explore/stations/{uuid}`, announced 2026-10-30)*
- A public "session/station by `uuid`" lookup returning all streams with ids, device model and
  address. Today this takes the chains above, one of them through a deprecated endpoint, and gov
  has no full-stream lookup at all.
- A public share URL for someone else's session (`share_url` exists only on own sessions).
- Threshold alerts for **government stations**: undocumented whether `session_uuid` accepts a
  station uuid.
- Threshold alerts on **bearer** auth. They are basic-auth only, while the rest of v3 uses bearer.
- Server-side storage of the user's edited thresholds. `threshold_sets` is defaults only.

---

## Map screen

Chrome, top → bottom:
- **Search field** "Search by name or location" (search icon) + **filter icon button** (opens the sheet).
- **Active filter chips** row — one dismissible chip per applied filter, e.g. `Particulate matter ✕`,
  `Government ✕`. Hidden when nothing is filtered.
- **Map** — full bleed.
- **Bottom nav:** Home · Explore · Record · Active · My data. *(Differs from `HOME_SCREEN.md`, which
  had Favorites — newer design.)*

### Marker states

| Filter state | Station marker | Source |
|---|---|---|
| **Show all** / several parameters | Blue halo + white pill with source badge: `GOV` (dot) or `AIRBEAM` (AirBeam icon). **No value, no colour.** | frame 1 |
| **Exactly one parameter** (e.g. Particulate matter) | Halo + pill tinted by threshold level, pill shows value + unit: `6 µg/m³` (green), `12 µg/m³` (yellow), `23 µg/m³` (orange), `65 µg/m³` (red). AirBeam keeps its icon, gov a dot. | frames 2–3 |
| Zoomed out (response contains ≥1 `cluster`) | **Every** item — clusters *and* lone stations — is a plain blue dot with a ring. No count, no value, never coloured. Decided in `ExploreViewModel.toMarkers`. | frame 4 |

Matches the API: the server sends `value` only when exactly one `measurement_type` is selected
(`API_V3.md` → *Explore map*), and colour comes from the user's local thresholds
(`DOMAIN_KNOWLEDGE.md` → *Explore map*).

## Filters bottom sheet

Header: `Cancel` · **Filters** · `Done`. Footer: `Clear all` · `Show N results` (outlined button).

- **SOURCE** — single choice: `Show all` (default) · `Only AirBeam Fixed` · `Only Government`.
  → `sources` = omitted / `airbeam` / `government`.
- **PARAMETER** — `Show all` (default) · `Particulate matter` · `Nitrogen Dioxide (NO₂)` · `Ozone` ·
  `Temperature` · `Humidity`. **Multi-select** (confirmed by product 2026-10-23); `Show all` = none
  selected. → `measurement_types` = omitted / comma-joined list.
- Selection is edited in the sheet and applied on `Done` / `Show N results`; `Cancel` discards it.

## Open points

- **`Show N results`** needs a count for the *pending* (not yet applied) filters — fetch with them;
  N = stations + Σ cluster `count` in the viewport. Not built yet.
- **Search by name** — done — Places SDK on both platforms via PlaceSearch; moves the camera to zoom 10 like legacy SearchLocationResultFragment.
- **Units** in the value pill: PM `µg/m³`, NO₂/Ozone `ppb`, Temperature °F (°C = display-time
  conversion), Humidity `%`.
