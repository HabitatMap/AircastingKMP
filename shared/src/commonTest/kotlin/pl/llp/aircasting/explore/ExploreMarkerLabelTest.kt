package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.MeasurementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ExploreMarkerLabelTest {

  private fun marker(source: DataSource, value: Int?) = ExploreMarker.Station(
    MapStation(
      uuid = "u1", source = source, title = "Station", provider = "p",
      location = GeoLocation(0.0, 0.0), lastMeasuredAt = Instant.fromEpochMilliseconds(0),
      measurementTypes = listOf(MeasurementType.ParticulateMatter), value = value,
    ),
    level = MeasurementLevel.LOW,
  )

  @Test
  fun one_type_selected_shows_value_with_unit() {
    assertEquals("6 µg/m³", marker(DataSource.Government, 6).label(MeasurementType.ParticulateMatter))
    assertEquals("71 °F", marker(DataSource.AirBeam, 71).label(MeasurementType.Temperature))
  }

  @Test
  fun otherwise_shows_the_source_badge() {
    assertEquals("GOV", marker(DataSource.Government, value = null).label(colourType = null))
    assertEquals("AIRBEAM", marker(DataSource.AirBeam, value = null).label(colourType = null))
  }
}