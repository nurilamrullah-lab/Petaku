package com.geoshift.locationsimulator.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.geoshift.locationsimulator.data.model.RouteSimulationTrack
import com.geoshift.locationsimulator.ui.theme.CanvasBase
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.OutlineHairline
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.ui.theme.TertiaryGreen
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun InDriveRouteMapCanvas(
    originLat: Double,
    originLng: Double,
    destLat: Double?,
    destLng: Double?,
    simulatedLat: Double? = null,
    simulatedLng: Double? = null,
    realLocation: Location? = null,
    routeTrack: RouteSimulationTrack? = null,
    modifier: Modifier = Modifier,
    onOriginChanged: (Double, Double) -> Unit = { _, _ -> },
    onDestChanged: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var markerA by remember { mutableStateOf<Marker?>(null) }
    var markerB by remember { mutableStateOf<Marker?>(null) }
    var markerVehicle by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    // Animasi radar fisik
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulseRoute")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseFractionRoute"
    )

    val realLocOverlay = remember { RealLocationPulseOverlay(realLocation, pulseFraction) }
    realLocOverlay.realLocation = realLocation
    realLocOverlay.pulseFraction = pulseFraction

    LaunchedEffect(pulseFraction) {
        mapViewInstance?.postInvalidate()
    }

    // Google Maps Roadmap tile source
    val googleRoadTileSource = remember {
        object : OnlineTileSourceBase(
            "GoogleRoads", 0, 20, 256, ".png",
            arrayOf(
                "https://mt0.google.com/vt/lyrs=m&",
                "https://mt1.google.com/vt/lyrs=m&",
                "https://mt2.google.com/vt/lyrs=m&",
                "https://mt3.google.com/vt/lyrs=m&"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$baseUrl" + "x=$x&y=$y&z=$z"
            }
        }
    }

    // Update Marker A (Titik Jemput)
    LaunchedEffect(originLat, originLng) {
        val mv = mapViewInstance ?: return@LaunchedEffect
        val gp = GeoPoint(originLat, originLng)
        if (markerA == null) {
            val mA = Marker(mv).apply {
                position = gp
                title = "Titik Jemput (A)"
                snippet = "Tahan & geser untuk ubah lokasi jemput"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = createLetterPinDrawable(context, "A", AndroidColor.parseColor("#10B981"))
                isDraggable = true
                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(m: Marker?) {}
                    override fun onMarkerDragStart(m: Marker?) {}
                    override fun onMarkerDragEnd(m: Marker?) {
                        m?.position?.let { onOriginChanged(it.latitude, it.longitude) }
                    }
                })
            }
            mv.overlays.add(mA)
            markerA = mA
        } else {
            markerA?.position = gp
        }
        mv.invalidate()
    }

    // Update Marker B (Titik Tujuan)
    LaunchedEffect(destLat, destLng) {
        val mv = mapViewInstance ?: return@LaunchedEffect
        if (destLat != null && destLng != null) {
            val gp = GeoPoint(destLat, destLng)
            if (markerB == null) {
                val mB = Marker(mv).apply {
                    position = gp
                    title = "Titik Tujuan (B)"
                    snippet = "Tahan & geser untuk ubah lokasi tujuan"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createLetterPinDrawable(context, "B", AndroidColor.parseColor("#EF4444"))
                    isDraggable = true
                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(m: Marker?) {}
                        override fun onMarkerDragStart(m: Marker?) {}
                        override fun onMarkerDragEnd(m: Marker?) {
                            m?.position?.let { onDestChanged(it.latitude, it.longitude) }
                        }
                    })
                }
                mv.overlays.add(mB)
                markerB = mB
            } else {
                markerB?.position = gp
            }
        } else {
            // Jika dest dihapus/null
            markerB?.let {
                mv.overlays.remove(it)
                markerB = null
            }
        }
        mv.invalidate()
    }

    // Update Jalur Rute (Polyline)
    LaunchedEffect(routeTrack) {
        val mv = mapViewInstance ?: return@LaunchedEffect
        routePolyline?.let { mv.overlays.remove(it) }

        if (routeTrack != null && routeTrack.waypoints.isNotEmpty() && destLat != null && destLng != null) {
            val polyline = Polyline(mv).apply {
                outlinePaint.color = AndroidColor.parseColor("#00E5FF")
                outlinePaint.strokeWidth = 14f
                outlinePaint.isAntiAlias = true
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                setPoints(routeTrack.waypoints.map { GeoPoint(it.latitude, it.longitude) })
            }
            mv.overlays.add(0, polyline)
            routePolyline = polyline

            // Zoom pas mencakup titik A dan B
            val minLat = minOf(originLat, destLat)
            val maxLat = maxOf(originLat, destLat)
            val minLng = minOf(originLng, destLng)
            val maxLng = maxOf(originLng, destLng)
            mv.zoomToBoundingBox(org.osmdroid.util.BoundingBox(maxLat + 0.008, maxLng + 0.008, minLat - 0.008, minLng - 0.008), true)
        } else {
            routePolyline = null
        }
        mv.invalidate()
    }

    // Update Posisi Fake GPS Berjalan
    LaunchedEffect(simulatedLat, simulatedLng) {
        val mv = mapViewInstance ?: return@LaunchedEffect
        if (simulatedLat != null && simulatedLng != null) {
            val gp = GeoPoint(simulatedLat, simulatedLng)
            if (markerVehicle == null) {
                val mV = Marker(mv).apply {
                    position = gp
                    title = "Posisi Fake GPS"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createVehicleMarkerDrawable(context)
                }
                mv.overlays.add(mV)
                markerVehicle = mV
            } else {
                markerVehicle?.position = gp
            }
            mv.invalidate()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(googleRoadTileSource)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(originLat, originLng))

                    // Overlay lokasi fisik HP berkedip
                    overlays.add(realLocOverlay)

                    // Event tap langsung di peta untuk menentukan tujuan
                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onDestChanged(p.latitude, p.longitude)
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onDestChanged(p.latitude, p.longitude)
                                return true
                            }
                            return false
                        }
                    }
                    overlays.add(MapEventsOverlay(mapEventsReceiver))

                    mapViewInstance = this
                }
            },
            update = { mv ->
                mapViewInstance = mv
            }
        )

        // Tombol Navigasi Cepat & Zoom di Kanan Tengah
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom In (+)
            SmallFloatingActionButton(
                onClick = {
                    mapViewInstance?.let { map ->
                        val nextZoom = (map.zoomLevelDouble + 1.0).coerceAtMost(map.maxZoomLevel)
                        map.controller.setZoom(nextZoom)
                        map.postInvalidate()
                    }
                },
                containerColor = SurfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Zoom Out (-)
            SmallFloatingActionButton(
                onClick = {
                    mapViewInstance?.let { map ->
                        val nextZoom = (map.zoomLevelDouble - 1.0).coerceAtLeast(map.minZoomLevel)
                        map.controller.setZoom(nextZoom)
                        map.postInvalidate()
                    }
                },
                containerColor = SurfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Tombol Fokus ke Titik A (Jemput)
            SmallFloatingActionButton(
                onClick = {
                    mapViewInstance?.controller?.animateTo(GeoPoint(originLat, originLng), 16.0, 500L)
                },
                containerColor = TertiaryGreen,
                contentColor = CanvasBase,
                modifier = Modifier.size(36.dp)
            ) {
                Text("A", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (destLat != null && destLng != null) {
                Spacer(modifier = Modifier.size(8.dp))
                // Tombol Fokus ke Titik B (Tujuan)
                SmallFloatingActionButton(
                    onClick = {
                        mapViewInstance?.controller?.animateTo(GeoPoint(destLat, destLng), 16.0, 500L)
                    },
                    containerColor = DestructiveRed,
                    contentColor = CanvasBase,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("B", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Tombol Fokus ke GPS Asli
            if (realLocation != null) {
                SmallFloatingActionButton(
                    onClick = {
                        mapViewInstance?.controller?.animateTo(GeoPoint(realLocation.latitude, realLocation.longitude), 16.5, 500L)
                    },
                    containerColor = PrimaryCyan,
                    contentColor = CanvasBase,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = "GPS Asli", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun createLetterPinDrawable(context: Context, letter: String, pinColor: Int): BitmapDrawable {
    val width = 72
    val height = 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        isAntiAlias = true
        color = pinColor
        style = Paint.Style.FILL
    }

    val shadowPaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.argb(70, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f, height - 8f, 10f, shadowPaint)

    val path = Path()
    val r = 30f
    val cx = width / 2f
    val cy = 34f
    path.moveTo(cx, height - 12f)
    path.quadTo(cx - 24f, cy + 20f, cx - r, cy)
    path.arcTo(cx - r, cy - r, cx + r, cy + r, 180f, 180f, false)
    path.quadTo(cx + 24f, cy + 20f, cx, height - 12f)
    path.close()
    canvas.drawPath(path, paint)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, 18f, paint)

    paint.color = pinColor
    paint.textSize = 24f
    paint.textAlign = Paint.Align.CENTER
    paint.isFakeBoldText = true
    canvas.drawText(letter, cx, cy + 8f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createVehicleMarkerDrawable(context: Context): BitmapDrawable {
    val size = 56
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val haloPaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.argb(80, 0, 229, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 26f, haloPaint)

    val corePaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 16f, corePaint)

    val whitePaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 6f, whitePaint)

    return BitmapDrawable(context.resources, bitmap)
}
