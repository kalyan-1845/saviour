package com.sarathi.emergency.ui.components

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sarathi.emergency.ui.theme.DarkNavy
import com.sarathi.emergency.ui.theme.PrimaryBlue
import com.sarathi.emergency.ui.theme.SuccessGreen
import com.sarathi.emergency.ui.theme.TextWhite

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

data class MapRoute(
    val points: List<Pair<Double, Double>>,
    val color: Int = android.graphics.Color.parseColor("#4F46E5"),
    val width: Float = 6f
)

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
    val centerPoint = remember(centerLatitude, centerLongitude) { LatLng(centerLatitude, centerLongitude) }
    val livePoint = remember(myLatitude, myLongitude) { LatLng(myLatitude, myLongitude) }
    val focusPoint = if (showMyLocation && myLatitude != 0.0 && myLongitude != 0.0) livePoint else centerPoint

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerPoint, zoomLevel.toFloat())
    }

    LaunchedEffect(centerLatitude, centerLongitude, zoomLevel) {
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(centerPoint, zoomLevel.toFloat()))
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var showStreetView by remember { mutableStateOf(false) }

    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        if (showStreetView) {
            InAppStreetView(
                modifier = Modifier.fillMaxSize(),
                target = focusPoint
            )
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = mapType,
                    isTrafficEnabled = true,
                    isBuildingEnabled = true
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = true
                )
            ) {
                if (showMyLocation && myLatitude != 0.0 && myLongitude != 0.0) {
                    Marker(
                        state = MarkerState(position = livePoint),
                        title = "My Location",
                        snippet = "Live GPS",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                markers.forEach { marker ->
                    Marker(
                        state = MarkerState(position = LatLng(marker.latitude, marker.longitude)),
                        title = marker.title,
                        snippet = marker.snippet,
                        icon = BitmapDescriptorFactory.defaultMarker(marker.markerColor.toHue())
                    )
                }

                routes.forEach { route ->
                    Polyline(
                        points = route.points.map { LatLng(it.first, it.second) },
                        color = Color(route.color),
                        width = route.width
                    )
                }
            }
        }

        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { cameraPositionState.move(CameraUpdateFactory.zoomIn()) },
                    containerColor = DarkNavy.copy(alpha = 0.85f),
                    contentColor = TextWhite
                ) {
                    Icon(Icons.Default.ZoomIn, "Zoom In", modifier = Modifier.size(18.dp))
                }

                SmallFloatingActionButton(
                    onClick = { cameraPositionState.move(CameraUpdateFactory.zoomOut()) },
                    containerColor = DarkNavy.copy(alpha = 0.85f),
                    contentColor = TextWhite
                ) {
                    Icon(Icons.Default.ZoomOut, "Zoom Out", modifier = Modifier.size(18.dp))
                }

                SmallFloatingActionButton(
                    onClick = {
                        mapType = when (mapType) {
                            MapType.NORMAL -> MapType.HYBRID
                            MapType.HYBRID -> MapType.SATELLITE
                            else -> MapType.NORMAL
                        }
                    },
                    containerColor = DarkNavy.copy(alpha = 0.85f),
                    contentColor = TextWhite
                ) {
                    Icon(Icons.Default.Layers, "Map Type", modifier = Modifier.size(18.dp))
                }

                SmallFloatingActionButton(
                    onClick = { showStreetView = !showStreetView },
                    containerColor = DarkNavy.copy(alpha = 0.85f),
                    contentColor = TextWhite
                ) {
                    Icon(Icons.Default.LocationSearching, "Street View", modifier = Modifier.size(18.dp))
                }

                if (showMyLocation && myLatitude != 0.0 && myLongitude != 0.0) {
                    SmallFloatingActionButton(
                        onClick = {
                            showStreetView = false
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(livePoint, 17f))
                        },
                        containerColor = PrimaryBlue.copy(alpha = 0.85f),
                        contentColor = TextWhite
                    ) {
                        Icon(Icons.Default.MyLocation, "My Location", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

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
                Icon(Icons.Default.Traffic, null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (showStreetView) "Street View In-App" else "Google Maps Traffic ON",
                    color = SuccessGreen,
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun MarkerColor.toHue(): Float {
    return when (this) {
        MarkerColor.RED -> BitmapDescriptorFactory.HUE_RED
        MarkerColor.GREEN -> BitmapDescriptorFactory.HUE_GREEN
        MarkerColor.BLUE -> BitmapDescriptorFactory.HUE_AZURE
        MarkerColor.ORANGE -> BitmapDescriptorFactory.HUE_ORANGE
        MarkerColor.PURPLE -> BitmapDescriptorFactory.HUE_VIOLET
    }
}

@Composable
private fun InAppStreetView(
    modifier: Modifier,
    target: LatLng
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var streetView by remember { mutableStateOf<StreetViewPanoramaView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> streetView?.onResume()
                Lifecycle.Event.ON_PAUSE -> streetView?.onPause()
                Lifecycle.Event.ON_DESTROY -> streetView?.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            streetView?.onPause()
            streetView?.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            StreetViewPanoramaView(
                context,
                StreetViewPanoramaOptions().position(target)
            ).apply {
                onCreate(Bundle())
                onResume()
                getStreetViewPanoramaAsync { panorama ->
                    panorama.setPosition(target)
                    panorama.isStreetNamesEnabled = true
                    panorama.isUserNavigationEnabled = true
                    panorama.isZoomGesturesEnabled = true
                    panorama.isPanningGesturesEnabled = true
                }
                streetView = this
            }
        },
        update = { view ->
            view.getStreetViewPanoramaAsync { panorama ->
                panorama.setPosition(target)
            }
        }
    )
}
