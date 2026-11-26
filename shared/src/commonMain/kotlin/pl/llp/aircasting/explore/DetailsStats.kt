package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.i18n.Strings
import pl.llp.aircasting.ui.format.DateFormatter

/** One stat column on the details screen. */
internal data class DetailsStat(val label: String, val value: Int?)

/**
 * - Explore station: NOW · MIN. 24H · MAX. 24H, the server's last 24 h.
 * - Our live session: NOW · AVG. · MIN. · MAX. over the whole session (Figma 859:7216).
 * - Our own fixed session: NOW only while live; AVG. · MIN. · MAX. empty once finished (see below).
 * - Our finished session: AVG. · MIN. · MAX.: nothing is "now" any more. Legacy hides the stats
 *   for dormant sessions entirely; the redesign's past session card shows these three.
 */
internal fun StationDetails.stats(stream: StationStream, strings: Strings): List<DetailsStat> {
  val now = DetailsStat(strings.stationNow, stream.value)
  if (startTime == null) {
    return listOf(now, DetailsStat(strings.stationMin24h, stream.min), DetailsStat(strings.stationMax24h, stream.max))
  }
  fun wholeSession(avg: Int?, min: Int?, max: Int?) = listOf(
    DetailsStat(strings.pastSessionsAvg.uppercase(), avg),
    DetailsStat(strings.pastSessionsMin.uppercase(), min),
    DetailsStat(strings.pastSessionsMax.uppercase(), max),
  )
  // Our own fixed session: the server sends no avg/min/max (/fixed_sessions has only the last reading),
  // and a fixed session can run for months, so they'd need a server-side aggregate. NOW while live;
  // once finished, the finished columns, empty (--) until then.
  if (type == SessionType.FIXED) return if (endTime == null) listOf(now) else wholeSession(null, null, null)
  val whole = wholeSession(stream.avg, stream.min, stream.max)
  return if (endTime == null) listOf(now) + whole else whole
}

/**
 * Own sessions' line under the title (Figma 1138:28732, the "Location name" slot): start - newest
 * measurement, live or finished (*decided 2026-11-24*; legacy `DurationStringHelper` showed the
 * start alone while live and used the finish tap as the end). The start alone until the first
 * measurement. Null for Explore stations.
 */
internal fun StationDetails.timeRange(dates: DateFormatter): String? {
  val start = startTime ?: return null
  return dates.range(start, streams.mapNotNull { it.measuredAt }.maxOrNull(), timeZone)
}
