# Home Screen — Design Reference

> **Purpose.** Preserve the Figma design context for the redesigned **Home** screen so it
> can be referenced during planning/implementation without re-fetching every time. The
> Figma MCP is still the source of truth — these URLs can be re-read anytime for exact
> pixels/tokens/assets. This doc captures the *structure, states, and design system* so
> the shape of the work is stable in context.
>
> **File:** Figma "Air Casting app re-design" — fileKey `8AQfUtkIu9qgd4CAVNPbXB`
> **Captured:** 2026-07-20
> **Frame size:** 412 × 1790 (Android, light scheme)

---

## Figma URLs (source of truth)

| State | node-id | URL |
|-------|---------|-----|
| **Populated / "Good"** | `277:7267` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=277-7267 |
| **Empty — no readings** (location known) | `131:10254` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=131-10254 |
| **Empty — no location services** | `131:10612` | https://www.figma.com/design/8AQfUtkIu9qgd4CAVNPbXB/Air-Casting-app-re-design?node-id=131-10612 |

> Re-fetch with `get_design_context` (code + tokens + assets) or `get_screenshot` (visual)
> using the node-id + fileKey above. Asset URLs from `get_design_context` expire in ~7 days,
> so download-and-commit any icon/image bytes at implementation time — never inline them.

---

## The screen = one layout, three data states

All three variations are the **same Home screen** with the same chrome and section order.
Only the **Air Quality card** and **Weekly exposure card** change with data availability.

### Shared chrome (all states)
- **Status bar** — 52px; time `9:30`, wifi/signal/battery, camera cutout.
- **App bar** — 64px; AirCasting wordmark logo (left) + `settings` icon button (right).
- **Bottom navigation** — 5 items: `Home` (home), `Explore` (map), `Record` (add_circle),
  `Favorites` (favorite), `My data` (folder). White bg, elevation-2 shadow, rounded
  active-icon container, gesture handle bar below.
- **Content container** — width 364, `gap 24` between sections, centered.

### Section order (top → bottom)
1. **Air Quality card** (state-dependent)
2. **Nearby stations** — header "Nearby stations" + "View map ›" text button; horizontal
   carousel of **Station cards** (populated in all three states).
3. **Your weekly exposure** — header + **Weekly exposure card** (state-dependent).
4. **Learn more** — header + 3 **Info item** cards.

---

## State details

### 1. Populated / "Good" — `277:7267`
- **Air Quality card:** green ghost/AQI icon, big status "Good" (green), subtitle
  "The air outside is clean. A great time to enjoy activities outside.", 3 metric chips
  each with `help` icon: **PM 2.5** 4.2 µg/m³, **NO₂** 9.1 ppb, **Ozone** 4.2 ppb.
  Below: station selector row "Central Park Station, New York / 0.4 mile away" + chevron.
  Footer note: "Based on your current location." (near_me icon).
- **Weekly exposure card:** bar chart with Y-axis bands (Good / Moderate / Poor /
  Unhealthy), 7 daily bars (Mon–Sun) with values; stat row **Avg. PM 2.5** 8.2,
  **Peak PM 2.5** 15.0, **Good days** 4/7; advice text "Your average PM 2.5 level this
  week is 8.2 µg/m³. Consider reducing outdoor time on high-pollution days."; pager dots.
  Monitor selector chip "GOVERNMENT MONITOR" + "PM 2.5 ▾".

### 2. Empty — no readings — `131:10254`
- **Air Quality card:** title "No air quality data available", body "We couldn't find
  current readings for this location. Select a different station below or check again
  later.", **station selector row** (outlined) "Central Park Station, New York /
  0.4 mile away" + chevron. Footer note "Based on your current location." (near_me).
- **Weekly exposure card:** empty state — stats show `--`; `circle-x` icon; "No data yet"
  / "We're gathering data from your station."

### 3. Empty — no location services — `131:10612`
- **Air Quality card:** title "No nearby station found", body "We couldn't match your
  location to a reporting station. Try enabling precise location.", **outline button
  "Turn on location services"**. Footer note "Location services turned off"
  (near_me_disabled icon).
