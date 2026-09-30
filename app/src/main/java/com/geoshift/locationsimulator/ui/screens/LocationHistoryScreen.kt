package com.geoshift.locationsimulator.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoshift.locationsimulator.data.model.HistoryItem
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.components.TactileHeader
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
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocationHistoryScreen(
    viewModel: GeoShiftViewModel,
    onNavigateToTab: (NavigationTab) -> Unit
) {
    val context = LocalContext.current
    val historyItems by viewModel.historyList.collectAsState()
    val simulationState by viewModel.simulationState.collectAsState()
    val simulationMode by viewModel.simulationMode.collectAsState()

    val favoriteCount = remember(historyItems) { historyItems.count { it.isFavorite } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBase)
    ) {
        TactileHeader(
            subtitle = "Riwayat Lokasi & Telemetri",
            simulationState = simulationState,
            simulationMode = simulationMode,
            onDiagnosticClick = { viewModel.openDiagnostic() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Panel Metrik Ringkasan Database
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, OutlineHairline, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DATABASE RIWAYAT LOKASI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tersimpan di SQLite Lokal HP",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Tombol Ekspor GPX
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerHigh)
                                    .clickable {
                                        val file = viewModel.exportHistoryToGpx(context)
                                        if (file != null) {
                                            Toast.makeText(context, "Berhasil ekspor: Download/${file.name}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Tidak ada data riwayat untuk diekspor", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ekspor GPX", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryCyan)
                                }
                            }
                        }

                        // Metrik Kotak Statistik
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Total Entri", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${historyItems.size} Lokasi", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = PrimaryCyan)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Favorit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$favoriteCount Disimpan", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = WarningAmber)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Status DB", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Sinkron", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = TertiaryGreen)
                                }
                            }
                        }
                    }
                }
            }

            // Header Daftar & Tombol Bersihkan Semua
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOG AKTIVITAS MOCK TERBARU",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (historyItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DestructiveRed.copy(alpha = 0.15f))
                                .clickable {
                                    viewModel.clearAllHistory()
                                    Toast.makeText(context, "Seluruh riwayat berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = DestructiveRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bersihkan Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DestructiveRed)
                        }
                    }
                }
            }

            // Empty State
            if (historyItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceContainer)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum Ada Riwayat Lokasi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Riwayat akan otomatis dicatat saat Anda melakukan simulasi atau teleport lokasi.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigateToTab(NavigationTab.HOME) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryCyan,
                                    contentColor = Color(0xFF003543)
                                )
                            ) {
                                Text("Buka Peta Utama", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Daftar Item Riwayat
            items(historyItems, key = { it.id }) { item ->
                HistoryCard(
                    item = item,
                    onTeleport = {
                        viewModel.teleportLocation(item.latitude, item.longitude, item.title, item.subtitle, context)
                        onNavigateToTab(NavigationTab.HOME)
                    },
                    onSelectOnMap = {
                        viewModel.setLocation(item.latitude, item.longitude, item.title, item.subtitle)
                        Toast.makeText(context, "Pin peta dipindahkan ke ${item.title}", Toast.LENGTH_SHORT).show()
                        onNavigateToTab(NavigationTab.HOME)
                    },
                    onToggleFavorite = {
                        viewModel.toggleFavorite(item.id, item.isFavorite)
                    },
                    onDelete = {
                        viewModel.deleteHistory(item.id)
                        Toast.makeText(context, "Item riwayat dihapus", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    onTeleport: () -> Unit,
    onSelectOnMap: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("id-ID")) }
    val formattedTime = remember(item.timestamp) { timeFormat.format(Date(item.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineHairline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Baris Atas: Icon Mode, Nama Tempat, dan Tombol Star/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val isRoute = item.mode.contains("Route", ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isRoute) TertiaryGreen.copy(alpha = 0.2f) else PrimaryCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRoute) Icons.Default.Route else Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = if (isRoute) TertiaryGreen else PrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = item.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }

                // Tombol Aksi Kanan (Bintang Favorit & Hapus)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorit",
                            tint = if (item.isFavorite) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus",
                            tint = DestructiveRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Koordinat & Waktu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${"%.4f".format(item.latitude)}, ${"%.4f".format(item.longitude)} · ${item.mode}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SecondaryBlue
                )
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tombol Aksi Eksekusi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelectOnMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pilih di Peta", fontSize = 11.sp)
                }

                Button(
                    onClick = onTeleport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TertiaryGreen,
                        contentColor = Color(0xFF003314)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Replay / Teleport", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
