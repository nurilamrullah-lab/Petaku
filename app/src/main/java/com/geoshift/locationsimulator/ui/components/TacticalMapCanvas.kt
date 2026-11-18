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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
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
    // Paint dibuat sekali; draw() dipanggil setiap frame animasi
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(255, 59, 48)
        style = Paint.Style.FILL
    }

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

        ripplePaint.color = AndroidColor.argb(waveAlpha, 255, 59, 48)
        c.drawCircle(px, py, waveRadius, ripplePaint)
        haloPaint.color = AndroidColor.argb((waveAlpha * 0.35f).toInt(), 255, 59, 48)
        c.drawCircle(px, py, waveRadius, haloPaint)

        // 2. Lingkaran luar putih, inti merah, titik pusat putih
        c.drawCircle(px, py, 14f, whitePaint)
        c.drawCircle(px, py, 11f, corePaint)
        c.drawCircle(px, py, 3.5f, whitePaint)
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
    showStatusOverlays: Boolean = true,
    // Ruang yang dipakai kartu melayang di atas peta, agar rail kontrol tidak tertutup
    controlsPadding: PaddingValues = PaddingValues(top = 16.dp),
    // Tinggi area peta yang tertutup kartu atas/panel bawah; pin dipusatkan di area yang terlihat
    topObstruction: Dp = 0.dp,
    bottomObstruction: Dp = 0.dp,
    onDeviceLocation: (Location) -> Unit = {},
    onLocationSelected: (Double, Double, String, String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val pinColor = MaterialTheme.colorScheme.primary.toArgb()
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var pinOnScreen by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val visibleTopPx by rememberUpdatedState(with(density) { topObstruction.toPx() })
    val visibleBottomPx by rememberUpdatedState(with(density) { bottomObstruction.toPx() })
    val targetPoint by rememberUpdatedState(GeoPoint(location.latitude, location.longitude))

    // Pantau apakah pin target masih terlihat di area peta yang tidak tertutup panel
    DisposableEffect(mapViewInstance) {
        val mv = mapViewInstance ?: return@DisposableEffect onDispose {}
        val pt = Point()
        fun check() {
            if (mv.width == 0) return
            mv.projection.toPixels(targetPoint, pt)
            pinOnScreen = pt.x in 0..mv.width &&
                pt.y >= visibleTopPx && pt.y <= mv.height - visibleBottomPx
        }
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { check(); return false }
            override fun onZoom(event: ZoomEvent?): Boolean { check(); return false }
        }
        mv.addMapListener(listener)
        onDispose { mv.removeMapListener(listener) }
    }
    // Disimpan per nama agar pilihan layer tidak kembali ke default saat pindah tab
    var selectedLayerName by rememberSaveable { mutableStateOf(MapLayerOption.GOOGLE_ROAD.name) }
    val selectedLayer = MapLayerOption.valueOf(selectedLayerName)
    var showLayerPicker by remember { mutableStateOf(false) }

    // Animasi kedip-kedip bergelombang untuk lokasi real-time HP
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseFraction = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseFraction"
    )

    val realLocOverlay = remember { RealLocationPulseOverlay(realLocation) }
    realLocOverlay.realLocation = realLocation

    // Nilai animasi dibaca di coroutine (bukan saat komposisi) agar layar tidak
    // di-recompose setiap frame; cukup gambar ulang peta.
    LaunchedEffect(realLocOverlay) {
        snapshotFlow { pulseFraction.value }.collect { fraction ->
            realLocOverlay.pulseFraction = fraction
            if (realLocOverlay.realLocation != null) mapViewInstance?.postInvalidate()
        }
    }

    // Geser titik tengah peta ke tengah area yang tidak tertutup kartu/panel
    val centerOffsetPx = with(LocalDensity.current) { ((topObstruction - bottomObstruction) / 2).roundToPx() }
    LaunchedEffect(centerOffsetPx, mapViewInstance) {
        val mv = mapViewInstance ?: return@LaunchedEffect
        mv.setMapCenterOffset(0, centerOffsetPx)
        mv.controller.setCenter(GeoPoint(location.latitude, location.longitude))
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
                    // Tombol zoom bawaan osmdroid tertutup panel bawah & duplikat kontrol kanan
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
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
                        icon = createTargetPinDrawable(ctx, pinColor)
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

        MapControlRail(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(controlsPadding)
                .padding(end = 14.dp),
            onZoomIn = {
                mapViewInstance?.let { map ->
                    map.controller.zoomTo((map.zoomLevelDouble + 1.0).coerceAtMost(20.0), 250L)
                }
            },
            onZoomOut = {
                mapViewInstance?.let { map ->
                    map.controller.zoomTo((map.zoomLevelDouble - 1.0).coerceAtLeast(4.0), 250L)
                }
            },
            onMyLocation = {
                // Hanya menggeser kamera; pin target tidak ikut berubah
                DeviceLocationHelper.getRealDeviceLocation(context) { realLoc ->
                    if (realLoc != null) {
                        onDeviceLocation(realLoc)
                        mapViewInstance?.controller?.animateTo(GeoPoint(realLoc.latitude, realLoc.longitude), 17.0, 450L)
                    } else {
                        Toast.makeText(context, "Sinyal GPS HP belum terdeteksi", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onFocusTarget = {
                mapViewInstance?.controller?.animateTo(GeoPoint(location.latitude, location.longitude), 17.0, 450L)
            },
            showFocusTarget = !pinOnScreen,
            onLayers = { showLayerPicker = true }
        )

        // Floating Satellite & Precision HUD Pill (Top Left)
        if (showStatusOverlays) Box(
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
        if (showStatusOverlays) Box(
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

        // Sheet Pemilihan Layer Peta
        if (showLayerPicker) {
            ModalBottomSheet(
                onDismissRequest = { showLayerPicker = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Gaya Peta",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    MapLayerOption.entries.forEach { option ->
                        val isSelected = option == selectedLayer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryCyan else Color.Transparent,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    selectedLayerName = option.name
                                    showLayerPicker = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.subtitle,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
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

/** Pin target modern: tetesan berwarna aksen tema dengan inti putih dan bayangan lembut. */
private fun createTargetPinDrawable(context: Context, color: Int): android.graphics.drawable.BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val w = (40 * d).toInt()
    val h = (52 * d).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val cx = w / 2f
    val r = 15 * d
    val cy = r + 3 * d

    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.argb(60, 0, 0, 0) }
    c.drawOval(cx - 7 * d, h - 5 * d, cx + 7 * d, h - 1 * d, shadow)

    val path = android.graphics.Path().apply {
        moveTo(cx, h - 3 * d)
        cubicTo(cx - 4 * d, cy + r * 0.9f, cx - r, cy + r * 0.55f, cx - r, cy)
        arcTo(cx - r, cy - r, cx + r, cy + r, 180f, 180f, false)
        cubicTo(cx + r, cy + r * 0.55f, cx + 4 * d, cy + r * 0.9f, cx, h - 3 * d)
        close()
    }
    c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2 * d
    })
    c.drawCircle(cx, cy, 6 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.WHITE })
    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}
