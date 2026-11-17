package pl.llp.aircasting.domain

import kotlin.time.Instant

/**
 * One reading placed on the map. [value]/[time] null = a station that never reported: drawn, uncoloured.
 * [time] defaults to null so a test about where a dot lands doesn't have to invent one.
 */
data class TrackPoint(val location: GeoLocation, val value: Double?, val time: Instant? = null)
