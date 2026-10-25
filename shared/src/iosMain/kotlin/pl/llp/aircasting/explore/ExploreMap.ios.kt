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
import kotlinx.cinterop.useContents
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.GeoSquare
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.ui.theme.LocalAqColors
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSBundle
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.darwin.NSObject
import swiftPMImport.Aircasting.shared.GMSCameraPosition
import swiftPMImport.Aircasting.shared.GMSCoordinateBounds
import swiftPMImport.Aircasting.shared.GMSMapStyle
import swiftPMImport.Aircasting.shared.GMSMapView
import swiftPMImport.Aircasting.shared.GMSMapViewDelegateProtocol
import swiftPMImport.Aircasting.shared.GMSMarker
import swiftPMImport.Aircasting.shared.GMSServices
import swiftPMImport.Aircasting.shared.create

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun ExploreMap(
  markers: List<ExploreMarker>,
  colourType: MeasurementType?,
  startCentre: GeoLocation?,
  flyTo: GeoLocation?,
  onFlownTo: () -> Unit,
  onCameraIdle: (MapViewport) -> Unit,
  modifier: Modifier,
) {
  val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
  val delegate = remember { ExploreMapDelegate { currentOnCameraIdle(it) } }
  val style = mapStyle()
  // Colours resolve here, in Compose, then cross into UIKit as plain values.
  val aqColors = LocalAqColors.current
  val uncoloured = MaterialTheme.colorScheme.primaryContainer
  val styled = markers.map { marker ->
    when (marker) {
      is ExploreMarker.Dot -> StyledMarker(marker, label = null, colour = uncoloured)
      is ExploreMarker.Station -> StyledMarker(
        marker,
        label = marker.label(colourType),
        colour = marker.level?.let(aqColors::forLevel) ?: uncoloured,
      )
    }
  }
  UIKitView(
    factory = {
      apiKeyProvided
      val camera = GMSCameraPosition.cameraWithLatitude(START.latitude, START.longitude, START_ZOOM)
      GMSMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), camera = camera).apply {
        this.delegate = delegate
      }
    },
    modifier = modifier,
    update = { map ->
      delegate.style(map, style)
      delegate.show(map, styled)
      delegate.centre(map, startCentre)
      flyTo?.let {
        map.setCamera(GMSCameraPosition.cameraWithLatitude(it.latitude, it.longitude, USER_ZOOM))
        onFlownTo()
      }
    },
    // The map owns its gestures; Compose doesn't wait to see if a touch is a scroll.
    properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative),
  )
}

/** Once per process, before the first GMSMapView. Key comes from Info.plist (GMSApiKey ← Secrets.xcconfig). */
@OptIn(ExperimentalForeignApi::class)
private val apiKeyProvided: Boolean by lazy {
  GMSServices.provideAPIKey(NSBundle.mainBundle.objectForInfoDictionaryKey("GMSApiKey") as? String ?: "")
}

/** A marker with its look resolved in Compose — UIKit only sees text and a colour. */
private data class StyledMarker(val marker: ExploreMarker, val label: String?, val colour: Color)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class ExploreMapDelegate(
  private val onCameraIdle: (MapViewport) -> Unit,
) : NSObject(), GMSMapViewDelegateProtocol {
  private var centredOn: GeoLocation? = null
  private var styledWith: String? = null
  private val shown = mutableMapOf<StyledMarker, GMSMarker>()

  /** Fires once the camera settles — after gestures, animations and the first layout. */
  override fun mapView(mapView: GMSMapView, idleAtCameraPosition: GMSCameraPosition) {
    val bounds = GMSCoordinateBounds.create(region = mapView.projection.visibleRegion())
    val ne = bounds.northEast.useContents { this.latitude to this.longitude }
    val sw = bounds.southWest.useContents { this.latitude to this.longitude }
    onCameraIdle(
      MapViewport(
        GeoSquare(north = ne.first, south = sw.first, east = ne.second, west = sw.second),
        zoom = idleAtCameraPosition.zoom.toDouble(),
      ),
    )
  }

  /** update{} re-runs on every recomposition — restyle only when light/dark actually flips. */
  fun style(map: GMSMapView, json: String) {
    if (json == styledWith) return
    styledWith = json
    map.mapStyle = GMSMapStyle.styleWithJSONString(json, error = null)
  }

  /** Diff instead of clear-and-add, so unchanged markers don't flicker after each fetch. */
  fun show(map: GMSMapView, markers: List<StyledMarker>) {
    val next = markers.toSet()
    (shown.keys - next).forEach { shown.remove(it)!!.map = null }
    (next - shown.keys).forEach { styled ->
      shown[styled] = GMSMarker.markerWithPosition(styled.location()).apply {
        iconView = markerView(styled)
        groundAnchor = CGPointMake(0.5, 0.5)
        tracksViewChanges = false // static icon — don't re-snapshot the view every frame
        this.map = map
      }
    }
  }

  fun centre(map: GMSMapView, centre: GeoLocation?) {
    if (centre == null || centre == centredOn) return
    centredOn = centre
    map.setCamera(GMSCameraPosition.cameraWithLatitude(centre.latitude, centre.longitude, USER_ZOOM))
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun StyledMarker.location() = when (val m = marker) {
  is ExploreMarker.Dot -> m.location
  is ExploreMarker.Station -> m.station.location
}.let { CLLocationCoordinate2DMake(it.latitude, it.longitude) }

/**
 * UIKit twin of Android's StationPill / ClusterDot: a translucent halo with, centred on top,
 * a white pill (dot + label) for a station or a solid dot for a cluster.
 */
@OptIn(ExperimentalForeignApi::class)
private fun markerView(styled: StyledMarker): UIView {
  val colour = styled.colour.toUIColor()
  val label = styled.label
  val haloSize = if (label == null) 28.0 else 56.0
  val content = if (label == null) dot(16.0, colour) else pill(label, colour)
  val width = maxOf(haloSize, content.frame.useContents { size.width })
  val centre = CGPointMake(width / 2, haloSize / 2)
  return UIView(CGRectMake(0.0, 0.0, width, haloSize)).apply {
    addSubview(dot(haloSize, colour.colorWithAlphaComponent(0.4)).apply { setCenter(centre) })
    addSubview(content.apply { setCenter(centre) })
  }
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

// Same camera numbers as ExploreMap.android.kt — both platforms now speak Google zoom levels.
private val START = GeoLocation(39.8, -98.6) // contiguous US
private const val START_ZOOM = 3f
private const val USER_ZOOM = 10f

private fun Color.toUIColor() = UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
