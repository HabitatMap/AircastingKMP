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
import pl.llp.aircasting.domain.ExploreSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.GeoSquare
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.ui.theme.LocalAqColors
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSAttributedString
import platform.Foundation.NSBundle
import platform.Foundation.create
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.NSKernAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UIImage
import platform.UIKit.UIImageRenderingMode
import platform.UIKit.UIImageView
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
  initialCamera: MapViewport?,
  startCentre: GeoLocation?,
  flyTo: GeoLocation?,
  onFlownTo: () -> Unit,
  onCameraIdle: (MapViewport) -> Unit,
  onStationClick: (uuid: String) -> Unit,
  modifier: Modifier,
) {
  val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
  val currentOnStationClick by rememberUpdatedState(onStationClick)
  val delegate = remember {
    ExploreMapDelegate(
      onCameraIdle = { currentOnCameraIdle(it) },
      onStationTap = { currentOnStationClick(it) },
    )
  }
  // Pinned at first composition: later idles update state.camera, but must not move this map.
  val restored = remember { initialCamera }
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
      val camera = restored?.let { GMSCameraPosition.cameraWithLatitude(it.centre.latitude, it.centre.longitude, it.zoom.toFloat()) }
        ?: GMSCameraPosition.cameraWithLatitude(START.latitude, START.longitude, START_ZOOM)
      GMSMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), camera = camera).apply {
        this.delegate = delegate
      }
    },
    modifier = modifier,
    update = { map ->
      delegate.style(map, style)
      delegate.show(map, styled)
      if (restored == null) delegate.centre(map, startCentre) // re-created map: stay where the user left it
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
internal val apiKeyProvided: Boolean by lazy {
  GMSServices.provideAPIKey(NSBundle.mainBundle.objectForInfoDictionaryKey("GMSApiKey") as? String ?: "")
}

/** A marker with its look resolved in Compose — UIKit only sees text and a colour. */
private data class StyledMarker(val marker: ExploreMarker, val label: String?, val colour: Color)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class ExploreMapDelegate(
  private val onCameraIdle: (MapViewport) -> Unit,
  private val onStationTap: (uuid: String) -> Unit,
) : NSObject(), GMSMapViewDelegateProtocol {
  private var centredOn: GeoLocation? = null
  private var styledWith: String? = null
  private val shown = mutableMapOf<StyledMarker, GMSMarker>()

  /** Fires once the camera settles — after gestures, animations and the first layout. */
  @ObjCSignatureOverride
  override fun mapView(mapView: GMSMapView, idleAtCameraPosition: GMSCameraPosition) {
    val bounds = GMSCoordinateBounds.create(region = mapView.projection.visibleRegion())
    val ne = bounds.northEast.useContents { this.latitude to this.longitude }
    val sw = bounds.southWest.useContents { this.latitude to this.longitude }
    val centre = idleAtCameraPosition.target.useContents { GeoLocation(latitude, longitude) }
    onCameraIdle(
      MapViewport(
        GeoSquare(north = ne.first, south = sw.first, east = ne.second, west = sw.second),
        zoom = idleAtCameraPosition.zoom.toDouble(),
        centre = centre,
      ),
    )
  }

  /** Station pills carry their uuid in userData; dots don't, so they keep the SDK's default tap. */
  @ObjCSignatureOverride
  override fun mapView(mapView: GMSMapView, didTapMarker: GMSMarker): Boolean {
    val uuid = didTapMarker.userData as? String ?: return false
    onStationTap(uuid)
    return true
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
        userData = (styled.marker as? ExploreMarker.Station)?.station?.uuid
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
 * UIKit twin of StationPill (ui/components) / ClusterDot: a translucent halo with a white pill
 * (dot or AirBeam glyph + label) for a station or a solid dot for a cluster. The halo is centred on
 * the pill's marker, and the view is padded symmetric around it so groundAnchor 0.5/0.5 lands the
 * marker on the station — same layout as StationPill.
 */
@OptIn(ExperimentalForeignApi::class)
private fun markerView(styled: StyledMarker): UIView {
  val colour = styled.colour.toUIColor()
  val label = styled.label
  val haloSize = if (label == null) 48.0 else 56.0 * SCALE // same as ClusterDot / StationPill
  val airBeam = (styled.marker as? ExploreMarker.Station)?.station?.source == ExploreSource.AirBeam
  val content = if (label == null) dot(24.0, colour) else pill(label, colour, airBeam)
  val (contentWidth, contentHeight) = content.frame.useContents { size.width to size.height }
  val markerX = if (label == null) contentWidth / 2 else PAD_START + (if (airBeam) GLYPH_W else DOT) / 2
  val half = maxOf(haloSize / 2, markerX, contentWidth - markerX)
  val height = maxOf(haloSize, contentHeight)
  return UIView(CGRectMake(0.0, 0.0, half * 2, height)).apply {
    addSubview(dot(haloSize, colour.colorWithAlphaComponent(0.4)).apply { setCenter(CGPointMake(half, height / 2)) })
    addSubview(content.apply { setCenter(CGPointMake(half - markerX + contentWidth / 2, height / 2)) })
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun pill(text: String, colour: UIColor, airBeam: Boolean): UIView {
  // Label Small (11 pt medium, +0.5 tracking) scaled by SCALE, in on-primary-container (#004059 in both schemes).
  val label = UILabel().apply {
    attributedText = NSAttributedString.create(
      string = text,
      attributes = mapOf<Any?, Any?>(
        NSFontAttributeName to UIFont.systemFontOfSize(11.0 * SCALE, UIFontWeightMedium),
        NSKernAttributeName to 0.5 * SCALE,
        NSForegroundColorAttributeName to LABEL_COLOUR,
      ),
    )
    sizeToFit()
  }
  val textWidth = label.frame.useContents { size.width }
  // The glyph: a template image (iosApp Assets.xcassets/AirBeamMarker), so tintColor paints it.
  val marker = if (airBeam) {
    UIImageView(UIImage.imageNamed("AirBeamMarker")?.imageWithRenderingMode(UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate)).apply {
      tintColor = colour
      setFrame(CGRectMake(0.0, 0.0, GLYPH_W, GLYPH_H))
    }
  } else dot(DOT, colour)
  val markerWidth = if (airBeam) GLYPH_W else DOT
  val width = PAD_START + markerWidth + GAP + textWidth + PAD_END
  val height = PAD_Y + LINE + PAD_Y
  return UIView(CGRectMake(0.0, 0.0, width, height)).apply {
    backgroundColor = UIColor.whiteColor
    layer.cornerRadius = height / 2
    layer.shadowColor = UIColor.blackColor.CGColor
    layer.shadowOpacity = 0.3f
    layer.shadowRadius = 1.0
    layer.shadowOffset = CGSizeMake(0.0, 1.0)
    addSubview(marker.apply { setCenter(CGPointMake(PAD_START + markerWidth / 2, height / 2)) })
    addSubview(label.apply { setFrame(CGRectMake(PAD_START + markerWidth + GAP, PAD_Y, textWidth, LINE)) })
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun dot(size: Double, colour: UIColor) = UIView(CGRectMake(0.0, 0.0, size, size)).apply {
  backgroundColor = colour
  layer.cornerRadius = size / 2
}

// Pill metrics in points — same numbers as the dp values in StationPill.kt (Figma size × SCALE).
private const val SCALE = 0.8
private const val PAD_START = 12.0 * SCALE
private const val PAD_END = 10.0 * SCALE
private const val PAD_Y = 6.0 * SCALE
private const val DOT = 12.0 * SCALE
private const val GAP = 4.0 * SCALE
private const val GLYPH_W = 14.0 * SCALE // ic_airbeam_marker
private const val GLYPH_H = 16.0 * SCALE
private const val LINE = 16.0 * SCALE // labelSmall line height

@OptIn(ExperimentalForeignApi::class)
private val LABEL_COLOUR = UIColor(red = 0x00 / 255.0, green = 0x40 / 255.0, blue = 0x59 / 255.0, alpha = 1.0)

// Same camera numbers as ExploreMap.android.kt — both platforms now speak Google zoom levels.
private val START = GeoLocation(39.8, -98.6) // contiguous US
private const val START_ZOOM = 3f
private const val USER_ZOOM = 10f

private fun Color.toUIColor() = UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
