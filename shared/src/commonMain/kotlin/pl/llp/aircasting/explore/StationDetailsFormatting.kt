package pl.llp.aircasting.explore

/** Chip text from a stream's sensor name: `AirBeam-PM2.5` → `PM 2.5` (design), `Government-NO2` → `NO2`. */
internal fun streamChipLabel(sensorName: String): String =
  sensorName.substringAfter('-').replace(PM_SIZE, "PM ")

/** `device_model` is the model id; the design shows the product name. */
internal fun deviceDisplayName(model: String): String = when (model) {
  "AirBeamMini" -> "AirBeam Mini"
  else -> model
}

/** `PM` directly followed by a digit — only there does the design add a space. */
private val PM_SIZE = Regex("^PM(?=\\d)")
