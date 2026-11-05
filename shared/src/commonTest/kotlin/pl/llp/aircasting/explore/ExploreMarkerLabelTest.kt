package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MapStream
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.MeasurementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ExploreMarkerLabelTest {

  private fun marker(
    source: DataSource,
    value: Int?,
    type: MeasurementType = MeasurementType.ParticulateMatter,
    unit: String = "µg/m³",
  ) =
    ExploreMarker.Station(
      MapStation(
        uuid = "u1", source = source, title = "Station", provider = "p",
        location = GeoLocation(0.0, 0.0),
        streams = listOfNotNull(value?.let {
          MapStream(1, type, sensorName = "AirBeam-PM2.5", unitSymbol = unit, value = it, measuredAt = Instant.fromEpochMilliseconds(0))
        }),
      ),
      level = MeasurementLevel.LOW,
    )
  @Test
  fun one_type_selected_shows_value_with_unit() {
    assertEquals("6 µg/m³", marker(DataSource.Government, 6).label(MeasurementType.ParticulateMatter))
    assertEquals("71 °F", marker(DataSource.AirBeam, 71, MeasurementType.Temperature, unit = "°F").label(MeasurementType.Temperature))
  }

  @Test
  fun the_unit_is_the_streams_own() {
    assertEquals("21 °C", marker(DataSource.AirBeam, 21, MeasurementType.Temperature, unit = "°C").label(MeasurementType.Temperature))
  }

  @Test
  fun otherwise_shows_the_source_badge() {
    assertEquals("GOV", marker(DataSource.Government, value = 6).label(colourType = null))
    assertEquals("AIRBEAM", marker(DataSource.AirBeam, value = 12).label(colourType = null))
  }

  @Test
  fun a_station_without_the_selected_types_stream_shows_the_source_badge() {
    assertEquals("AIRBEAM", marker(DataSource.AirBeam, value = 12).label(MeasurementType.Ozone))
  }
}