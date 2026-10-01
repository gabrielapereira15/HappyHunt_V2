package com.example.happyhunt.ui.map

import android.graphics.RectF
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.Kind
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.coalesce
import org.maplibre.android.style.expressions.Expression.color
import org.maplibre.android.style.expressions.Expression.concat
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.linear
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.match
import org.maplibre.android.style.expressions.Expression.neq
import org.maplibre.android.style.expressions.Expression.step
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.zoom
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textMaxWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.cos
import kotlin.math.sin

/** Where the map looks. */
data class CameraSpot(val center: GeoPoint, val zoom: Double)

/** A request to move the camera. A new [key] is a new request, even to the same place. */
data class CameraGoal(val key: Int, val center: GeoPoint, val radiusMeters: Int? = null, val zoom: Double? = null)

/** The place highlighted with a large pin. */
data class SelectedPin(val id: String, val point: GeoPoint, val kind: Kind)

object MapStyle {
    /** OpenFreeMap: free map tiles from OpenStreetMap data, with no key and no tracking. */
    fun url(dark: Boolean) = if (dark) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/positron"

    /** The style's own background, shown while tiles load, so the map never flashes another colour. */
    fun loadingColor(dark: Boolean): Int = if (dark) 0xFF0C0C0C.toInt() else 0xFFF2F3F0.toInt()

    /** Shown wherever the map is, as the tile licence asks (MapLibre's own button is turned off). */
    const val ATTRIBUTION = "© OpenMapTiles © OpenStreetMap contributors"

    /** Builds the points for the map, off the main thread when there are thousands. */
    fun pins(pins: List<Pin>): FeatureCollection = FeatureCollection.fromFeatures(
        pins.map { pin ->
            Feature.fromGeometry(Point.fromLngLat(pin.point.lon, pin.point.lat)).apply {
                addStringProperty(PROP_ID, pin.id)
                addStringProperty(PROP_CATEGORY, pin.kind.category.name)
                addStringProperty(PROP_KIND, pin.kind.name)
                pin.name?.let { addStringProperty(PROP_NAME, it) }
            }
        },
    )

    data class Pin(val id: String, val point: GeoPoint, val kind: Kind, val name: String?)

    internal const val PROP_ID = "id"
    internal const val PROP_CATEGORY = "category"
    internal const val PROP_KIND = "kind"
    internal const val PROP_NAME = "name"
}

private const val PINS = "hunt-pins"
private const val SELECTED = "hunt-selected"
private const val ORIGIN = "hunt-origin"
private const val RADIUS = "hunt-radius"
private const val LAYER_DOTS = "hunt-dots"
private const val LAYER_PINS = "hunt-pins"

/** Below this zoom each place is a small dot in its colour; from it on, a pin with its icon. */
private const val PIN_ZOOM = 15f
private const val LAYER_SELECTED = "hunt-selected"
private val BOLD = arrayOf("Noto Sans Bold")

/**
 * The OpenStreetMap map, through MapLibre. The places come in as GeoJSON and
 * are drawn by the map itself (clustered when they crowd), so thousands of
 * them stay smooth.
 */
