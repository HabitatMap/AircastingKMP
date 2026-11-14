package pl.llp.aircasting.ui.chart

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.time.Instant

/**
 * The chart formats x as UTC (`ChartAxis`), so adding [timeZone]'s offset *at that instant* makes
 * it read that zone's clock — DST-correct per point, unlike one fixed offset for the whole day.
 */
internal fun wallClockPoint(time: Instant, value: Double, timeZone: TimeZone) =
  ChartPoint(x = (time.epochSeconds + timeZone.offsetAt(time).totalSeconds).toDouble(), y = value)
