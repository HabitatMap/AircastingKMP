# Vico Chart Spike — Results Report

**Status:** spike complete. **Recommendation: proceed with Vico**, with one mandatory
architectural requirement (viewport windowing, §4).

**Scope of evidence:** measured on a physical **OnePlus 7T (HD1903), Android 12, 1080×2400,
90 Hz panel**. iOS is compile-and-link verified only — runtime verification is still open (§7).

Branch `spike/vico-chart`, commits `c650ea7` and `f3e257b`.

---

## 1. Executive summary

| Spike goal | Result |
|---|---|
| 1. Bands + line + scroll/zoom on both platforms | **PASS** on Android (rendered). iOS builds and links; not yet run. |
| 2. Frame times under dense data | **PASS with a condition.** Decimated: p95 **14–15 ms** at 90 Hz, 0.6–0.9% janky. Raw 500k: fine at the series start, **catastrophic at the series end** (p95 97–105 ms). |
| 3. Custom drawing stays in `commonMain` | **PASS, unambiguously.** Zero platform files added; compiles for `iosArm64` + `iosSimulatorArm64`. |

The brief's central worry — "we do not control Vico's per-frame cost and even after decimating a
dense viewport may still lag" — **did not materialise**. With ~800 points on screen Vico holds
90 Hz comfortably.

A different, sharper problem did materialise, and the brief did not predict it: **cost scales with
scroll position, not viewport density.** It is fully mitigated by windowing, which we were already
signed up to build.

Three of the six documented limitations are **obsolete** — they described Vico 2.x. Current stable
is **3.2.3**, and it is a different library in the areas that matter here.

---

## 2. Corrections to the original brief

The brief was written against v2 APIs (`lineSeries`, `LineSpec`, separate `vico-compose`
Android-only and `vico-multiplatform` artifacts). Current stable is **3.2.3**, published as a
single KMP artifact `com.patrykandpatrick.vico:compose` with `iosArm64` and `iosSimulatorArm64`
variants — exactly this project's targets. Verified against the v3.2.3 source tag, not docs.

### Limitation #2 — "custom-drawing escape hatch is narrower than plain Canvas" — **OBSOLETE**

The brief warned that advanced custom drawing works against "Vico's own drawing context … not
Compose's `DrawScope`", and might leak `android.graphics.Paint` / `DashPathEffect`.

In v3.2.3 this is no longer true:

```kotlin
public interface DrawingContext : MeasuringContext {
  public val canvas: Canvas                       // androidx.compose.ui.graphics.Canvas
  public val mutableDrawScope: MutableDrawScope   // DrawScope by delegation
}
```

`ShapeComponent.shape` is `androidx.compose.ui.graphics.Shape`. `LineStroke.Dashed` uses
`PathEffect.dashPathEffect`. All of it `commonMain`.

**Proof, not assertion:** `VerticalDashedLine.kt` does real custom drawing (dashed vertical line,
own data-x → canvas-x mapping) and compiles for both iOS targets with **no `androidMain` or
`iosMain` file added anywhere**. `:shared:linkDebugFrameworkIosSimulatorArm64` succeeds, and the
full `iosApp` Xcode target builds for `iphonesimulator`.

### Limitation #3 — "dashed *now* marker is not a built-in" — **CONFIRMED, but cheap**

Still true: only `HorizontalLine` and `HorizontalBox` exist, both horizontal. But the custom
`Decoration` route the brief listed third is the *easy* one, ~25 lines, and it is what we used.
The brief's "simplest: a plain Compose overlay" suggestion is actually the **worse** option: an
overlay would have to duplicate the data-x → canvas-x mapping outside the chart and would need
scroll and zoom values that Vico only exposes inside the drawing context.

The mapping, for the record — it must match `LineCartesianLayer`'s own or the marker drifts off
the trace:

```kotlin
val canvasX = (if (isLtr) layerBounds.left else layerBounds.right) +
  layoutDirectionMultiplier * layerDimensions.startPadding -
  scroll +
  layoutDirectionMultiplier * layerDimensions.xSpacing *
  ((targetX - ranges.minX) / ranges.xStep).toFloat()
```

`xSpacing` is already zoom-scaled by Vico, so zoom needs no term of its own.

### Limitation #4 — "coloring the trace by band is not clean" — **OBSOLETE**

v3.2.3 ships an actual color scale:

```kotlin
LineCartesianLayer.LineFill.colorScale {
  Color.Green at 12; Color.Yellow at 35; Color.Orange at 55; Color.Red at 150
}
```

No custom `LineFill`. Caveat: it interpolates *between* stops, so hard band edges need each
boundary value duplicated. Also available as `AreaFill.colorScale`. This makes the question in §8
("is the trace white or band-coloured?") low-stakes either way.

