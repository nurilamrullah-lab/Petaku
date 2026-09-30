package com.geoshift.locationsimulator.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Point
import android.location.Location
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.SimulationMode
import com.geoshift.locationsimulator.ui.theme.OutlineHairline
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SecondaryBlue
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerHigh
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.util.DeviceLocationHelper
import com.geoshift.locationsimulator.util.MapTileSources
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

enum class MapLayerOption(val title: String, val subtitle: String, val tileSource: ITileSource) {
    GOOGLE_ROAD("Google Maps (Grab/Gojek)", "Peta jalan resmi Google dengan POI lengkap", MapTileSources.GOOGLE_ROAD),
    GOOGLE_HYBRID("Google Satelit", "Citra satelit bumi dengan nama jalan", MapTileSources.GOOGLE_HYBRID),
    GOOGLE_TERRAIN("Google Terrain", "Kontur permukaan & topografi", MapTileSources.GOOGLE_TERRAIN),
    CARTO_DARK("Tactical Dark (CartoDB)", "Peta gelap kontras tinggi avionic", MapTileSources.CARTO_DARK),
    OSM_STANDARD("OpenStreetMap", "Peta open-source standar global", MapTileSources.OSM_STANDARD)
}

// Overlay untuk menggambar titik lokasi fisik real-time dengan animasi kedip-kedip bergelombang
class RealLocationPulseOverlay(
    var realLocation: Location?,
    var pulseFraction: Float = 0f
) : Overlay() {
    private val point = Point()

    override fun draw(c: android.graphics.Canvas, osmv: MapView, shadow: Boolean) {
        if (shadow) return
        val loc = realLocation ?: return
        val gp = GeoPoint(loc.latitude, loc.longitude)
        osmv.projection.toPixels(gp, point)

        val px = point.x.toFloat()
        val py = point.y.toFloat()

        // 1. Gelombang animasi membesar dan memudar (kedip-kedip)
        val maxWaveRadius = 55f
        val waveRadius = 14f + (pulseFraction * (maxWaveRadius - 14f))
        val waveAlpha = ((1f - pulseFraction) * 220).toInt().coerceIn(0, 255)

        val ripplePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.argb(waveAlpha, 255, 59, 48) // Merah cerah
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        c.drawCircle(px, py, waveRadius, ripplePaint)

        val haloPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.argb((waveAlpha * 0.35f).toInt(), 255, 59, 48)
            style = Paint.Style.FILL
        }
        c.drawCircle(px, py, waveRadius, haloPaint)

        // 2. Lingkaran luar solid merah
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        c.drawCircle(px, py, 14f, borderPaint)

        // 3. Inti merah terang
        val corePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.rgb(255, 59, 48)
            style = Paint.Style.FILL
        }
        c.drawCircle(px, py, 11f, corePaint)

        // 4. Titik pusat putih kecil
        val centerDot = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        c.drawCircle(px, py, 3.5f, centerDot)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalMapCanvas(
    location: LocationModel,
    mode: SimulationMode,
    realLocation: Location? = null,
    satellitesInUse: Int = 16,
    satellitesInView: Int = 24,
    modifier: Modifier = Modifier,
    onLocationSelected: (Double, Double, String, String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var selectedLayer by remember { mutableStateOf(MapLayerOption.GOOGLE_ROAD) }
    var showLayerPicker by remember { mutableStateOf(false) }

    // Animasi kedip-kedip bergelombang untuk lokasi real-time HP
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseFraction"
    )

    val realLocOverlay = remember { RealLocationPulseOverlay(realLocation, pulseFraction) }
    realLocOverlay.realLocation = realLocation
    realLocOverlay.pulseFraction = pulseFraction

    // Re-draw map canvas on each animation pulse frame
    LaunchedEffect(pulseFraction) {
        mapViewInstance?.postInvalidate()
    }

    // Update marker on location change
    LaunchedEffect(location.latitude, location.longitude) {
        val pt = GeoPoint(location.latitude, location.longitude)
        currentMarker?.let { m ->
            m.position = pt
            m.title = location.locationName.ifEmpty { "Target Location" }
            m.snippet = "Lat: ${"%.4f".format(location.latitude)}, Lng: ${"%.4f".format(location.longitude)}"
        }
        mapViewInstance?.controller?.animateTo(pt)
        mapViewInstance?.invalidate()
    }

    // Update tile source when user selects another layer
    LaunchedEffect(selectedLayer) {
        mapViewInstance?.setTileSource(selectedLayer.tileSource)
        mapViewInstance?.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(selectedLayer.tileSource)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(location.latitude, location.longitude))

                    // Overlay untuk klik dan long-press pada peta
                    val overlayEvents = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let {
                                onLocationSelected(
                                    it.latitude,
                                    it.longitude,
                                    "Titik Dipilih",
                                    "Koordinat: ${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}"
                                )
                            }
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            p?.let {
                                onLocationSelected(
                                    it.latitude,
                                    it.longitude,
                                    "Titik Ditandai",
                                    "Koordinat: ${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}"
                                )
                            }
                            return true
                        }
                    })
                    overlays.add(overlayEvents)

                    // Overlay real-time location (titik merah berkedip)
                    overlays.add(realLocOverlay)

                    // Target marker (pin lokasi yang dapat digeser-geser secara bebas)
                    val m = Marker(this).apply {
                        position = GeoPoint(location.latitude, location.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = location.locationName.ifEmpty { "Target Location" }
                        isDraggable = true
                        setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                            override fun onMarkerDrag(marker: Marker?) {}
                            override fun onMarkerDragStart(marker: Marker?) {}
                            override fun onMarkerDragEnd(marker: Marker?) {
                                marker?.let {
                                    onLocationSelected(
                                        it.position.latitude,
                                        it.position.longitude,
                                        "Pin Digeser",
                                        "Koordinat: ${"%.5f".format(it.position.latitude)}, ${"%.5f".format(it.position.longitude)}"
                                    )
                                }
                            }
                        })
                    }
                    overlays.add(m)
                    currentMarker = m
                    mapViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating right-side precision map controls (Centered vertically and shifted inwards to center)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zoom In (+)
            Surface(
                shape = CircleShape,
                color = SurfaceContainer.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, OutlineHairline),
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(
                    onClick = {
                        mapViewInstance?.let { map ->
                            val nextZoom = (map.zoomLevelDouble + 1.0).coerceAtMost(20.0)
                            map.controller.setZoom(nextZoom)
                            map.postInvalidate()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Zoom Out (-)
            Surface(
                shape = CircleShape,
                color = SurfaceContainer.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, OutlineHairline),
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(
                    onClick = {
                        mapViewInstance?.let { map ->
                            val nextZoom = (map.zoomLevelDouble - 1.0).coerceAtLeast(4.0)
                            map.controller.setZoom(nextZoom)
                            map.postInvalidate()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Lokasi Saya Asli (Real Device Hardware GPS)
            Surface(
                shape = CircleShape,
                color = SurfaceContainer.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.5.dp, PrimaryCyan),
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(
                    onClick = {
                        Toast.makeText(context, "Mencari sinyal GPS fisik perangkat...", Toast.LENGTH_SHORT).show()
                        DeviceLocationHelper.getRealDeviceLocation(context) { realLoc ->
                            if (realLoc != null) {
                                val pt = GeoPoint(realLoc.latitude, realLoc.longitude)
                                mapViewInstance?.controller?.animateTo(pt)
                                mapViewInstance?.controller?.setZoom(17.0)
                                onLocationSelected(
                                    realLoc.latitude,
                                    realLoc.longitude,
                                    "Lokasi GPS Asli",
                                    "Terdeteksi dari sensor GPS HP (Akurasi ±${"%.1f".format(realLoc.accuracy)}m)"
                                )
                                Toast.makeText(
                                    context,
                                    "Lokasi GPS HP: ${"%.4f".format(realLoc.latitude)}, ${"%.4f".format(realLoc.longitude)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val pt = GeoPoint(location.latitude, location.longitude)
                                mapViewInstance?.controller?.animateTo(pt)
                                Toast.makeText(context, "Sinyal GPS fisik belum terdeteksi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Lokasi Saya",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Pilihan Layer Peta (Google Maps Road, Satelit, Terrain, dll)
            Surface(
                shape = CircleShape,
                color = SurfaceContainer.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, OutlineHairline),
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(
                    onClick = { showLayerPicker = true },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Pilihan Peta",
                        tint = SecondaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Floating Satellite & Precision HUD Pill (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLowest.copy(alpha = 0.88f))
                .border(1.dp, OutlineHairline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WifiTethering,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$satellitesInUse/$satellitesInView SATS · ±${"%.1f".format(location.accuracy)}m",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Active Layer Badge (Bottom Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 12.dp, start = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceContainerLowest.copy(alpha = 0.82f))
                .border(1.dp, OutlineHairline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = selectedLayer.title,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Dialog Pemilihan Layer Peta
        if (showLayerPicker) {
            BasicAlertDialog(
                onDismissRequest = { showLayerPicker = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, OutlineHairline, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Pilih Gaya Peta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    MapLayerOption.values().forEach { option ->
                        val isSelected = option == selectedLayer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SurfaceContainerHigh else SurfaceContainerLowest)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryCyan else OutlineHairline,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedLayer = option
                                    showLayerPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.subtitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