- **Weekly exposure card:** same empty state as #2.

---

## Reusable component boundaries (proposed)

Sections repeat across states → natural composables:

| Composable | Notes |
|------------|-------|
| `HomeTopAppBar` | logo + settings; shared |
| `HomeBottomNav` | 5 items; shared |
| `AirQualityCard` | polymorphic on state: **Good/populated**, **NoReadings**, **NoLocationServices**. Shares station selector row + location note sub-components. |
| `AirQualityMetricChip` | dot + value + unit + label (PM 2.5 / NO₂ / Ozone) |
| `StationSelectorRow` | station name + distance + chevron (used in populated + no-readings) |
| `LocationNote` | icon + text ("Based on your location" / "Location services turned off") |
| `NearbyStationsSection` | header + "View map" + carousel |
| `StationCard` | GOV MONITOR tag + status badge + name/location/updated + 3 metric cells |
| `WeeklyExposureCard` | polymorphic: **chart+stats** vs **empty state**; monitor + pollutant selector |
| `WeeklyExposureChart` | 7-day bar chart with AQI bands |
| `LearnMoreSection` | header + list |
| `InfoItemCard` | thumbnail image + category + title + open_in_new icon |

---

## Design system (Material 3, light scheme, Roboto)

### Colors
| Token | Hex |
|-------|-----|
| `background` | `#F5FAFF` |
| `surface-container-lowest` (cards) | `#FFFFFF` |
| `surface` (metric cells) | `#FCF8F8` |
| `surface-container` | `#F1EDEC` |
| `primary-container` | `#00B2EF` |
| `secondary` | `#006382` |
| `on-primary-container` | `#004059` |
| `on-surface` | `#1D1B20` |
| `on-surface-variant` | `#44474A` / `#49454F` |
| `outline` | `#75777B` |
| `outline-variant` | `#CAC4D0` |
| `on-background` | `#171C20` |
| `on-secondary-fixed` (info title) | `#001E2B` |
| `on-secondary` (button label) | `#003547` |
| green (Good badge/dot) | `#006E02`, badge bg `#E8F5E9` |
| gov-tag border | `#00B2EF`, bg `rgba(10,125,163,0.08)` |

### Typography (Roboto)
| Style | Size / Line / Weight / Tracking |
|-------|--------------------------------|
| title-large | 22 / 28 / 500 / 0 |
| title-medium | 16 / 24 / 500 / 0.15 |
| title-small | 14 / 20 / 500 (semibold 600 in station names) / 0.1 |
| body-medium | 14 / 20 / 500 / 0.25 |
| body-small | 12 / 16 / 400 / 0.4 |
| label-small | 11 / 16 / 500 / 0.5 |
| label-medium | 12 / 16 / 500 / 0.5 |

### Shape / spacing / elevation
- Card radius **16**; chips/metric cells/tags radius **8**; badge radius **100** (pill).
- Card padding **16**; section gap **24**; content width **364**; station card width **332**.
- Metric icons **24**; chip dots **8**; empty-state icon **64** in 72 container.
- Elevation-2 (bottom nav): `0 1 2 rgba(0,0,0,0.3)` + `0 2 6 2 rgba(0,0,0,0.15)`.

### Key Figma components (with docs in file)
- `settings` icon — node `9:44587`
- `Button - outline` — node `3:6876` (five color/size options; sentence-case labels)
- Icon button `Type=Square, Size=Small` — node `3:6958`
- `Navigation Bar: Vertical items/5` — node `8:11245`

---

## Open questions / notes for implementation
- Bar chart (`WeeklyExposureChart`) is the only non-trivial custom draw — needs AQI band
  background + labeled bars; decide Canvas vs. existing chart lib in the KMP stack.
- Pollutant selector ("PM 2.5 ▾") implies switchable pollutant on both AQ card + chart.
- Nearby stations carousel is present even in "empty" AQ states → nearby data is
  independent of the current-location reading.
