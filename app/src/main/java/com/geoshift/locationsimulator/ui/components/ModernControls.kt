package com.geoshift.locationsimulator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.geoshift.locationsimulator.GeoShiftApplication
import com.geoshift.locationsimulator.data.model.SimulationMode
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

// ==========================================
// STATUS BADGE (Running / Paused / Idle)
// ==========================================

enum class SimStatus(val label: String) { RUNNING("AKTIF"), PAUSED("DIJEDA"), IDLE("SIAGA") }

fun SimulationState.toSimStatus(): SimStatus = when (this) {
    SimulationState.RUNNING -> SimStatus.RUNNING
    SimulationState.PAUSED -> SimStatus.PAUSED
    else -> SimStatus.IDLE
}

@Composable
fun simStatusColor(status: SimStatus): Color = when (status) {
    SimStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
    SimStatus.PAUSED -> WarningAmber
    SimStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun SimulationStatusBadge(state: SimulationState, modifier: Modifier = Modifier) {
    val status = state.toSimStatus()
    val color by animateColorAsState(simStatusColor(status), label = "BadgeColor")
    val pulse = rememberInfiniteTransition(label = "BadgePulse")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "BadgeRing"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
            .semantics { contentDescription = "Status simulasi: ${status.label}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            val r = size.minDimension / 2
            if (status == SimStatus.RUNNING) {
                drawCircle(color.copy(alpha = (1f - ring) * 0.6f), radius = r * (0.6f + ring * 0.9f))
            }
            drawCircle(color, radius = r * 0.6f)
        }
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "BadgeLabel"
        ) { s ->
            Text(
                text = s.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ==========================================
// JOYSTICK PAD
// ==========================================

/**
 * Joystick virtual. [onVectorChange] menerima (x, y) ternormalisasi -1..1
 * (x ke kanan/timur, y ke atas/utara), dan (0,0) saat dilepas.
 */
@Composable
fun JoystickPad(
    onVectorChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 132.dp
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { (size / 2 - 22.dp).toPx() }
    val knob = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary
    val ringColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .semantics { contentDescription = "Joystick: geser untuk menggerakkan lokasi" }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        onVectorChange(0f, 0f)
                        scope.launch { knob.animateTo(Offset.Zero, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                    },
                    onDragCancel = {
                        onVectorChange(0f, 0f)
                        scope.launch { knob.animateTo(Offset.Zero) }
                    }
                ) { change, drag ->
                    change.consume()
                    var next = knob.value + drag
                    val dist = hypot(next.x, next.y)
                    if (dist > radiusPx) next = next * (radiusPx / dist)
                    scope.launch { knob.snapTo(next) }
                    onVectorChange(next.x / radiusPx, -next.y / radiusPx)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val c = center
            drawCircle(ringColor.copy(alpha = 0.35f), radius = radiusPx, center = c, style = Stroke(width = 1.5.dp.toPx()))
            val tick = 6.dp.toPx()
            listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
                drawCircle(ringColor.copy(alpha = 0.6f), radius = 2.dp.toPx(), center = c + d * (radiusPx + tick))
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(knob.value.x.roundToInt(), knob.value.y.roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(accent)
                .border(3.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        )
    }
}

// ==========================================
// MAP CONTROL RAIL
// ==========================================

@Composable
fun MapControlRail(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterPin: () -> Unit,
    onMyLocation: () -> Unit,
    onLayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingGroup {
            RailButton(Icons.Default.Add, "Perbesar peta", onZoomIn)
            HorizontalDivider(Modifier.width(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
            RailButton(Icons.Default.Remove, "Perkecil peta", onZoomOut)
        }
        FloatingGroup {
            RailButton(Icons.Default.GpsFixed, "Fokus ke pin target", onCenterPin, tint = MaterialTheme.colorScheme.primary)
            HorizontalDivider(Modifier.width(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
            RailButton(Icons.Default.MyLocation, "Lokasi GPS asli HP", onMyLocation, tint = MaterialTheme.colorScheme.secondary)
        }
        FloatingGroup {
            RailButton(Icons.Default.Layers, "Pilih gaya peta", onLayers)
        }
    }
}

@Composable
private fun FloatingGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun RailButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

// ==========================================
// STEALTH / MOCK SETTINGS SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthSettingsSheet(
    simulationMode: SimulationMode,
    onOpenDiagnostic: () -> Unit,
    onDismiss: () -> Unit
) {
    val repo = remember { GeoShiftApplication.instance.settingsRepository }
    val jitter by repo.jitterEnabled.collectAsState()
    val antiDetection by repo.antiDetectionHeuristics.collectAsState()
    val persistence by repo.backgroundPersistence.collectAsState()
    val notification by repo.notificationEnabled.collectAsState()
    val interval by repo.updateIntervalMs.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Stealth & Mock Lokasi", style = MaterialTheme.typography.titleLarge)
                    Text(
                        simulationMode.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsGroup {
                ToggleRow(Icons.Outlined.GraphicEq, "Atmospheric jitter", "Fluktuasi akurasi ±1.5 m agar terlihat alami", jitter) { repo.setJitterEnabled(it) }
                GroupDivider()
                ToggleRow(Icons.Outlined.Shield, "Heuristik anti-deteksi", "Sembunyikan jejak mock dari aplikasi target", antiDetection) { repo.setAntiDetectionHeuristics(it) }
                GroupDivider()
                ToggleRow(Icons.Outlined.Sync, "Tetap jalan di latar belakang", "Layanan simulasi bertahan saat app ditutup", persistence) { repo.setBackgroundPersistence(it) }
                GroupDivider()
                ToggleRow(Icons.Outlined.Notifications, "Notifikasi status", "Tampilkan kontrol cepat di panel notifikasi", notification) { repo.setNotificationEnabled(it) }
            }

            SettingsGroup {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Interval pembaruan", style = MaterialTheme.typography.titleSmall)
                            Text("Lebih cepat = lebih halus, lebih boros baterai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val options = listOf(250L, 500L, 1000L)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, ms ->
                            SegmentedButton(
                                selected = interval == ms,
                                onClick = { repo.setUpdateInterval(ms) },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size)
                            ) {
                                Text(if (ms < 1000) "${ms}ms" else "${ms / 1000}s", fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                        onOpenDiagnostic()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.BugReport, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Diagnostik perangkat")
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column { content() }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ==========================================
// COORDINATE INPUT DIALOG
// ==========================================

@Composable
fun CoordinateInputDialog(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var latText by remember { mutableStateOf("%.6f".format(java.util.Locale.US, initialLat)) }
    var lngText by remember { mutableStateOf("%.6f".format(java.util.Locale.US, initialLng)) }

    // Mendukung tempel "lat, lng" sekaligus di kolom latitude
    fun onLatChange(v: String) {
        val parts = v.split(',', ' ', ';').filter { it.isNotBlank() }
        if (parts.size == 2 && parts.all { it.trim().toDoubleOrNull() != null }) {
            latText = parts[0].trim(); lngText = parts[1].trim()
        } else latText = v
    }

    val lat = latText.trim().replace(',', '.').toDoubleOrNull()
    val lng = lngText.trim().replace(',', '.').toDoubleOrNull()
    val latOk = lat != null && lat in -90.0..90.0
    val lngOk = lng != null && lng in -180.0..180.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.GpsFixed, null) },
        title = { Text("Atur koordinat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Tempel \"lat, lng\" langsung di kolom latitude bila perlu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = latText,
                    onValueChange = ::onLatChange,
                    label = { Text("Latitude") },
                    singleLine = true,
                    isError = !latOk,
                    supportingText = { if (!latOk) Text("Rentang -90 sampai 90") },
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    isError = !lngOk,
                    supportingText = { if (!lngOk) Text("Rentang -180 sampai 180") },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = latOk && lngOk, onClick = { onConfirm(lat!!, lng!!) }) { Text("Terapkan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

/** Efek tekan "pegas" ringan untuk tombol utama. */
@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier {
    val s by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "PressScale"
    )
    return this.scale(s)
}
