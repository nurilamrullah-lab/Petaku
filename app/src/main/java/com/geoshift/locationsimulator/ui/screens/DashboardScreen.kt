package com.geoshift.locationsimulator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.ui.components.CoordinateInputDialog
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.components.SimulationStatusBadge
import com.geoshift.locationsimulator.ui.components.StealthSettingsSheet
import com.geoshift.locationsimulator.ui.components.TacticalMapCanvas
import com.geoshift.locationsimulator.ui.components.pressScale
import com.geoshift.locationsimulator.ui.theme.WarningAmber
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import kotlin.math.abs

/**
 * Menu Peta: khusus mock lokasi DIAM di satu titik. Simulasi jalan, kecepatan
 * dan joystick ada di menu Rute.
 */
@Composable
fun DashboardScreen(
    viewModel: GeoShiftViewModel,
    onNavigateToTab: (NavigationTab) -> Unit
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val simulationState by viewModel.simulationState.collectAsState()
    val simulationMode by viewModel.simulationMode.collectAsState()
    val routeActive by viewModel.routeActive.collectAsState()
    val realDeviceLocation by viewModel.realDeviceLocation.collectAsState()
    val fixInfo by viewModel.fixInfo.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    val isFavorite = historyList.any {
        it.isFavorite &&
            abs(it.latitude - currentLocation.latitude) < 0.0001 &&
            abs(it.longitude - currentLocation.longitude) < 0.0001
    }
    var showStealthSheet by remember { mutableStateOf(false) }
    var showCoordinateDialog by remember { mutableStateOf(false) }

    val isMockActive = simulationState == SimulationState.RUNNING || simulationState == SimulationState.PAUSED
    // Simulasi rute/joystick dikelola di menu Rute; jangan pindahkan pinnya dari sini
    val routeRunning = isMockActive && routeActive

    val density = LocalDensity.current
    var topCardBottomPx by remember { mutableIntStateOf(0) }
    var bottomPanelHeightPx by remember { mutableIntStateOf(0) }
    val topObstruction = with(density) { topCardBottomPx.toDp() }
    val bottomObstruction = with(density) { bottomPanelHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
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
            onDeviceLocation = { viewModel.setRealDeviceLocation(it) },
            onLocationSelected = { lat, lng, name, address ->
                if (routeRunning) {
                    Toast.makeText(context, "Simulasi rute sedang berjalan. Hentikan dulu di menu Rute.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setLocation(lat, lng, name, address)
                }
            }
        )

        LocationHeaderCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .onGloballyPositioned { topCardBottomPx = it.boundsInParent().bottom.toInt() },
            simulationState = simulationState,
            title = currentLocation.locationName.ifEmpty { "Posisi Terpilih" },
            subtitle = currentLocation.address.ifEmpty { "Ketuk peta atau geser pin untuk memilih titik" },
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

        StaticMockPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { bottomPanelHeightPx = it.height },
            isMockActive = isMockActive,
            routeRunning = routeRunning,
            modeLabel = simulationMode.displayName,
            lat = currentLocation.latitude,
            lng = currentLocation.longitude,
            accuracy = currentLocation.accuracy,
            onOpenStealth = { showStealthSheet = true },
            onOpenRoutes = { onNavigateToTab(NavigationTab.ROUTES) },
            onEditCoordinate = {
                if (routeRunning) {
                    Toast.makeText(context, "Hentikan simulasi rute dulu", Toast.LENGTH_SHORT).show()
                } else {
                    showCoordinateDialog = true
                }
            },
            onCopyCoordinate = {
                val text = "%.6f, %.6f".format(currentLocation.latitude, currentLocation.longitude)
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Koordinat", text))
                Toast.makeText(context, "Koordinat disalin", Toast.LENGTH_SHORT).show()
            },
            onActivate = { viewModel.startStaticMock(context) },
            onStop = { viewModel.stopSimulation(context) }
        )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
private fun StaticMockPanel(
    isMockActive: Boolean,
    routeRunning: Boolean,
    modeLabel: String,
    lat: Double,
    lng: Double,
    accuracy: Float,
    onOpenStealth: () -> Unit,
    onOpenRoutes: () -> Unit,
    onEditCoordinate: () -> Unit,
    onCopyCoordinate: () -> Unit,
    onActivate: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emerald = MaterialTheme.colorScheme.tertiary
    val borderColor by animateColorAsState(
        if (isMockActive) emerald.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
        label = "PanelBorder"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 14.dp)
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isMockActive) emerald.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PinDrop,
                        contentDescription = null,
                        tint = if (isMockActive) emerald else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mock Lokasi Diam", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (isMockActive) "Aktif · $modeLabel" else "Ketuk peta atau geser pin · $modeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMockActive) emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalIconButton(onClick = onOpenStealth, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.Shield, contentDescription = "Pengaturan stealth & anti-deteksi")
                }
            }

            // Koordinat titik terpilih
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "LAT / LNG",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "%.6f, %.6f".format(lat, lng),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Akurasi ±%.1f m".format(accuracy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            AnimatedVisibility(
                visible = routeRunning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    onClick = onOpenRoutes,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Route, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Simulasi rute sedang berjalan. Kelola di menu Rute.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            MockToggleButton(isMockActive = isMockActive, onActivate = onActivate, onStop = onStop)
        }
    }
}

@Composable
private fun MockToggleButton(
    isMockActive: Boolean,
    onActivate: () -> Unit,
    onStop: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val emerald = MaterialTheme.colorScheme.tertiary
    val container by animateColorAsState(
        if (isMockActive) emerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary,
        label = "MockButtonColor"
    )
    val content = if (isMockActive) emerald else MaterialTheme.colorScheme.onPrimary
    Button(
        onClick = if (isMockActive) onStop else onActivate,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressScale(pressed),
        shape = CircleShape,
        border = if (isMockActive) BorderStroke(1.dp, emerald.copy(alpha = 0.6f)) else null,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        AnimatedContent(
            targetState = isMockActive,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
            label = "MockButtonContent"
        ) { active ->
            val (label, icon) = if (active) "Hentikan Mock" to Icons.Filled.Stop else "Aktifkan Mock Lokasi" to Icons.Outlined.PinDrop
            ButtonContent(label, icon)
        }
    }
}

@Composable
private fun ButtonContent(label: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
