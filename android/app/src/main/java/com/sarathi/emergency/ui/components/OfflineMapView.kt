package com.sarathi.emergency.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sarathi.emergency.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

/**
 * Marker data for the offline map.
 */
data class MapMarker(
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String = "",
    val markerColor: MarkerColor = MarkerColor.RED
)

enum class MarkerColor {
    RED, GREEN, BLUE, ORANGE, PURPLE
}

/**
 * Route line for the map.
 */
data class MapRoute(
    val points: List<Pair<Double, Double>>,
    val color: Int = android.graphics.Color.parseColor("#4F46E5"),
    val width: Float = 6f
)

/**
 * Reusable Offline Map composable built on OSMDroid.
 * Uses OpenStreetMap tiles, auto-caches for offline use.
 *
 * @param modifier Modifier for layout
 * @param centerLatitude Map center latitude
 * @param centerLongitude Map center longitude
 * @param zoomLevel Initial zoom (1–20), 15 = street level
 * @param markers List of markers to display
 * @param routes List of route polylines
 * @param showMyLocation Whether to show the user's GPS dot
 * @param myLatitude User's latitude
 * @param myLongitude User's longitude
 * @param showControls Whether to show zoom/layer buttons
 */
@Composable
fun OfflineMapView(
    modifier: Modifier = Modifier,
    centerLatitude: Double = 17.4426,
    centerLongitude: Double = 78.5006,
    zoomLevel: Double = 15.0,
    markers: List<MapMarker> = emptyList(),
    routes: List<MapRoute> = emptyList(),
    showMyLocation: Boolean = true,
    myLatitude: Double = 0.0,
    myLongitude: Double = 0.0,
    showControls: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Configure OSMDroid once
    LaunchedEffect(Unit) {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        // Enable tile caching for offline use
        config.osmdroidBasePath = context.getExternalFilesDir(null)
        config.osmdroidTileCache = context.getExternalFilesDir("osmdroid/tiles")
        // Larger cache for offline
        config.tileFileSystemCacheMaxBytes = 500L * 1024 * 1024 // 500 MB
        config.tileFileSystemCacheTrimBytes = 400L * 1024 * 1024 // trim at 400 MB
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    // Use OpenStreetMap Mapnik (standard street tiles)
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)

                    // Enable offline: use cached tiles when no network
                    setUseDataConnection(true) // download when online
                    isTilesScaledToDpi = true

                    // Set initial position
                    controller.setZoom(zoomLevel)
                    controller.setCenter(GeoPoint(centerLatitude, centerLongitude))

                    mapView = this
                }
            },
            update = { view ->
                // Clear old overlays
                view.overlays.clear()

                // Add my-location pulsing dot
                if (showMyLocation && myLatitude != 0.0 && myLongitude != 0.0) {
                    val myLocOverlay = object : Overlay() {
                        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                            if (shadow) return
                            val point = Point()
                            mapView.projection.toPixels(GeoPoint(myLatitude, myLongitude), point)

                            // Outer glow
                            val outerPaint = Paint().apply {
                                color = android.graphics.Color.parseColor("#334F46E5")
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            }
                            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 40f, outerPaint)

                            // Inner circle
                            val innerPaint = Paint().apply {
                                color = android.graphics.Color.parseColor("#4F46E5")
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            }
                            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 16f, innerPaint)

                            // White center
                            val whitePaint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            }
                            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 6f, whitePaint)
                        }
                    }
                    view.overlays.add(myLocOverlay)
                }

                // Add markers
                markers.forEach { markerData ->
                    val marker = Marker(view)
                    marker.position = GeoPoint(markerData.latitude, markerData.longitude)
                    marker.title = markerData.title
                    marker.snippet = markerData.snippet
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    view.overlays.add(marker)
                }

                // Add routes
                routes.forEach { route ->
                    val polyline = Polyline()
                    polyline.setPoints(route.points.map { GeoPoint(it.first, it.second) })
                    polyline.outlinePaint.color = route.color
                    polyline.outlinePaint.strokeWidth = route.width
                    polyline.outlinePaint.isAntiAlias = true
                    view.overlays.add(polyline)
                }

                // Animate to center
                view.controller.animateTo(GeoPoint(centerLatitude, centerLongitude))

                view.invalidate()
            }
        )

        // Map controls overlay
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Zoom In
                SmallFloatingActionButton(
                    onClick = { mapView?.controller?.zoomIn() },
                    containerColor = DarkNavy.copy(alpha = 0.85f),
                    contentColor = TextWhite
                ) {
                    Icon(Icons.Default.ZoomIn, "Zoom In", modifier = Modifier.size(18.dp))
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = { mapView?.controller?.zoomOut() },
                    containerColor = DarkNavy.copy(alpha = 0.85f),
                    contentColor = TextWhite
                ) {
                    Icon(Icons.Default.ZoomOut, "Zoom Out", modifier = Modifier.size(18.dp))
                }

                // Re-center
                if (showMyLocation && myLatitude != 0.0) {
                    SmallFloatingActionButton(
                        onClick = {
                            mapView?.controller?.animateTo(GeoPoint(myLatitude, myLongitude))
                            mapView?.controller?.setZoom(16.0)
                        },
                        containerColor = PrimaryBlue.copy(alpha = 0.85f),
                        contentColor = TextWhite
                    ) {
                        Icon(Icons.Default.MyLocation, "My Location", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // "OFFLINE READY" badge
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavy.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Layers, null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Street Map • Offline Ready",
                    color = SuccessGreen,
                    fontSize = 9.sp
                )
            }
        }
    }
}
