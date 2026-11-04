package pl.llp.aircasting.domain

/**
 * The bands to colour with, looked up by any spelling of a sensor's name.
 *
 * [aliases] maps a model spelling (`AirBeamMini-PM2.5`) to its canonical parameter
 * (`AirBeam-PM2.5`), as GET /api/v3/threshold_sets lists them. An override replaces its
 * parameter's default for every model at once.
 */
class Thresholds(
  defaults: List<SensorThreshold>,
  private val aliases: Map<String, String>,
  overrides: List<SensorThreshold> = emptyList(),
) {
  // associateBy keeps the last element per key, so an override wins over its default.
  private val effective = (defaults + overrides).associateBy { it.sensorName }

  /** Override, else default. Always named canonically, so saving an edited copy applies to every model. */
  fun forSensor(sensorName: String): SensorThreshold? = effective[aliases[sensorName] ?: sensorName]
}