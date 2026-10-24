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
import pl.llp.aircasting.domain.GeoLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIColor
import platform.darwin.NSObject
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.domain.regionViewport
import pl.llp.aircasting.ui.theme.LocalAqColors
import platform.MapKit.MKMarkerAnnotationView

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun ExploreMap(
  markers: List<ExploreMarker>,
  colourType: MeasurementType?,
  startCentre: GeoLocation?,
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
    update = { map ->
      delegate.show(map, styled)
      delegate.centre(map, startCentre)
    },
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
  private var centredOn: GeoLocation? = null

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
    val view = mapView.dequeueReusableAnnotationViewWithIdentifier(REUSE_ID)
      ?: MKAnnotationView(viewForAnnotation, REUSE_ID)
    view.annotation = viewForAnnotation
    view.draw(styled)
    return view
  }
  /** update{} re-runs on every recomposition — move only when the centre actually changes. */
  fun centre(map: MKMapView, centre: GeoLocation?) {
    if (centre == null || centre == centredOn) return
    centredOn = centre
    map.setRegion(
      MKCoordinateRegionMake(
        CLLocationCoordinate2DMake(centre.latitude, centre.longitude),
        MKCoordinateSpanMake(USER_SPAN_DEGREES, USER_SPAN_DEGREES),
      ),
      animated = false,
    )
  }
}
private const val USER_SPAN_DEGREES = 0.5 // ≈ Google zoom 10 on a phone-width map
// Top level, not in a companion: companions of Obj-C subclasses can't hold fields.
private const val REUSE_ID = "explore-marker"
/**
 * UIKit twin of Android's StationPill / ClusterDot: a translucent halo with, centred on top,
 * a white pill (dot + label) for a station or a solid dot for a cluster.
 */
@OptIn(ExperimentalForeignApi::class)
private fun MKAnnotationView.draw(styled: StyledMarker) {
  subviews.forEach { (it as UIView).removeFromSuperview() } // reused views keep the old marker's
  val colour = styled.colour.toUIColor()
  val label = styled.label
  val haloSize = if (label == null) 28.0 else 56.0
  val content = if (label == null) dot(16.0, colour) else pill(label, colour)
  val width = maxOf(haloSize, content.frame.useContents { size.width })
  setFrame(CGRectMake(0.0, 0.0, width, haloSize))
  val centre = CGPointMake(width / 2, haloSize / 2)
  addSubview(dot(haloSize, colour.colorWithAlphaComponent(0.4)).apply { setCenter(centre) })
  addSubview(content.apply { setCenter(centre) })
}
@OptIn(ExperimentalForeignApi::class)
private fun pill(text: String, colour: UIColor): UIView {
  val label = UILabel().apply {
    this.text = text
    font = UIFont.systemFontOfSize(12.0, UIFontWeightMedium)
    textColor = UIColor.blackColor
    sizeToFit()
  }
  val textWidth = label.frame.useContents { size.width }
  val width = PAD_X + DOT + GAP + textWidth + PAD_X
  val height = PAD_Y + LINE + PAD_Y
  return UIView(CGRectMake(0.0, 0.0, width, height)).apply {
    backgroundColor = UIColor.whiteColor
    layer.cornerRadius = height / 2
    layer.shadowColor = UIColor.blackColor.CGColor
    layer.shadowOpacity = 0.25f
    layer.shadowRadius = 2.0
    layer.shadowOffset = CGSizeMake(0.0, 1.0)
    addSubview(dot(DOT, colour).apply { setCenter(CGPointMake(PAD_X + DOT / 2, height / 2)) })
    addSubview(label.apply { setFrame(CGRectMake(PAD_X + DOT + GAP, PAD_Y, textWidth, LINE)) })
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun dot(size: Double, colour: UIColor) = UIView(CGRectMake(0.0, 0.0, size, size)).apply {
  backgroundColor = colour
  layer.cornerRadius = size / 2
}

// Pill metrics in points — same numbers as the dp values in ExploreMap.android.kt.
private const val PAD_X = 10.0
private const val PAD_Y = 6.0
private const val DOT = 12.0
private const val GAP = 6.0
private const val LINE = 16.0 // labelMedium line height

private fun Color.toUIColor() = UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
