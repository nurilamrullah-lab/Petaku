package com.geoshift.locationsimulator.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geoshift.locationsimulator.data.model.RoutePoint
import com.geoshift.locationsimulator.data.model.RouteSimulationTrack
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.ui.components.ENGINE_BASE_SPEED_KMH
import com.geoshift.locationsimulator.ui.components.InDriveRouteMapCanvas
import com.geoshift.locationsimulator.ui.components.JoystickPad
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.components.SpeedControl
import com.geoshift.locationsimulator.ui.components.pressScale
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.WarningAmber
import com.geoshift.locationsimulator.util.LocationSearchHelper
import com.geoshift.locationsimulator.util.RouteCalculatorHelper
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class MoveMode(val label: String) { ROUTE("Rute"), JOYSTICK("Joystick") }

private const val JOYSTICK_TICK_MS = 100L

private val stopsSaver = listSaver<List<RoutePoint>, Any>(
    save = { list -> list.flatMap { listOf(it.name, it.latitude, it.longitude) } },
    restore = { flat -> flat.chunked(3).map { (n, la, ln) -> RoutePoint(n as String, la as Double, ln as Double) } }
)

/** Gabungkan segmen asal → singgah… → tujuan menjadi satu track. */
private suspend fun buildMultiPointRoute(points: List<RoutePoint>): RouteSimulationTrack {
    val segments = points.zipWithNext { a, b ->
        RouteCalculatorHelper.calculateRoadRoute(a.name, a.latitude, a.longitude, b.name, b.latitude, b.longitude)
    }
    if (segments.size == 1) return segments.first()
    val waypoints = segments.flatMapIndexed { i, seg -> if (i == 0) seg.waypoints else seg.waypoints.drop(1) }
    return RouteSimulationTrack(
        id = UUID.randomUUID().toString(),
        name = points.joinToString(" -> ") { it.name },
        originName = points.first().name,
        destinationName = points.last().name,
        totalDistanceKm = segments.sumOf { it.totalDistanceKm.toDouble() }.toFloat(),
        estimatedDurationMinutes = segments.sumOf { it.estimatedDurationMinutes },
        waypoints = waypoints
    )
}

/**
 * Menu Rute: semua simulasi BERGERAK — rute A→B dengan titik singgah, kecepatan,
 * kontrol perjalanan, dan joystick virtual.
 */