@Composable
fun HuntMap(
    pins: FeatureCollection,
    selected: SelectedPin?,
    origin: GeoPoint?,
    radiusMeters: Int?,
    dark: Boolean,
    initialCamera: CameraSpot,
    goal: CameraGoal?,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onCameraIdle: (CameraSpot) -> Unit = {},
    onUserMovedMap: () -> Unit = {},
    onPinClick: (String) -> Unit = {},
    onMapClick: () -> Unit = {},
    onGoalHandled: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val colors = Hunt.colors
    val images = rememberMarkerImages()

    val mapView = remember {
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
            .attributionEnabled(false)
            .logoEnabled(false)
            .compassEnabled(false)
            .tiltGesturesEnabled(false)
            .foregroundLoadColor(MapStyle.loadingColor(dark))
            .camera(CameraPosition.Builder().target(initialCamera.center.toLatLng()).zoom(initialCamera.zoom).build())
        MapView(context, options).apply { onCreate(null) }
    }
    ForwardLifecycle(mapView)

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val currentPadding by rememberUpdatedState(padding.toPixels(density, layoutDirection))
    val idle by rememberUpdatedState(onCameraIdle)
    val moved by rememberUpdatedState(onUserMovedMap)
    val pinClick by rememberUpdatedState(onPinClick)
    val mapClick by rememberUpdatedState(onMapClick)
    val goalHandled by rememberUpdatedState(onGoalHandled)

    DisposableEffect(mapView) {
        mapView.getMapAsync { m ->
            m.uiSettings.apply {
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                setAllGesturesEnabled(interactive)
            }
            m.setMinZoomPreference(3.0)
            m.addOnCameraIdleListener {
                val position = m.cameraPosition
                position.target?.let { idle(CameraSpot(GeoPoint(it.latitude, it.longitude), position.zoom)) }
            }
            m.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) moved()
            }
            m.addOnMapClickListener { latLng ->
                if (!interactive) return@addOnMapClickListener false
                val point = m.projection.toScreenLocation(latLng)
                val reach = 16 * density.density
                val hits = m.queryRenderedFeatures(
                    RectF(point.x - reach, point.y - reach, point.x + reach, point.y + reach),
                    LAYER_SELECTED, LAYER_PINS, LAYER_DOTS,
                )
                // Of everything under the finger, the place closest to it.
                val hit = hits.minByOrNull { feature ->
                    val at = (feature.geometry() as? Point)?.let { m.projection.toScreenLocation(LatLng(it.latitude(), it.longitude())) }
                    if (at == null) Float.MAX_VALUE else (at.x - point.x) * (at.x - point.x) + (at.y - point.y) * (at.y - point.y)
                }
                val id = hit?.getStringProperty(MapStyle.PROP_ID)
                if (id == null) mapClick() else pinClick(id)
                true
            }
            map = m
        }
        onDispose { }
    }

    // A new theme means a new style; everything of ours is added to it again.
    LaunchedEffect(map, dark, images) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.setStyle(Style.Builder().fromUri(MapStyle.url(dark))) { loaded ->
            images.forEach { (name, bitmap) -> loaded.addImage(name, bitmap) }
            addLayers(loaded, colors)
            style = loaded
        }
    }
    LaunchedEffect(style, pins) {
        style?.getSourceAs<GeoJsonSource>(PINS)?.setGeoJson(pins)
    }
    LaunchedEffect(style, selected) {
        // The selected place shows as the big pin only, not also as its dot or small pin underneath.
        val others = neq(get(MapStyle.PROP_ID), literal(selected?.id.orEmpty()))
        style?.getLayerAs<SymbolLayer>(LAYER_PINS)?.setFilter(others)
        style?.getLayerAs<CircleLayer>(LAYER_DOTS)?.setFilter(others)
        val source = style?.getSourceAs<GeoJsonSource>(SELECTED) ?: return@LaunchedEffect
        if (selected == null) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        } else {
            source.setGeoJson(
                Feature.fromGeometry(Point.fromLngLat(selected.point.lon, selected.point.lat)).apply {
                    addStringProperty(MapStyle.PROP_ID, selected.id)
                    addStringProperty(MapStyle.PROP_KIND, selected.kind.name)
                },
            )
        }
    }
    LaunchedEffect(style, origin, radiusMeters) {
        val loaded = style ?: return@LaunchedEffect
        val empty = FeatureCollection.fromFeatures(emptyList())
        loaded.getSourceAs<GeoJsonSource>(ORIGIN)?.let { source ->
            if (origin == null) source.setGeoJson(empty) else source.setGeoJson(Point.fromLngLat(origin.lon, origin.lat))
        }
        loaded.getSourceAs<GeoJsonSource>(RADIUS)?.let { source ->
            if (origin == null || radiusMeters == null) source.setGeoJson(empty) else source.setGeoJson(circle(origin, radiusMeters))
        }
    }
    LaunchedEffect(map, goal) {
        val m = map ?: return@LaunchedEffect
        val target = goal ?: return@LaunchedEffect
        val update = if (target.radiusMeters != null) {
            val p = currentPadding
            CameraUpdateFactory.newLatLngBounds(bounds(target.center, target.radiusMeters), p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].toInt())
        } else {
            CameraUpdateFactory.newCameraPosition(position(target.center.toLatLng(), target.zoom ?: m.cameraPosition.zoom, currentPadding))
        }
        m.animateCamera(update, 600)
        goalHandled(target.key)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** MapView needs the screen's lifecycle passed on to it; leaving the screen destroys it. */
@Composable
private fun ForwardLifecycle(mapView: MapView) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, mapView) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart().also { started = true }
                Lifecycle.Event.ON_RESUME -> mapView.onResume().also { resumed = true }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause().also { resumed = false }
                Lifecycle.Event.ON_STOP -> mapView.onStop().also { started = false }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }
}

