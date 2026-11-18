package com.geoshift.locationsimulator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.ui.components.CoordinateInputDialog
import com.geoshift.locationsimulator.ui.components.JoystickPad
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.components.SimulationStatusBadge
import com.geoshift.locationsimulator.ui.components.StealthSettingsSheet
import com.geoshift.locationsimulator.ui.components.TacticalMapCanvas
import com.geoshift.locationsimulator.ui.components.pressScale
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.WarningAmber
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot

/** Kecepatan dasar engine rute (km/jam) yang dikalikan pengali kecepatan. */
private const val BASE_SPEED_KMH = 50f
private const val JOYSTICK_TICK_MS = 100L

@Composable
fun DashboardScreen(
    viewModel: GeoShiftViewModel,
    onNavigateToTab: (NavigationTab) -> Unit
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val simulationState by viewModel.simulationState.collectAsState()
    val simulationMode by viewModel.simulationMode.collectAsState()
    val realDeviceLocation by viewModel.realDeviceLocation.collectAsState()
    val fixInfo by viewModel.fixInfo.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val speedMultiplier by viewModel.speedMultiplier.collectAsState()

    val isFavorite = historyList.any {
        it.isFavorite &&
            abs(it.latitude - currentLocation.latitude) < 0.0001 &&
            abs(it.longitude - currentLocation.longitude) < 0.0001
    }
    var isExpandedSheet by rememberSaveable { mutableStateOf(true) }
    var joystickEnabled by rememberSaveable { mutableStateOf(false) }
    var showStealthSheet by remember { mutableStateOf(false) }
    var showCoordinateDialog by remember { mutableStateOf(false) }

    val isRunning = simulationState == SimulationState.RUNNING
    val isPaused = simulationState == SimulationState.PAUSED

    // Vektor joystick dibaca oleh loop di bawah; state biasa agar tidak memicu recomposition per gerakan
    val joystickVector = remember { floatArrayOf(0f, 0f) }
    val currentSpeed by androidx.compose.runtime.rememberUpdatedState(speedMultiplier)
    LaunchedEffect(joystickEnabled) {
        if (!joystickEnabled) return@LaunchedEffect
        while (true) {
            delay(JOYSTICK_TICK_MS)
            val (x, y) = joystickVector
            if (hypot(x, y) > 0.08f) {
                val metersPerTick = BASE_SPEED_KMH / 3.6f * currentSpeed * (JOYSTICK_TICK_MS / 1000f)
                viewModel.nudgeLocation((x * metersPerTick).toDouble(), (y * metersPerTick).toDouble())
            }
        }
    }

    val density = LocalDensity.current
    var topCardBottomPx by remember { mutableIntStateOf(0) }
    var bottomPanelHeightPx by remember { mutableIntStateOf(0) }
    val topObstruction = with(density) { topCardBottomPx.toDp() }
    val bottomObstruction = with(density) { bottomPanelHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. PETA PENUH LAYAR
        TacticalMapCanvas(
            modifier = Modifier.fillMaxSize(),
            location = currentLocation,
            mode = simulationMode,
            realLocation = realDeviceLocation,
            satellitesInUse = fixInfo.satellitesInUse,
            satellitesInView = fixInfo.satellitesInView,
            showStatusOverlays = false,
            controlsPadding = PaddingValues(top = topObstruction + 12.dp),
            topObstruction = topObstruction,
            bottomObstruction = bottomObstruction,
            onLocationSelected = { lat, lng, name, address ->
                viewModel.setLocation(lat, lng, name, address)
            }
        )

        // 2. KARTU LOKASI MELAYANG
        LocationHeaderCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .onGloballyPositioned { topCardBottomPx = it.boundsInParent().bottom.toInt() },
            simulationState = simulationState,
            title = currentLocation.locationName.ifEmpty { "Posisi Terpilih" },
            subtitle = "${"%.5f".format(currentLocation.latitude)}, ${"%.5f".format(currentLocation.longitude)}",
            satellites = "${fixInfo.satellitesInUse}/${fixInfo.satellitesInView}",
            isFavorite = isFavorite,
            onSearch = { onNavigateToTab(NavigationTab.SEARCH) },
            onToggleFavorite = {
                val saved = viewModel.toggleCurrentLocationFavorite()
                Toast.makeText(
                    context,
                    if (saved) "Disimpan ke Favorit (lihat tab Riwayat)" else "Dihapus dari Favorit",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        // 3. JOYSTICK + PANEL KONTROL
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { bottomPanelHeightPx = it.height }
        ) {
            AnimatedVisibility(
                visible = joystickEnabled,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    JoystickPad(onVectorChange = { x, y ->
                        joystickVector[0] = x
                        joystickVector[1] = y
                    })
                    if (!isRunning) {
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
                        ) {
                            Text(
                                "Mulai mock agar gerakan\njoystick terkirim ke GPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            ControlSheet(
                expanded = isExpandedSheet,
                onToggleExpanded = { isExpandedSheet = !isExpandedSheet },
                modeLabel = simulationMode.displayName,
                simulationState = simulationState,
                lat = currentLocation.latitude,
                lng = currentLocation.longitude,
                altitude = currentLocation.altitude,
                accuracy = currentLocation.accuracy,
                satellitesInUse = fixInfo.satellitesInUse,
                speedMultiplier = speedMultiplier,
                onSpeedChange = { viewModel.setSpeedMultiplier(it) },
                joystickEnabled = joystickEnabled,
                onJoystickChange = { joystickEnabled = it },
                onOpenStealth = { showStealthSheet = true },
                onEditCoordinate = { showCoordinateDialog = true },
                onCopyCoordinate = {
                    val text = "%.6f, %.6f".format(currentLocation.latitude, currentLocation.longitude)
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Koordinat", text))
                    Toast.makeText(context, "Koordinat disalin", Toast.LENGTH_SHORT).show()
                },
                onStart = { viewModel.startSimulation(context) },
                onPause = { viewModel.pauseSimulation() },
                onResume = { viewModel.resumeSimulation() },
                onStop = { viewModel.stopSimulation(context) },
                isPaused = isPaused
            )
        }
    }

    if (showStealthSheet) {
        StealthSettingsSheet(
            simulationMode = simulationMode,
            onOpenDiagnostic = { viewModel.openDiagnostic() },
            onDismiss = { showStealthSheet = false }
        )
    }

    if (showCoordinateDialog) {
        CoordinateInputDialog(
            initialLat = currentLocation.latitude,
            initialLng = currentLocation.longitude,
            onConfirm = { lat, lng ->
                viewModel.setManualCoordinate(lat, lng)
                showCoordinateDialog = false
            },
            onDismiss = { showCoordinateDialog = false }
        )
    }
}

@Composable
private fun LocationHeaderCard(
    simulationState: SimulationState,
    title: String,
    subtitle: String,
    satellites: String,
    isFavorite: Boolean,
    onSearch: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Cari lokasi", onClick = onSearch)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SimulationStatusBadge(simulationState)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$satellites SAT",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                AnimatedContent(
                    targetState = isFavorite,
                    transitionSpec = { scaleIn(spring(Spring.DampingRatioMediumBouncy)) togetherWith scaleOut() },
                    label = "FavIcon"
                ) { fav ->
                    Icon(
                        imageVector = if (fav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (fav) "Hapus dari favorit" else "Simpan ke favorit",
                        tint = if (fav) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalIconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = "Cari lokasi")
            }
        }
    }
}

@Composable
private fun ControlSheet(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modeLabel: String,
    simulationState: SimulationState,
    lat: Double,
    lng: Double,
    altitude: Double,
    accuracy: Float,
    satellitesInUse: Int,
    speedMultiplier: Float,
    onSpeedChange: (Float) -> Unit,
    joystickEnabled: Boolean,
    onJoystickChange: (Boolean) -> Unit,
    onOpenStealth: () -> Unit,
    onEditCoordinate: () -> Unit,
    onCopyCoordinate: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    isPaused: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Handle + header (ketuk untuk ciutkan/bentangkan)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = if (expanded) "Ciutkan panel" else "Bentangkan panel",
                        onClick = onToggleExpanded
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Kontrol Simulasi", style = MaterialTheme.typography.titleLarge)
                        Text(
                            modeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconToggleButton(
                        checked = joystickEnabled,
                        onCheckedChange = onJoystickChange,
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Outlined.SportsEsports,
                            contentDescription = if (joystickEnabled) "Matikan mode joystick" else "Nyalakan mode joystick"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = onOpenStealth, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Shield, contentDescription = "Pengaturan stealth & mock")
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Koordinat + metrik ringkas
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "%.6f, %.6f".format(lat, lng),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Elev %.0f m  ·  ±%.1f m  ·  %d sat".format(altitude, accuracy, satellitesInUse),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            IconButton(onClick = onCopyCoordinate, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin koordinat", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onEditCoordinate, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Ketik koordinat", modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    SpeedControl(speedMultiplier, onSpeedChange)
                }
            }

            PrimaryActions(
                simulationState = simulationState,
                isPaused = isPaused,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun SpeedControl(speedMultiplier: Float, onSpeedChange: (Float) -> Unit) {
    // Nilai lokal agar slider mulus saat digeser; disimpan ke pengaturan saat dilepas
    var sliderValue by remember(speedMultiplier) { mutableFloatStateOf(speedMultiplier.coerceIn(0.1f, 5f)) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Speed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Kecepatan simulasi", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "≈ %.0f km/j · %.1f×".format(BASE_SPEED_KMH * sliderValue, sliderValue),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onSpeedChange((sliderValue * 10).toInt() / 10f) },
            valueRange = 0.1f..5f
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Jalan", "Sepeda", "Motor", "Mobil").forEachIndexed { i, label ->
                val preset = floatArrayOf(0.12f, 0.4f, 1f, 1.5f)[i]
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (abs(sliderValue - preset) < 0.05f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            sliderValue = preset
                            onSpeedChange(preset)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}


@Composable
private fun PrimaryActions(
    simulationState: SimulationState,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val active = simulationState == SimulationState.RUNNING || isPaused
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val (label, icon, container, content) = when {
            !active -> Quad("Mulai Mock Lokasi", Icons.Filled.PlayArrow, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            isPaused -> Quad("Lanjutkan", Icons.Filled.PlayArrow, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            else -> Quad("Jeda", Icons.Filled.Pause, WarningAmber, Color(0xFF332000))
        }
        Button(
            onClick = {
                when {
                    !active -> onStart()
                    isPaused -> onResume()
                    else -> onPause()
                }
            },
            interactionSource = interaction,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .pressScale(pressed),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            AnimatedContent(
                targetState = label to icon,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
                label = "PrimaryActionContent"
            ) { (l, i) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(i, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(l, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = active,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            FilledIconButton(
                onClick = onStop,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = DestructiveRed,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Hentikan mock")
            }
        }
    }
}

private data class Quad(val label: String, val icon: ImageVector, val container: Color, val content: Color)
