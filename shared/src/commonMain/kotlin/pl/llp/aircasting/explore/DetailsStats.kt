package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.i18n.Strings
import pl.llp.aircasting.mydata.dateTimeLabel
import pl.llp.aircasting.mydata.timeRangeLabel

/** One stat column on the details screen. */
internal data class DetailsStat(val label: String, val value: Int?)

/**
 * - Explore station: NOW · MIN. 24H · MAX. 24H, the server's last 24 h.
 * - Our live session: NOW · AVG. · MIN. · MAX. over the whole session (Figma 859:7216).
 * - Our finished session: AVG. · MIN. · MAX.: nothing is "now" any more. Legacy hides the stats
 *   for dormant sessions entirely; the redesign's past session card shows these three.
 */
internal fun StationDetails.stats(stream: StationStream, strings: Strings): List<DetailsStat> {
  val now = DetailsStat(strings.stationNow, stream.value)
  if (startTime == null) {
    return listOf(now, DetailsStat(strings.stationMin24h, stream.min), DetailsStat(strings.stationMax24h, stream.max))
  }
  val wholeSession = listOf(
    DetailsStat(strings.pastSessionsAvg.uppercase(), stream.avg),
    DetailsStat(strings.pastSessionsMin.uppercase(), stream.min),
    DetailsStat(strings.pastSessionsMax.uppercase(), stream.max),
  )
  return if (endTime == null) listOf(now) + wholeSession else wholeSession
}

/**
 * Own sessions' line under the title (Figma 1138:28732, the "Location name" slot): start only while
 * live, start - end once finished, like legacy `DurationStringHelper`. Null for Explore stations.
 */
internal fun StationDetails.timeRange(): String? {
  val start = startTime ?: return null
  return endTime?.let { timeRangeLabel(start, it, timeZone) } ?: dateTimeLabel(start, timeZone)
}
