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
- **Search by name** — the BE queries take a `title` filter internally (PR #1247), but
  `/api/v3/explore/map` doesn't expose it yet. Location search = platform geocoding/places.
- **Units** in the value pill: PM `µg/m³`, NO₂/Ozone `ppb`, Temperature °F (°C = display-time
  conversion), Humidity `%`.
