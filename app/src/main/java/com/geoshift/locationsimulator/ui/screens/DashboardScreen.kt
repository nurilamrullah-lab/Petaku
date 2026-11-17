package com.geoshift.locationsimulator.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.components.TacticalMapCanvas
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.OutlineHairline
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SecondaryBlue
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.ui.theme.TertiaryGreen
import com.geoshift.locationsimulator.ui.theme.WarningAmber
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import kotlin.math.abs

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

    val isFavorite = historyList.any {
        it.isFavorite &&
            abs(it.latitude - currentLocation.latitude) < 0.0001 &&
            abs(it.longitude - currentLocation.longitude) < 0.0001
    }
    var isExpandedSheet by rememberSaveable { mutableStateOf(true) }

    val isRunning = simulationState == SimulationState.RUNNING
    val isPaused = simulationState == SimulationState.PAUSED
    val statusColor = when {
        isRunning -> TertiaryGreen
        isPaused -> WarningAmber
        else -> PrimaryCyan
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ==========================================
        // 1. PETA PENUH LAYAR (FULL SCREEN MAP CANVAS)
        // ==========================================
        TacticalMapCanvas(
            modifier = Modifier.fillMaxSize(),
            location = currentLocation,
            mode = simulationMode,
            realLocation = realDeviceLocation,
            satellitesInUse = fixInfo.satellitesInUse,
            satellitesInView = fixInfo.satellitesInView,
            // Info satelit & akurasi sudah tampil di kartu atas/bawah; badge peta akan tertutup kartu
            showStatusOverlays = false,
            onLocationSelected = { lat, lng, name, address ->
                viewModel.setLocation(lat, lng, name, address)
            }
        )

        // ==========================================
        // 2. FLOATING TOP CARD (INDRIVE STYLE HEADER)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainer.copy(alpha = 0.94f))
                .border(1.dp, OutlineHairline, RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolom Info Lokasi
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClickLabel = "Cari lokasi") { onNavigateToTab(NavigationTab.SEARCH) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isRunning -> "MOCK AKTIF"
                                isPaused -> "MOCK DIJEDA"
                                else -> "SIAP SIMULASI"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    }

                    Text(
                        text = currentLocation.locationName.ifEmpty { "Posisi Terpilih" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Text(
                        text = "${"%.4f".format(currentLocation.latitude)}, ${"%.4f".format(currentLocation.longitude)} · ${fixInfo.satellitesInUse}/${fixInfo.satellitesInView} SATS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SecondaryBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tombol Aksi Kanan (Favorit & Cari)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val saved = viewModel.toggleCurrentLocationFavorite()
                            Toast.makeText(
                                context,
                                if (saved) "Disimpan ke Favorit (lihat tab Riwayat)" else "Dihapus dari Favorit",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isFavorite) "Hapus dari favorit" else "Simpan ke favorit",
                            tint = if (isFavorite) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { onNavigateToTab(NavigationTab.SEARCH) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari lokasi",
                            tint = PrimaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. FLOATING BOTTOM SHEET (INDRIVE STYLE TELEMETRY & CONTROLS)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(SurfaceContainer.copy(alpha = 0.96f))
                .border(1.dp, OutlineHairline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Handle & toggle collapse (seluruh baris dapat diketuk)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            onClickLabel = if (isExpandedSheet) "Ciutkan detail" else "Tampilkan detail"
                        ) { isExpandedSheet = !isExpandedSheet }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KONTROL SIMULASI LOKASI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryCyan
                        )
                        Icon(
                            imageVector = if (isExpandedSheet) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Metrik Telemetri Cards (Koordinat, Elevasi, Akurasi)
                AnimatedVisibility(visible = isExpandedSheet) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HomeMetricPill(
                                label = "Koordinat",
                                value = "${"%.4f".format(currentLocation.latitude)}, ${"%.4f".format(currentLocation.longitude)}",
                                modifier = Modifier.weight(1.4f)
                            )
                            HomeMetricPill(
                                label = "Elevasi",
                                value = "${"%.1f".format(currentLocation.altitude)} m",
                                modifier = Modifier.weight(0.8f)
                            )
                            HomeMetricPill(
                                label = "Akurasi",
                                value = "±${"%.1f".format(currentLocation.accuracy)} m",
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                        Text(
                            text = "Ketuk atau tahan peta, atau geser pin, untuk memilih titik.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tombol Aksi Utama (Mulai / Jeda / Lanjutkan / Hentikan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        isRunning -> {
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
                            StopMockButton(
                                onClick = { viewModel.stopSimulation(context) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        isPaused -> {
                            // Lanjutkan, bukan mulai ulang: mulai ulang mencatat riwayat ganda
                            Button(
                                onClick = { viewModel.resumeSimulation() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TertiaryGreen,
                                    contentColor = Color(0xFF003314)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lanjutkan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            StopMockButton(
                                onClick = { viewModel.stopSimulation(context) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        else -> {
                            Button(
                                onClick = { viewModel.startSimulation(context) },
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
                                Text("Mulai Mock Lokasi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopMockButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DestructiveRed,
            contentColor = Color.White
        )
    ) {
        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Hentikan Mock", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineHairline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