@Composable
fun RouteSimulationScreen(
    viewModel: GeoShiftViewModel,
    onNavigateToTab: (NavigationTab) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val simulationState by viewModel.simulationState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val realLocation by viewModel.realDeviceLocation.collectAsState()
    val routeProgress by viewModel.routeProgress.collectAsState()
    val routeActive by viewModel.routeActive.collectAsState()
    val speedMultiplier by viewModel.speedMultiplier.collectAsState()

    var moveModeName by rememberSaveable { mutableStateOf(MoveMode.ROUTE.name) }
    val moveMode = MoveMode.valueOf(moveModeName)

    // rememberSaveable agar rute tidak hilang saat pindah tab
    var originName by rememberSaveable { mutableStateOf("Lokasi Saat Ini") }
    var originLat by rememberSaveable { mutableStateOf(realLocation?.latitude ?: currentLocation.latitude) }
    var originLng by rememberSaveable { mutableStateOf(realLocation?.longitude ?: currentLocation.longitude) }
    var stops by rememberSaveable(stateSaver = stopsSaver) { mutableStateOf(emptyList<RoutePoint>()) }
    var destName by rememberSaveable { mutableStateOf("") }
    var destLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var destLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var destSearchQuery by rememberSaveable { mutableStateOf("") }

    var isCalculatingRoute by remember { mutableStateOf(false) }
    var calculatedRoute by remember { mutableStateOf<RouteSimulationTrack?>(null) }

    val isSimulationActive = simulationState == SimulationState.RUNNING || simulationState == SimulationState.PAUSED
    val isPaused = simulationState == SimulationState.PAUSED
    // Simulasi yang sedang aktif milik mode lain (mis. mock diam dari menu Peta)
    val foreignSimulation = isSimulationActive && when (moveMode) {
        MoveMode.ROUTE -> !routeActive
        MoveMode.JOYSTICK -> routeActive
    }

    fun isRouteLocked(): Boolean {
        if (isSimulationActive) {
            Toast.makeText(context, "Hentikan simulasi dulu untuk mengubah rute", Toast.LENGTH_SHORT).show()
        }
        return isSimulationActive
    }

    val quickDestinations = listOf(
        "Kopi Kenangan Gajah Mada" to Pair(-8.1818, 113.6853),
        "Alun-Alun Jember" to Pair(-8.1724, 113.7007),
        "Lippo Plaza Jember" to Pair(-8.1793, 113.6845),
        "Stasiun Jember" to Pair(-8.1643, 113.7032),
        "Universitas Jember" to Pair(-8.1649, 113.7171)
    )

    fun recalculate(updateEngine: Boolean = true) {
        val dLat = destLat ?: return
        val dLng = destLng ?: return
        isCalculatingRoute = true
        val points = listOf(RoutePoint(originName, originLat, originLng)) + stops + RoutePoint(destName, dLat, dLng)
        coroutineScope.launch {
            val route = buildMultiPointRoute(points)
            calculatedRoute = route
            if (updateEngine) viewModel.setRoute(route)
            isCalculatingRoute = false
        }
    }

    fun setDestination(lat: Double, lng: Double, name: String) {
        destLat = lat
        destLng = lng
        destName = name
        recalculate()
    }

    fun searchAndSetDestination(query: String) {
        val q = query.trim()
        if (q.isEmpty() || isRouteLocked()) return
        isCalculatingRoute = true
        coroutineScope.launch {
            val parsed = LocationSearchHelper.parseDirectCoordinates(q)
            if (parsed != null) {
                setDestination(parsed.latitude, parsed.longitude, parsed.title)
            } else {
                val first = LocationSearchHelper.searchLocation(context, q).firstOrNull()
                if (first != null) {
                    setDestination(first.latitude, first.longitude, first.title)
                } else {
                    Toast.makeText(context, "Tujuan \"$q\" tidak ditemukan", Toast.LENGTH_SHORT).show()
                    isCalculatingRoute = false
                }
            }
        }
    }

    // Pulihkan garis rute setelah kembali ke tab ini; jangan reset engine bila simulasi aktif
    LaunchedEffect(Unit) {
        if (destLat != null && calculatedRoute == null) recalculate(updateEngine = !isSimulationActive)
    }

    // Joystick: geser posisi sesuai arah & kecepatan terpilih selama simulasi berjalan
    val joystickVector = remember { floatArrayOf(0f, 0f) }
    val currentSpeed by rememberUpdatedState(speedMultiplier)
    LaunchedEffect(moveMode, simulationState, routeActive) {
        if (moveMode != MoveMode.JOYSTICK || simulationState != SimulationState.RUNNING || routeActive) return@LaunchedEffect
        while (true) {
            delay(JOYSTICK_TICK_MS)
            val (x, y) = joystickVector
            if (hypot(x, y) > 0.08f) {
                val metersPerTick = ENGINE_BASE_SPEED_KMH / 3.6f * currentSpeed * (JOYSTICK_TICK_MS / 1000f)
                viewModel.nudgeLocation((x * metersPerTick).toDouble(), (y * metersPerTick).toDouble())
            }
        }
    }

    val density = LocalDensity.current
    var topCardBottomPx by remember { mutableIntStateOf(0) }
    val topObstruction = with(density) { topCardBottomPx.toDp() }

    val showSimulated = isSimulationActive && !foreignSimulation
    val route = calculatedRoute

    Box(modifier = Modifier.fillMaxSize()) {
        InDriveRouteMapCanvas(
            originLat = originLat,
            originLng = originLng,
            destLat = if (moveMode == MoveMode.ROUTE) destLat else null,
            destLng = if (moveMode == MoveMode.ROUTE) destLng else null,
            stops = if (moveMode == MoveMode.ROUTE) stops else emptyList(),
            simulatedLat = if (showSimulated) currentLocation.latitude else null,
            simulatedLng = if (showSimulated) currentLocation.longitude else null,
            realLocation = realLocation,
            routeTrack = if (moveMode == MoveMode.ROUTE) route else null,
            followSimulated = moveMode == MoveMode.JOYSTICK,
            controlsPadding = PaddingValues(top = topObstruction + 12.dp),
            onDeviceLocation = { viewModel.setRealDeviceLocation(it) },
            modifier = Modifier.fillMaxSize(),
            onOriginChanged = { lat, lng ->
                if (isRouteLocked()) return@InDriveRouteMapCanvas
                originLat = lat
                originLng = lng
                originName = "Titik Asal (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                recalculate()
            },
            onDestChanged = { lat, lng ->
                if (isRouteLocked()) return@InDriveRouteMapCanvas
                if (moveMode == MoveMode.JOYSTICK) {
                    originLat = lat
                    originLng = lng
                    originName = "Titik Mulai (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                } else {
                    setDestination(lat, lng, "Tujuan (${"%.4f".format(lat)}, ${"%.4f".format(lng)})")
                }
            }
        )

        // ===== KARTU ATAS: mode + titik rute =====
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .onGloballyPositioned { topCardBottomPx = it.boundsInParent().bottom.toInt() },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MoveMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = moveMode == mode,
                            onClick = { if (!isRouteLocked()) moveModeName = mode.name },
                            shape = SegmentedButtonDefaults.itemShape(i, MoveMode.entries.size),
                            icon = {
                                Icon(
                                    if (mode == MoveMode.ROUTE) Icons.Outlined.Route else Icons.Outlined.SportsEsports,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        ) { Text(mode.label) }
                    }
                }

                PointRow(
                    dotColor = MaterialTheme.colorScheme.tertiary,
                    dotLabel = "A",
                    label = if (moveMode == MoveMode.ROUTE) "Dari" else "Mulai",
                    value = originName,
                    trailing = {
                        if (realLocation != null) {
                            TextButton(
                                onClick = {
                                    val loc = realLocation ?: return@TextButton
                                    if (isRouteLocked()) return@TextButton
                                    originLat = loc.latitude
                                    originLng = loc.longitude
                                    originName = "Lokasi GPS HP"
                                    recalculate()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) { Text("GPS HP", style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                )

                if (moveMode == MoveMode.ROUTE) {
                    stops.forEachIndexed { i, stop ->
                        PointRow(
                            dotColor = WarningAmber,
                            dotLabel = "${i + 1}",
                            label = "Singgah",
                            value = stop.name,
                            trailing = {
                                IconButton(
                                    onClick = {
                                        if (isRouteLocked()) return@IconButton
                                        stops = stops.filterIndexed { idx, _ -> idx != i }
                                        recalculate()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Hapus titik singgah ${i + 1}", modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PointDot(DestructiveRed, "B")
                        Spacer(Modifier.width(10.dp))
                        TextField(
                            value = destSearchQuery,
                            onValueChange = { destSearchQuery = it },
                            placeholder = { Text(if (destLat != null) destName else "Cari tujuan atau ketuk peta", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                searchAndSetDestination(destSearchQuery)
                            }),
                            trailingIcon = {
                                if (isCalculatingRoute) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (destSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        keyboardController?.hide()
                                        searchAndSetDestination(destSearchQuery)
                                    }) { Icon(Icons.Outlined.Search, contentDescription = "Cari tujuan") }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (destLat != null) {
                            IconButton(
                                onClick = {
                                    if (isRouteLocked()) return@IconButton
                                    val (oLat, oLng, oName) = Triple(originLat, originLng, originName)
                                    originLat = destLat!!
                                    originLng = destLng!!
                                    originName = destName
                                    stops = stops.reversed()
                                    setDestination(oLat, oLng, oName)
                                },
                                modifier = Modifier.size(44.dp)
                            ) { Icon(Icons.Outlined.SwapVert, contentDescription = "Balik arah rute") }
                        }
                    }
                } else {
                    Text(
                        "Ketuk peta untuk memindah titik mulai, lalu geser joystick untuk bergerak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ===== JOYSTICK + PANEL BAWAH =====
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = moveMode == MoveMode.JOYSTICK,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    JoystickPad(onVectorChange = { x, y ->
                        joystickVector[0] = x
                        joystickVector[1] = y
                    })
                    if (simulationState != SimulationState.RUNNING || routeActive) {
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
                        ) {
                            Text(
                                "Tekan Mulai agar gerakan\njoystick terkirim ke GPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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

                    if (moveMode == MoveMode.ROUTE) {
                        if (route == null || destLat == null) {
                            Text(
                                "Ketuk peta atau pilih tujuan cepat",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCalculatingRoute) LinearProgressIndicator(Modifier.fillMaxWidth())
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quickDestinations) { (title, coords) ->
                                    Surface(
                                        onClick = {
                                            if (isRouteLocked()) return@Surface
                                            destSearchQuery = ""
                                            setDestination(coords.first, coords.second, title)
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            RouteSummary(
                                route = route,
                                destName = destName,
                                stopCount = stops.size,
                                speedKmh = speedMultiplier * ENGINE_BASE_SPEED_KMH,
                                progress = if (routeActive) routeProgress else 0f,
                                simulationState = if (foreignSimulation) SimulationState.STOPPED else simulationState,
                                onAddStop = {
                                    if (isRouteLocked()) return@RouteSummary
                                    stops = stops + RoutePoint(destName, destLat!!, destLng!!)
                                    destLat = null
                                    destLng = null
                                    destName = ""
                                    calculatedRoute = null
                                    Toast.makeText(context, "Titik singgah ditambahkan. Pilih tujuan berikutnya.", Toast.LENGTH_SHORT).show()
                                },
                                onClear = {
                                    if (isRouteLocked()) return@RouteSummary
                                    destLat = null
                                    destLng = null
                                    destName = ""
                                    destSearchQuery = ""
                                    stops = emptyList()
                                    calculatedRoute = null
                                }
                            )
                            SpeedControl(speedMultiplier, onSpeedChange = { viewModel.setSpeedMultiplier(it) })
                        }
                    } else {
                        SpeedControl(speedMultiplier, onSpeedChange = { viewModel.setSpeedMultiplier(it) })
                    }

                    AnimatedVisibility(
                        visible = foreignSimulation,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Info, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (routeActive) "Simulasi rute sedang berjalan. Hentikan dulu untuk memakai joystick."
                                    else "Mock lokasi diam sedang aktif. Hentikan dulu untuk memulai perjalanan.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    TripActions(
                        isActive = isSimulationActive,
                        isPaused = isPaused,
                        canPause = !foreignSimulation,
                        startLabel = if (moveMode == MoveMode.ROUTE) "Mulai Perjalanan" else "Mulai Joystick",
                        startEnabled = moveMode == MoveMode.JOYSTICK || (route != null && destLat != null && !isCalculatingRoute),
                        onStart = {
                            if (moveMode == MoveMode.ROUTE) {
                                val r = calculatedRoute ?: return@TripActions
                                viewModel.startRouteSimulation(r, context)
                            } else {
                                viewModel.startJoystickMock(originLat, originLng, originName, context)
                            }
                        },
                        onPause = { viewModel.pauseSimulation() },
                        onResume = { viewModel.resumeSimulation() },
                        onStop = { viewModel.stopSimulation(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PointDot(color: Color, label: String) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun PointRow(
    dotColor: Color,
    dotLabel: String,
    label: String,
    value: String,
    trailing: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        PointDot(dotColor, dotLabel)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

@Composable
private fun RouteSummary(
    route: RouteSimulationTrack,
    destName: String,
    stopCount: Int,
    speedKmh: Float,
    progress: Float,
    simulationState: SimulationState,
    onAddStop: () -> Unit,
    onClear: () -> Unit
) {
    val etaMinutes = (route.totalDistanceKm / speedKmh.coerceAtLeast(1f) * 60f).roundToInt().coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(destName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (stopCount > 0) "Via $stopCount titik singgah" else "Rute langsung",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onAddStop, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Icon(Icons.Outlined.AddLocationAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Singgah")
            }
            IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Hapus rute")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Jarak", "%.1f km".format(route.totalDistanceKm), Modifier.weight(1f))
            MetricTile("Estimasi", if (etaMinutes >= 60) "${etaMinutes / 60} j ${etaMinutes % 60} m" else "$etaMinutes mnt", Modifier.weight(1f))
            MetricTile("Progres", "${(progress * 100).roundToInt()}%", Modifier.weight(1f))
        }
        if (simulationState == SimulationState.RUNNING || simulationState == SimulationState.PAUSED) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (simulationState == SimulationState.PAUSED) WarningAmber else MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
    }
}

@Composable
private fun TripActions(
    isActive: Boolean,
    isPaused: Boolean,
    canPause: Boolean,
    startLabel: String,
    startEnabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val (label, icon, colors) = when {
            !isActive -> Triple(startLabel, Icons.Filled.PlayArrow, ButtonDefaults.buttonColors())
            isPaused -> Triple(
                "Lanjutkan", Icons.Filled.PlayArrow,
                ButtonDefaults.buttonColors(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            )
            else -> Triple("Jeda", Icons.Filled.Pause, ButtonDefaults.buttonColors(WarningAmber, Color(0xFF332000)))
        }
        if (!isActive || canPause) {
            Button(
                onClick = { if (!isActive) onStart() else if (isPaused) onResume() else onPause() },
                enabled = isActive || startEnabled,
                interactionSource = interaction,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .pressScale(pressed),
                shape = CircleShape,
                colors = colors
            ) {
                AnimatedContent(
                    targetState = label to icon,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
                    label = "TripActionContent"
                ) { (l, i) -> ActionContent(l, i) }
            }
        }
        AnimatedVisibility(
            visible = isActive,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = if (canPause) Modifier else Modifier.weight(1f)
        ) {
            if (canPause) {
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = DestructiveRed, contentColor = Color.White)
                ) { Icon(Icons.Filled.Stop, contentDescription = "Hentikan") }
            } else {
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(DestructiveRed, Color.White)
                ) { ActionContent("Hentikan", Icons.Filled.Stop) }
            }
        }
    }
}

@Composable
private fun ActionContent(label: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
