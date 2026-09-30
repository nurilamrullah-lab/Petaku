package com.geoshift.locationsimulator.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoshift.locationsimulator.data.model.RouteSimulationTrack
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.ui.components.InDriveRouteMapCanvas
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.theme.CanvasBase
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.OutlineHairline
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SecondaryBlue
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerHigh
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.ui.theme.TertiaryGreen
import com.geoshift.locationsimulator.ui.theme.WarningAmber
import com.geoshift.locationsimulator.util.LocationSearchHelper
import com.geoshift.locationsimulator.util.RouteCalculatorHelper
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import kotlinx.coroutines.launch

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

    // Titik Awal (Jemput)
    var originName by remember { mutableStateOf("Jl. Ikan Paus") }
    var originLat by remember { mutableStateOf(realLocation?.latitude ?: currentLocation.latitude) }
    var originLng by remember { mutableStateOf(realLocation?.longitude ?: currentLocation.longitude) }

    // Titik Tujuan (Antar)
    var destName by remember { mutableStateOf("") }
    var destLat by remember { mutableStateOf<Double?>(null) }
    var destLng by remember { mutableStateOf<Double?>(null) }
    var destSearchQuery by remember { mutableStateOf("") }

    // Moda Transportasi & Kecepatan
    var transportMode by remember { mutableStateOf("Motor") }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var calculatedRoute by remember { mutableStateOf<RouteSimulationTrack?>(null) }

    val quickDestinations = listOf(
        "Kopi Kenangan Gajah Mada" to Pair(-8.1818, 113.6853),
        "Alun-Alun Jember" to Pair(-8.1724, 113.7007),
        "Lippo Plaza Jember" to Pair(-8.1793, 113.6845),
        "Stasiun Jember" to Pair(-8.1643, 113.7032),
        "Universitas Jember" to Pair(-8.1649, 113.7171)
    )

    fun calculateRouteToDestination(dLat: Double, dLng: Double, dName: String) {
        destLat = dLat
        destLng = dLng
        destName = dName
        isCalculatingRoute = true
        coroutineScope.launch {
            val route = RouteCalculatorHelper.calculateRoadRoute(
                originName = originName,
                originLat = originLat,
                originLng = originLng,
                destName = dName,
                destLat = dLat,
                destLng = dLng
            )
            calculatedRoute = route
            viewModel.setRoute(route)
            isCalculatingRoute = false
        }
    }

    fun searchAndSetDestination(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        isCalculatingRoute = true
        coroutineScope.launch {
            val parsedCoord = LocationSearchHelper.parseDirectCoordinates(q)
            if (parsedCoord != null) {
                calculateRouteToDestination(parsedCoord.latitude, parsedCoord.longitude, parsedCoord.title)
            } else {
                val results = LocationSearchHelper.searchLocation(context, q)
                val first = results.firstOrNull()
                if (first != null) {
                    calculateRouteToDestination(first.latitude, first.longitude, first.title)
                } else {
                    Toast.makeText(context, "Tujuan \"$q\" tidak ditemukan", Toast.LENGTH_SHORT).show()
                    isCalculatingRoute = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ==========================================
        // 1. FULL MAP BACKGROUND (PETA PENUH LAYAR)
        // ==========================================
        InDriveRouteMapCanvas(
            originLat = originLat,
            originLng = originLng,
            destLat = destLat,
            destLng = destLng,
            simulatedLat = if (simulationState == SimulationState.RUNNING) currentLocation.latitude else null,
            simulatedLng = if (simulationState == SimulationState.RUNNING) currentLocation.longitude else null,
            realLocation = realLocation,
            routeTrack = calculatedRoute,
            modifier = Modifier.fillMaxSize(),
            onOriginChanged = { newLat, newLng ->
                originLat = newLat
                originLng = newLng
                originName = "Titik Jemput (${"%.4f".format(newLat)}, ${"%.4f".format(newLng)})"
                if (destLat != null && destLng != null) {
                    calculateRouteToDestination(destLat!!, destLng!!, destName)
                }
            },
            onDestChanged = { newLat, newLng ->
                val name = "Tujuan (${"%.4f".format(newLat)}, ${"%.4f".format(newLng)})"
                calculateRouteToDestination(newLat, newLng, name)
            }
        )

        // ==========================================
        // 2. FLOATING TOP BAR: FORM RUTE (INDRIVE STYLE)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainer.copy(alpha = 0.95f))
                .border(1.dp, OutlineHairline, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Header Mini
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MASUKKAN RUTE ANDA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryCyan
                    )

                    if (destLat != null) {
                        IconButton(
                            onClick = {
                                val tLat = originLat
                                val tLng = originLng
                                val tName = originName
                                originLat = destLat!!
                                originLng = destLng!!
                                originName = destName
                                calculateRouteToDestination(tLat, tLng, tName)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Tukar", tint = SecondaryBlue)
                        }
                    }
                }

                // Baris Dari (Jemput)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TertiaryGreen)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Dari",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = originName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (realLocation != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHigh)
                                .clickable {
                                    val loc = realLocation
                                    if (loc != null) {
                                        originLat = loc.latitude
                                        originLng = loc.longitude
                                        originName = "Lokasi GPS HP"
                                        if (destLat != null && destLng != null) {
                                            calculateRouteToDestination(destLat!!, destLng!!, destName)
                                        }
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("GPS HP", fontSize = 9.sp, color = PrimaryCyan)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OutlineHairline.copy(alpha = 0.5f))
                )

                // Baris Ke (Tujuan)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(DestructiveRed)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ke",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    OutlinedTextField(
                        value = destSearchQuery,
                        onValueChange = { destSearchQuery = it },
                        placeholder = { Text("Ketik tujuan atau sentuh peta...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                searchAndSetDestination(destSearchQuery)
                            }
                        ),
                        trailingIcon = {
                            if (isCalculatingRoute) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryCyan)
                            } else if (destSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    keyboardController?.hide()
                                    searchAndSetDestination(destSearchQuery)
                                }) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = "Cari", tint = PrimaryCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = PrimaryCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==========================================
        // 3. TAMPILAN AWAL (SEBELUM ADA TUJUAN): HINT & QUICK CHIPS
        // ==========================================
        if (destLat == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hint Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.90f))
                            .border(1.dp, PrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "👉 Sentuh lokasi mana saja di peta untuk menentukan tujuan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Quick Destination Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickDestinations) { (title, coords) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainer.copy(alpha = 0.92f))
                                    .border(1.dp, OutlineHairline, RoundedCornerShape(10.dp))
                                    .clickable {
                                        destSearchQuery = title
                                        calculateRouteToDestination(coords.first, coords.second, title)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. SETELAH TUJUAN DITENTUKAN: BOTTOM SHEET INFORMATIF (INDRIVE STYLE)
        // ==========================================
        AnimatedVisibility(
            visible = destLat != null && calculatedRoute != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val route = calculatedRoute ?: return@AnimatedVisibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(SurfaceContainer.copy(alpha = 0.96f))
                    .border(1.dp, OutlineHairline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Ringkasan & Tombol Batal/Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = destName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Dari $originName",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        // Tombol Reset / Kembali ke Full Peta
                        IconButton(
                            onClick = {
                                destLat = null
                                destLng = null
                                destName = ""
                                destSearchQuery = ""
                                calculatedRoute = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Baris Kalkulasi Jarak & Estimasi Waktu (inDrive Style)
                    val estimatedMinutes = when (transportMode) {
                        "Jalan" -> (route.totalDistanceKm / 5.0 * 60).toInt().coerceAtLeast(1)
                        "Sepeda" -> (route.totalDistanceKm / 18.0 * 60).toInt().coerceAtLeast(1)
                        "Motor" -> (route.totalDistanceKm / 40.0 * 60).toInt().coerceAtLeast(1)
                        else -> (route.totalDistanceKm / 60.0 * 60).toInt().coerceAtLeast(1)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLowest)
                                .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Column {
                                Text(text = "Total Jarak", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${"%.1f".format(route.totalDistanceKm)} km",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = PrimaryCyan
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLowest)
                                .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Column {
                                Text(text = "Estimasi Waktu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$estimatedMinutes Menit",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TertiaryGreen
                                )
                            }
                        }
                    }

                    // Pilihan Moda Transportasi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InDriveTransportOption(
                            label = "Motor",
                            speed = "40 km/h",
                            icon = Icons.Default.TwoWheeler,
                            isSelected = transportMode == "Motor",
                            onClick = {
                                transportMode = "Motor"
                                viewModel.setSpeed(1.0f)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        InDriveTransportOption(
                            label = "Mobil",
                            speed = "60 km/h",
                            icon = Icons.Default.DirectionsCar,
                            isSelected = transportMode == "Mobil",
                            onClick = {
                                transportMode = "Mobil"
                                viewModel.setSpeed(1.5f)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        InDriveTransportOption(
                            label = "Sepeda",
                            speed = "18 km/h",
                            icon = Icons.Default.DirectionsBike,
                            isSelected = transportMode == "Sepeda",
                            onClick = {
                                transportMode = "Sepeda"
                                viewModel.setSpeed(0.4f)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        InDriveTransportOption(
                            label = "Jalan",
                            speed = "5 km/h",
                            icon = Icons.Default.DirectionsWalk,
                            isSelected = transportMode == "Jalan",
                            onClick = {
                                transportMode = "Jalan"
                                viewModel.setSpeed(0.12f)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Progress Simulasi Perjalanan Jika Aktif
                    if (simulationState == SimulationState.RUNNING || routeProgress > 0f) {
                        val completedPercent = (routeProgress * 100).toInt()
                        val distanceCovered = routeProgress * route.totalDistanceKm
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Simulasi Sedang Berjalan...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$completedPercent% (${"%.1f".format(distanceCovered)} / ${"%.1f".format(route.totalDistanceKm)} km)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan
                                )
                            }
                            LinearProgressIndicator(
                                progress = { routeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PrimaryCyan,
                                trackColor = SurfaceContainerHigh
                            )
                        }
                    }

                    // Tombol Aksi Utama
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (simulationState != SimulationState.RUNNING) {
                            Button(
                                onClick = {
                                    viewModel.startRouteSimulation(route, context)
                                    Toast.makeText(context, "Simulasi Rute Berjalan!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TertiaryGreen,
                                    contentColor = Color(0xFF003314)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mulai Simulasi Rute", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.pauseSimulation() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarningAmber,
                                    contentColor = Color(0xFF332000)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Jeda", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.stopSimulation(context)
                                    Toast.makeText(context, "Simulasi Dihentikan", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DestructiveRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hentikan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InDriveTransportOption(
    label: String,
    speed: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryCyan else SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF003543) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF003543) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = speed,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color(0xFF003543).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
