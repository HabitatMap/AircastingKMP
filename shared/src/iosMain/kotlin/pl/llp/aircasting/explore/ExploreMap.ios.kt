package pl.llp.aircasting.explore

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIColor
import platform.darwin.NSObject
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.domain.regionViewport
import pl.llp.aircasting.ui.theme.LocalAqColors

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun ExploreMap(
  markers: List<ExploreMarker>,
  colourType: MeasurementType?,
  onCameraIdle: (MapViewport) -> Unit,
  modifier: Modifier,
) {
  val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
  val delegate = remember { ExploreMapDelegate { currentOnCameraIdle(it) } }
  // Colours resolve here, in Compose, then cross into UIKit as plain values.
  val aqColors = LocalAqColors.current
  val uncoloured = MaterialTheme.colorScheme.primaryContainer
  val styled = markers.map { marker ->
    when (marker) {
      is ExploreMarker.Cluster -> StyledMarker(marker, label = null, colour = uncoloured)
      is ExploreMarker.Station -> StyledMarker(
        marker,
        label = marker.label(colourType),
        colour = marker.level?.let(aqColors::forLevel) ?: uncoloured,
      )
    }
  }
  UIKitView(
    factory = {
      MKMapView().apply {
        this.delegate = delegate
        setRegion(
          MKCoordinateRegionMake(CLLocationCoordinate2DMake(39.8, -98.6), MKCoordinateSpanMake(40.0, 60.0)),
          animated = false,
        )
      }
    },
    modifier = modifier,
    update = { map -> delegate.show(map, styled) },
    // The map owns its gestures; Compose doesn't wait to see if a touch is a scroll.
    properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative),
  )
}

/** A marker with its look resolved in Compose — UIKit only sees text and a colour. */
private data class StyledMarker(val marker: ExploreMarker, val label: String?, val colour: Color)

@OptIn(ExperimentalForeignApi::class)
private class MarkerAnnotation(val styled: StyledMarker) : MKPointAnnotation() {
  init {
    val location = when (val m = styled.marker) {
      is ExploreMarker.Cluster -> m.cluster.location
      is ExploreMarker.Station -> m.station.location
    }
    setCoordinate(CLLocationCoordinate2DMake(location.latitude, location.longitude))
  }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class ExploreMapDelegate(
  private val onCameraIdle: (MapViewport) -> Unit,
) : NSObject(), MKMapViewDelegateProtocol {

  private val shown = mutableMapOf<StyledMarker, MarkerAnnotation>()

  /** Diff instead of clear-and-add, so unchanged markers don't flicker after each fetch. */
  fun show(map: MKMapView, markers: List<StyledMarker>) {
    val next = markers.toSet()
    val gone = shown.keys - next
    map.removeAnnotations(gone.map { shown.remove(it)!! })
    val added = (next - shown.keys).map { styled -> MarkerAnnotation(styled).also { shown[styled] = it } }
    map.addAnnotations(added)
  }

  // regionDidChange / regionWillChange share a Kotlin signature — the override picks by ObjC selector.
  @ObjCSignatureOverride
  override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
    val width = mapView.bounds.useContents { size.width }
    mapView.region.useContents {
      onCameraIdle(regionViewport(center.latitude, center.longitude, span.latitudeDelta, span.longitudeDelta, width))
    }
  }
  override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
    val styled = (viewForAnnotation as? MarkerAnnotation)?.styled ?: return null
    val view = (mapView.dequeueReusableAnnotationViewWithIdentifier(REUSE_ID) as? MKMarkerAnnotationView)
      ?: MKMarkerAnnotationView(viewForAnnotation, REUSE_ID)
    view.annotation = viewForAnnotation
    view.markerTintColor = styled.colour.toUIColor()
    view.glyphText = styled.label
    return view
  }
}

// Top level, not in a companion: companions of Obj-C subclasses can't hold fields.
private const val REUSE_ID = "explore-marker"

private fun Color.toUIColor() = UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
