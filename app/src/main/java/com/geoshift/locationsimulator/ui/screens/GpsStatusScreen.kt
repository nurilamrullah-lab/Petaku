package com.geoshift.locationsimulator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoshift.locationsimulator.data.model.GnssConstellation
import com.geoshift.locationsimulator.data.model.GnssSatellite
import com.geoshift.locationsimulator.ui.components.TactileHeader
import com.geoshift.locationsimulator.ui.theme.CanvasBase
import com.geoshift.locationsimulator.ui.theme.OutlineHairline
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SecondaryBlue
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerHigh
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.ui.theme.TertiaryGreen
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GpsStatusScreen(
    viewModel: GeoShiftViewModel
) {
    val context = LocalContext.current
    val satellites by viewModel.satellites.collectAsState()
    val fixInfo by viewModel.fixInfo.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val liveLoc by viewModel.liveGnssLocation.collectAsState()
    val simulationState by viewModel.simulationState.collectAsState()
    val simulationMode by viewModel.simulationMode.collectAsState()

    val displayLat = liveLoc?.latitude ?: currentLocation.latitude
    val displayLng = liveLoc?.longitude ?: currentLocation.longitude
    val displayAlt = if (liveLoc != null && liveLoc!!.hasAltitude()) liveLoc!!.altitude else currentLocation.altitude
    val displayAcc = if (liveLoc != null && liveLoc!!.hasAccuracy()) liveLoc!!.accuracy else currentLocation.accuracy

    DisposableEffect(Unit) {
        viewModel.startGnssMonitoring()
        onDispose {
            viewModel.stopGnssMonitoring()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBase)
    ) {
        TactileHeader(
            subtitle = "GNSS Status · Live Satellite Radar",
            simulationState = simulationState,
            simulationMode = simulationMode,
            onDiagnosticClick = { viewModel.openDiagnostic() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer)
                    .border(1.dp, OutlineHairline, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (fixInfo.isFixed) TertiaryGreen else Color(0xFFFFB300))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fixInfo.fixType,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Akurasi: ±${"%.1f".format(displayAcc)}m · Elevasi: ${"%.1f".format(displayAlt)}m · ${"%.4f".format(displayLat)}, ${"%.4f".format(displayLng)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${fixInfo.satellitesInUse}/${fixInfo.satellitesInView}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryCyan
                        )
                        Text(
                            text = "SATS DIGUNAKAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Polar Sky Plot Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineHairline, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                PolarRadarView(
                    satellites = satellites,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Constellation Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConstellationBadge("GPS", 0xFF00D2FF)
                ConstellationBadge("GLONASS", 0xFFFF5252)
                ConstellationBadge("Galileo", 0xFF10B981)
                ConstellationBadge("BeiDou", 0xFFFFB300)
                ConstellationBadge("QZSS", 0xFF7C4DFF)
            }

            // Signal Strength Bars (C/N0 dB-Hz)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer)
                    .border(1.dp, OutlineHairline, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kekuatan Sinyal Satelit (C/N₀ dB-Hz)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "0 - 50 dB",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryCyan
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(satellites) { sat ->
                        SatelliteSignalBar(satellite = sat)
                    }
                }
            }

            // Dilution of Precision (DOP) Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DopMetricCard("HDOP", String.format("%.2f", fixInfo.hdop), "Horizontal", Modifier.weight(1f))
                DopMetricCard("VDOP", String.format("%.2f", fixInfo.vdop), "Vertikal", Modifier.weight(1f))
                DopMetricCard("PDOP", String.format("%.2f", fixInfo.pdop), "Posisi 3D", Modifier.weight(1f))
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = "GNSS Fix: ${fixInfo.fixType}, Sats: ${fixInfo.satellitesInUse}/${fixInfo.satellitesInView}, Coords: ${currentLocation.latitude}, ${currentLocation.longitude}, HDOP: ${fixInfo.hdop}"
                        clipboard.setPrimaryClip(ClipData.newPlainText("GNSS Data", text))
                        Toast.makeText(context, "Data GNSS disalin ke clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Data", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.startGnssMonitoring()
                        Toast.makeText(context, "Sinyal GNSS diperbarui", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        contentColor = Color(0xFF003543)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh Sinyal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PolarRadarView(
    satellites: List<GnssSatellite>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2f) - 24f

        val gridColor = Color(0xFF233452)
        val textColor = android.graphics.Color.parseColor("#859399")

        // Concentric Elevation Rings (0°, 30°, 60°)
        drawCircle(color = gridColor, radius = radius, style = Stroke(width = 1.5f))
        drawCircle(color = gridColor.copy(alpha = 0.7f), radius = radius * 0.66f, style = Stroke(width = 1f))
        drawCircle(color = gridColor.copy(alpha = 0.5f), radius = radius * 0.33f, style = Stroke(width = 1f))
        drawCircle(color = Color(0xFF00D2FF).copy(alpha = 0.3f), radius = 4f)

        // Crosshairs
        drawLine(color = gridColor, start = Offset(center.x, center.y - radius), end = Offset(center.x, center.y + radius), strokeWidth = 1f)
        drawLine(color = gridColor, start = Offset(center.x - radius, center.y), end = Offset(center.x + radius, center.y), strokeWidth = 1f)

        // Cardinal Text
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = textColor
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.MONOSPACE
            }
            drawText("U", center.x, center.y - radius - 8f, paint)
            drawText("S", center.x, center.y + radius + 24f, paint)
            drawText("B", center.x - radius - 16f, center.y + 10f, paint)
            drawText("T", center.x + radius + 16f, center.y + 10f, paint)
        }

        // Plot Satellites
        for (sat in satellites) {
            val elClamped = sat.elevationDegrees.coerceIn(0f, 90f)
            val r = radius * ((90f - elClamped) / 90f)
            val azRad = Math.toRadians((sat.azimuthDegrees - 90.0))
            val x = center.x + (r * cos(azRad)).toFloat()
            val y = center.y + (r * sin(azRad)).toFloat()

            val satColor = Color(sat.constellation.colorHex)

            // Outer ring if used in fix
            if (sat.usedInFix) {
                drawCircle(
                    color = satColor.copy(alpha = 0.35f),
                    radius = 12f,
                    center = Offset(x, y)
                )
            }

            drawCircle(
                color = satColor,
                radius = 6.5f,
                center = Offset(x, y)
            )

            // PRN Text
            drawContext.canvas.nativeCanvas.drawText(
                sat.prnLabel,
                x + 12f,
                y + 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 22f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                }
            )
        }
    }
}

@Composable
private fun SatelliteSignalBar(satellite: GnssSatellite) {
    val barMaxHeight = 80.dp
    val fraction = (satellite.cn0DbHz / 50f).coerceIn(0f, 1f)
    val color = when {
        satellite.cn0DbHz >= 35f -> TertiaryGreen
        satellite.cn0DbHz >= 25f -> Color(0xFFFFB300)
        else -> Color(0xFF859399)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Text(
            text = "${satellite.cn0DbHz.toInt()}",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bar Container
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(barMaxHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceContainerLowest),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barMaxHeight * fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = satellite.prnLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (satellite.usedInFix) Color(satellite.constellation.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConstellationBadge(name: String, colorHex: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineHairline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(colorHex))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DopMetricCard(label: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = PrimaryCyan, modifier = Modifier.padding(vertical = 2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