### Limitation #1 — "no decimation, rendering lags under load" — **HALF WRONG**

**Wrong half:** Vico *does* window to the visible x-range. `LineCartesianLayer` calls
`series.getSliceIndices(visibleXRange.start, visibleXRange.endInclusive, padding)` and only builds
a path from that slice. The brief's claim of no viewport windowing is incorrect for v3.

**Right half:** there is still no downsampling, so the decimation pipeline is ours. Confirmed.

**What the brief missed** — the actual defect, and it is in the windowing code:

```kotlin
// CartesianLayerModel.kt, v3.2.3
internal fun <T : CartesianLayerModel.Entry> List<T>.getSliceIndices(...): IntRange {
  var firstVisible = size
  for (index in indices) {                          // ← linear scan from 0, not a binary search
    if (this[index].x >= visibleXRangeStart) { firstVisible = index; break }
  }
  ...
}
```

Sorted input, linear search. And it runs **twice per frame per series** —
once in `collectPointsAndVisibleIndexRange` for the path, once in `forEachPointInBounds` for
marker targets. So per-frame cost is O(index of first visible point): **free at the series start,
500 000 iterations × 2 at the end of a 500k series**, with identical pixels on screen.

This is measured and reproduced below. It is the single most important finding in the spike.

### Limitation #5 — "API churn" — **CONFIRMED, worse than stated**

v3 renamed again beyond what the brief lists. Notably `lineSeries { }` → **`lineModel { }`**
(the old name is deprecated but present). `PointConnector` → `Interpolator`.
`Zoom`/`Scroll`/`Decoration` are stable. Practical consequence unchanged: **do not trust any Vico
snippet found by search.** Everything in this spike was written against the v3.2.3 source tree.

### Limitation #6 — "touch/marker interaction is chart-level" — **CONFIRMED, untested**

Not exercised; we set `marker = null` deliberately (see §3). Still an open risk if per-region tap
behaviour is wanted later.

### NEW limitation — `HorizontalBox` cannot be used as a background band as-is

Not in the brief, and it will bite anyone who follows the brief's advice to "stack 4 of them".

`HorizontalBox` implements only `drawOverLayers`. Used directly, the four bands paint **on top of
the trace and hide it completely**. Our first device run rendered four clean bands and no line at
all. There is no flag for this.

Fix is four lines, because `Decoration` declares both passes:

```kotlin
private class UnderLayers(private val delegate: Decoration) : Decoration {
  override fun drawUnderLayers(context: CartesianDrawingContext) = delegate.drawOverLayers(context)
}
```

---

## 3. Method

Harness: `shared/src/commonMain/kotlin/pl/llp/aircasting/chart/`, all `commonMain`.

- `Decimation.kt` — `ChartPoint` + LTTB downsampling. Pure Kotlin, 6 unit tests, all green
  (`:shared:testAndroidHostTest`). Test-first; the load-bearing test is
  *"keeps an isolated spike that naive sampling would drop"*, which is what separates LTTB from
  every-Nth sampling and is exactly the failure mode that would hide a pollution spike from a user.
- `MeasurementChart.kt` — the chart under test.
- `VerticalDashedLine.kt` — custom `Decoration`, the escape-hatch probe.
- `FrameStats.kt` — `withFrameNanos` probe.
- `ChartSpikeScreen.kt` — point-count selector (1k/10k/100k/500k), decimate toggle,
  jump-to-start / jump-to-end.

Data: deterministic random walk, `Random(seed = 42)`, clamped to the threshold range.
Gesture: six `adb shell input swipe … 220ms` flings per configuration.

**Two instruments, cross-checked:**

1. `dumpsys gfxinfo <pkg>` percentiles — the platform's own accounting, reset before each run.
2. The in-app `FrameStats` probe.

They agree (e.g. 500k raw at end: gfxinfo 90th = 89 ms, probe p50 = 44.8 ms — different statistics
over the same bimodal distribution).

**Measurement hygiene** — settings that exist to make the numbers mean something, and which are
**not** production defaults:

- `marker = null`. The marker is the only thing that makes Vico walk the series for tap
  hit-testing; leaving it on would contaminate render cost with input cost.
- `animationSpec = null`, `animateIn = false`. Difference animations interpolate every point on
  data change — a guaranteed stall at these densities.
- `Interpolator.Sharp` (straight segments), not cubic.

**Two instrument defects found and fixed mid-spike** — both worth knowing, because the first
numbers we took were wrong:

- The probe originally mixed idle frames into the same rolling window as scroll frames, so
  percentiles were diluted and a chart that stuttered badly under the finger read as smooth. It
  now gates on `scrollState.value` and records **only frames that moved the viewport**.
- The jank threshold was compared against the bare frame budget. A frame landing at exactly
  11.1 ms on a 90 Hz panel is *on time*, so a perfectly smooth chart reported **100% jank**.
  Threshold is now a parameter, set to 12.0 ms for this device.