private fun addLayers(style: Style, colors: HuntColors) {
    val primary = colors.primary.toArgb()
    style.addSource(GeoJsonSource(RADIUS))
    style.addSource(GeoJsonSource(ORIGIN))
    style.addSource(GeoJsonSource(PINS, FeatureCollection.fromFeatures(emptyList())))
    style.addSource(GeoJsonSource(SELECTED))

    style.addLayer(FillLayer("hunt-radius-fill", RADIUS).withProperties(fillColor(color(primary)), fillOpacity(0.06f)))
    style.addLayer(
        LineLayer("hunt-radius-line", RADIUS).withProperties(
            lineColor(color(primary)),
            lineOpacity(0.45f),
            lineWidth(1.5f),
            lineDasharray(arrayOf(2f, 2f)),
        ),
    )
    style.addLayer(
        CircleLayer("hunt-origin-halo", ORIGIN).withProperties(circleColor(color(primary)), circleOpacity(0.18f), circleRadius(16f)),
    )
    style.addLayer(
        CircleLayer("hunt-origin", ORIGIN).withProperties(
            circleColor(color(primary)),
            circleRadius(7f),
            circleStrokeColor(color(android.graphics.Color.WHITE)),
            circleStrokeWidth(3f),
        ),
    )
    // Zoomed out, a confetti of dots, one colour per kind of outing: where the parks are, where the food is.
    val categoryColor = match(
        get(MapStyle.PROP_CATEGORY),
        color(primary),
        *Category.entries.map { stop(it.name, color(colors.category(it).pin.toArgb())) }.toTypedArray(),
    )
    style.addLayer(
        CircleLayer(LAYER_DOTS, PINS).withProperties(
            circleColor(categoryColor),
            circleRadius(interpolate(linear(), zoom(), stop(10, 3f), stop(13, 4.5f), stop(PIN_ZOOM, 7f))),
            circleStrokeColor(color(android.graphics.Color.WHITE)),
            circleStrokeWidth(interpolate(linear(), zoom(), stop(10, 1f), stop(PIN_ZOOM, 2f))),
        ).apply { maxZoom = PIN_ZOOM },
    )
    style.addLayer(
        SymbolLayer(LAYER_PINS, PINS).withProperties(
            iconImage(concat(literal("pin-"), get(MapStyle.PROP_KIND))),
            iconAllowOverlap(true),
            // Names appear once there is room for them, under their pins.
            textField(step(zoom(), literal(""), stop(15.5, coalesce(get(MapStyle.PROP_NAME), literal(""))))),
            textFont(BOLD),
            textSize(12f),
            textMaxWidth(8f),
            textAnchor(Property.TEXT_ANCHOR_TOP),
            textOffset(arrayOf(0f, 1.3f)),
            textOptional(true),
            textColor(color(colors.ink.toArgb())),
            textHaloColor(color(colors.surface.toArgb())),
            textHaloWidth(1.6f),
        ).apply { minZoom = PIN_ZOOM },
    )
    style.addLayer(
        SymbolLayer(LAYER_SELECTED, SELECTED).withProperties(
            iconImage(concat(literal("selected-"), get(MapStyle.PROP_KIND))),
            iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
}

private fun GeoPoint.toLatLng() = LatLng(lat, lon)

private fun position(target: LatLng, zoom: Double, padding: DoubleArray) =
    CameraPosition.Builder().target(target).zoom(zoom).padding(padding).build()

/** Left, top, right, bottom, in pixels. */
private fun PaddingValues.toPixels(density: Density, direction: LayoutDirection): DoubleArray = with(density) {
    doubleArrayOf(
        calculateLeftPadding(direction).toPx().toDouble(),
        calculateTopPadding().toPx().toDouble(),
        calculateRightPadding(direction).toPx().toDouble(),
        calculateBottomPadding().toPx().toDouble(),
    )
}

private fun PaddingValues.calculateLeftPadding(direction: LayoutDirection) =
    if (direction == LayoutDirection.Ltr) calculateStartPadding(direction) else calculateEndPadding(direction)

private fun PaddingValues.calculateRightPadding(direction: LayoutDirection) =
    if (direction == LayoutDirection.Ltr) calculateEndPadding(direction) else calculateStartPadding(direction)

private const val METERS_PER_DEGREE = 111_320.0

private fun bounds(center: GeoPoint, radiusMeters: Int): LatLngBounds {
    val dLat = radiusMeters / METERS_PER_DEGREE
    val dLon = radiusMeters / (METERS_PER_DEGREE * cos(Math.toRadians(center.lat)))
    return LatLngBounds.Builder()
        .include(LatLng(center.lat - dLat, center.lon - dLon))
        .include(LatLng(center.lat + dLat, center.lon + dLon))
        .build()
}

/** The search area as a polygon, for the dashed ring on the map. */
private fun circle(center: GeoPoint, radiusMeters: Int): Polygon {
    val dLat = radiusMeters / METERS_PER_DEGREE
    val dLon = radiusMeters / (METERS_PER_DEGREE * cos(Math.toRadians(center.lat)))
    val ring = (0..64).map { i ->
        val angle = 2 * Math.PI * i / 64
        Point.fromLngLat(center.lon + dLon * sin(angle), center.lat + dLat * cos(angle))
    }
    return Polygon.fromLngLats(listOf(ring))
}
