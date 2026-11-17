package pl.llp.aircasting.domain

/** One reading placed on the map. [value] null = a station that never reported: drawn, uncoloured. */
data class TrackPoint(val location: GeoLocation, val value: Double?)