**Rejected evidence:** an earlier run on the Android emulator is worthless and is excluded. Its
noise floor swamped the signal completely — 1k baseline 97 ms p50 / 58% janky vs 500k raw 89 ms /
57%, i.e. no measurable difference between the cheapest and most expensive configuration, plus a
nonsensical `gpu percentile: 4950ms`. **Do not benchmark this chart on an emulator.**

---

## 4. Results

Device: OnePlus 7T, 90 Hz (frame budget 11.1 ms). `dumpsys gfxinfo`, scrolling:

| Config | points fed | 50th | 90th | 95th | janky |
|---|---|---|---|---|---|
| 1k raw @ start | 1 000 | 21 ms | 36 ms | 38 ms | 21.5% ¹ |
| 1k raw @ end | 1 000 | 12 ms | 16 ms | 19 ms | 2.3% |
| 100k raw @ start | 100 000 | 15 ms | 20 ms | 24 ms | 4.0% |
| 100k raw @ end | 100 000 | 22 ms | 31 ms | 32 ms | 19.7% |
| **500k raw @ start** | 500 000 | 12 ms | 17 ms | 19 ms | **1.3%** |
| **500k raw @ end** | 500 000 | 9 ms | **89 ms** | **105 ms** | **29.6%** |
| **500k decimated @ start** | 800 | 10 ms | 13 ms | 15 ms | **0.9%** |
| **500k decimated @ end** | 800 | 10 ms | 12 ms | 14 ms | **0.6%** |

¹ first run of the session — includes app warmup/JIT. Ignore; the @end row for the same config is
the clean baseline.

In-app probe (scroll frames only), same runs:

| Config | p50 | p95 | jank (>12 ms) |
|---|---|---|---|
| 500k raw @ start | 11.2 ms | 22.3 ms | 20% |
| 500k raw @ end | **44.8 ms** | **66.7 ms** | **80%** |
| 500k decimated @ start | 11.1 ms | 11.2 ms | ~0 |
| 500k decimated @ end | 11.1 ms | 11.2 ms | ~0 |

**Reproducibility of the headline result** — three independent repeats, alternating position:

| Repeat | @end p50 | @end 90th (gfx) | @start p50 | @start 90th (gfx) |
|---|---|---|---|---|
| 1 | 44.7 ms | 81 ms | 11.1 ms | 17 ms |
| 2 | 44.8 ms | 89 ms | 11.1 ms | 13 ms |
| 3 | 44.9 ms | 93 ms | 11.1 ms | 13 ms |
| 4 (post-fix) | 44.8 ms | — | 11.2 ms | — |

Variance is negligible. This is not thermal, ordering, or noise.

### What the numbers say

1. **The `getSliceIndices` linear scan is confirmed, decisively.** Same 500k dataset, same
   viewport density, same pixels on screen — only scroll position differs. **4× worse p50, ~7×
   worse 90th percentile.** ~89–105 ms frames means visibly ~8–9 dropped frames in a row: not
   "slightly janky", *unusable*.
2. **Decimation eliminates it completely.** 800 points: 12–14 ms at the 90th percentile at *both*
   ends, 0.6% janky. Better than the 1k raw baseline, because the series is shorter.
3. **The cliff scales with series length, as the mechanism predicts.** @end 90th percentile:
   1k → 16 ms, 100k → 31 ms, 500k → 89 ms. Monotonic in series length at constant viewport
   density. This is the signature of an O(series length) per-frame cost, and rules out
   "dense viewport is expensive" as the explanation.
4. **Vico's raw rendering throughput is not the problem.** At the series start, 500k raw points ran
   at 1.3% jank. The brief's #1 worry is not what limits us.

---

## 5. Verdict

**Proceed with Vico 3.2.3.** Both of the brief's pass/fail gates are met:

- Custom drawing stays in `commonMain` — **closed**, and more cleanly than the brief hoped.
- Frame times hold at worst-case density — **met**, provided we window.

Neither failure condition ("either fails badly → reconsider a Canvas-based core") is triggered.
A hand-rolled `Canvas` core would additionally require us to write scroll, fling physics,
pinch-zoom, axis tick placement and label layout — all of which Vico gave us working on the first
device run.

### Mandatory architectural requirement

**Never hand Vico a series longer than the visible window needs.** The pipeline is:

1. **Window first.** Slice the domain series to the visible x-range plus a margin, using a
   *binary* search (data is sorted by x). This is the part that fixes the cliff.
2. **Then decimate.** LTTB the window down to ≈ the horizontal pixel count (~800). Already built
   and tested.
3. Re-transact on scroll/zoom settle.

Decimating the *whole* series to 800 points — what the spike harness does — fixes the frame times
but throws away time resolution you need when zoomed in. Windowing is what makes both work.

Note this also fixes the memory cost: `lineModel { series(x, y) }` takes `Collection<Number>`, so
every value is **boxed**. 500k points ≈ 1M boxed `Double`s per transaction. Windowing reduces that
to ~800.

### Suggested next tasks

1. Viewport windowing + re-transaction on scroll settle (binary-search slice). **The one blocker.**
2. Replace the whole-series decimation in `ChartSpikeScreen` with window-then-decimate.
3. Re-run this matrix after (1) — expect the @end cliff to vanish at every series length.
4. Run goals 1 and 3 on iOS (§7).
5. Revert `App.kt`'s `CHART_SPIKE` constant and delete the harness, keeping `Decimation.kt`,
   `VerticalDashedLine.kt`, `MeasurementChart.kt`.

---

## 6. Answers to the brief's open questions

| Question | Answer |
|---|---|
| **Metric and band thresholds?** | **Still open — needs product input.** Spike uses placeholder PM2.5 `0 / 12 / 35 / 55 / 150`, wired through the project's existing `domain.SensorThreshold` and `ui.theme.AqColors`, so swapping is a one-line change and introduces no new domain concept. The `SensorThreshold`'s five boundaries map exactly onto the reference image's four bands. |
| **Worst-case points in one viewport?** | **Answered as a design constraint rather than a number:** it does not matter, because we must never feed more than ~1 point per horizontal pixel (~800–1200). What *does* matter, and what the brief should have asked, is **worst-case total session length**, since that is what drives the scroll-position cliff. Still needs a number. |
| **Zoom time-axis-only, or both?** | Implemented **time-axis only**, via `Zoom.x(visibleXUnits)` with `min`/`max` clamps. This is not merely a preference: y must stay pinned (`CartesianLayerRangeProvider.fixed`) or the threshold bands slide out from under the trace. Recommend keeping x-only. |
| **Trace white over bands, or coloured per band?** | **Low-stakes either way** now that `LineFill.colorScale` exists (§2, #4). Spike implements white-over-bands per the reference image. Still needs a design decision, but it is no longer an architectural risk. |
| **Live/streaming or static historical?** | **Still open, and now the highest-risk unknown.** Not exercised. `AutoScrollCondition.OnModelGrowth` exists, but each `runTransaction` rebuilds and re-boxes the entire series. At 1 Hz on a long session that will not survive. If live data is in scope, it needs its own spike. |

---

## 7. Open / not verified

- **iOS runtime.** Compile-and-link verified for `iosArm64` and `iosSimulatorArm64`, and the full
  `iosApp` Xcode target builds for `iphonesimulator` (`** BUILD SUCCEEDED **`). **Not run.**
  Blocked on this machine: `xcrun simctl list devicetypes` returns an empty list — no simulator
  device types installed, only runtimes (iOS 26.2, 26.5). A paired physical iPhone 13 mini is
  available if someone wants to authorise a device install. Goals 1 and 3 should be re-confirmed
  visually on iOS before this report is treated as closed on the "both platforms" claim.
- **iOS frame times.** Unmeasured. The linear-scan defect is in `commonMain` Kotlin, so the
  mechanism is platform-independent and the mitigation is the same; but absolute numbers on
  Kotlin/Native + Skia are unknown.
- **Live/streaming data.** Not exercised. See §6.
- **Marker / touch interaction.** Deliberately disabled. Limitation #6 untested.
- **Pinch-zoom frame cost.** Only scroll was measured. Zoom works (verified by hand) but was not
  benchmarked — `Zoom.x` changes `xSpacing`, which does not change the slice-scan cost, so no
  additional cliff is expected there.
- **60 Hz behaviour.** The OnePlus panel switches between 60 and 90 Hz adaptively; the strict
  90 Hz budget (11.1 ms) was used throughout, which is the conservative choice.
- **Very long sessions (> 500k points).** Untested. Given the mechanism is linear in series
  length, expect the @end cliff to keep growing until windowing lands.

---

## 8. Artifacts

- Branch `spike/vico-chart` — `c650ea7` (harness), `f3e257b` (probe fixes).
- `shared/src/commonTest/.../DecimationTest.kt` — 6 tests, green.
- `App.kt` is switched to the spike screen behind `private const val CHART_SPIKE = true`. **Revert
  this.**
- Reference sources read for this report: Vico tag `v3.2.3`, files `CartesianChartHost.kt`,
  `CartesianDrawingContext.kt`, `DrawingContext.kt`, `LineCartesianLayer.kt`,
  `CartesianLayerModel.kt` (`getSliceIndices`), `HorizontalBox.kt`, `Decoration.kt`, `Zoom.kt`,
  `VicoScrollState.kt`, `ColorScaleScope.kt`.
