package com.geoshift.locationsimulator.ui.screens

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoshift.locationsimulator.GeoShiftApplication
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

@Composable
fun SettingsScreen(
    viewModel: GeoShiftViewModel
) {
    val context = LocalContext.current
    val diagnostic by viewModel.diagnostic.collectAsState()
    val simulationState by viewModel.simulationState.collectAsState()
    val simulationMode by viewModel.simulationMode.collectAsState()

    val settingsRepo = remember { GeoShiftApplication.instance.settingsRepository }
    val jitterEnabled by settingsRepo.jitterEnabled.collectAsState()
    val backgroundPersistence by settingsRepo.backgroundPersistence.collectAsState()
    val antiDetection by settingsRepo.antiDetectionHeuristics.collectAsState()
    val updateInterval by settingsRepo.updateIntervalMs.collectAsState()
    val mapStyle by settingsRepo.mapStyle.collectAsState()
    val uiTheme by settingsRepo.uiTheme.collectAsState()
    val defaultAnchorName by settingsRepo.defaultAnchorName.collectAsState()
    val telemetryUnits by settingsRepo.telemetryUnits.collectAsState()
    val autoStartBoot by settingsRepo.autoStartBoot.collectAsState()
    val notificationEnabled by settingsRepo.notificationEnabled.collectAsState()

    // Dialog state controllers
    var showThemeDialog by remember { mutableStateOf(false) }
    var showMapStyleDialog by remember { mutableStateOf(false) }
    var showAnchorDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    var pingLatencyText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBase)
    ) {
        TactileHeader(
            subtitle = "Location Simulator · Developer Settings",
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
            // ==========================================
            // STATUS HARDWARE & PROVIDER TEST PING
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainer)
                    .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (diagnostic.isMockAppConfigured) TertiaryGreen.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (diagnostic.isMockAppConfigured) TertiaryGreen else WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FusedLocation Engine",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (diagnostic.isMockAppConfigured) TertiaryGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (diagnostic.isMockAppConfigured) "Active" else "Warning",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (diagnostic.isMockAppConfigured) TertiaryGreen else WarningAmber,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Text(
                                text = pingLatencyText ?: (if (diagnostic.isMockAppConfigured) "Mock provider siap diinject" else "Perlu izin Mock Location di Pengembang"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val startNs = SystemClock.elapsedRealtimeNanos()
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                            val elapsedMs = ((SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000.0).coerceAtLeast(1.2)
                            val latencyFormatted = "%.1f ms".format(elapsedMs)
                            pingLatencyText = "Latensi Provider: $latencyFormatted (GPS aktif: $isGpsEnabled)"
                            viewModel.refreshDiagnostic()
                            Toast.makeText(context, "Provider Responsif! Latensi: $latencyFormatted", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerHigh,
                            contentColor = PrimaryCyan
                        )
                    ) {
                        Text("Test Ping", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // ==========================================
            // GENERAL PREFERENCES (THEME, UNITS, ANCHOR)
            // ==========================================
            SettingsSectionHeader(title = "General Preferences", icon = Icons.Default.Palette)
            SettingsCard {
                SettingsNavigationItem(
                    icon = Icons.Default.DarkMode,
                    title = "Interface Appearance",
                    subtitle = "Tema warna antarmuka aplikasi",
                    trailingText = uiTheme,
                    onClick = { showThemeDialog = true }
                )
                SettingsNavigationItem(
                    icon = Icons.Default.Layers,
                    title = "Map Rendering Theme",
                    subtitle = "Tipe layer peta OpenStreetMap & Google",
                    trailingText = mapStyle,
                    onClick = { showMapStyleDialog = true }
                )
                SettingsNavigationItem(
                    icon = Icons.Default.PinDrop,
                    title = "Default Anchor Location",
                    subtitle = defaultAnchorName,
                    trailingText = "Atur",
                    onClick = { showAnchorDialog = true }
                )
                SettingsNavigationItem(
                    icon = Icons.Default.Straighten,
                    title = "Telemetry Units",
                    subtitle = "Satuan pengukuran jarak, kecepatan & elevasi",
                    trailingText = telemetryUnits,
                    onClick = { showUnitsDialog = true }
                )
            }

            // ==========================================
            // SIMULATION ENGINE (JITTER, INTERVAL)
            // ==========================================
            SettingsSectionHeader(title = "Simulation Engine", icon = Icons.Default.Tune)
            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.Vibration,
                    title = "Location Accuracy Jitter (±1.5m)",
                    subtitle = "Simulasi variasi multipath atmosfer alami agar tidak terdeteksi statis",
                    checked = jitterEnabled,
                    onCheckedChange = {
                        settingsRepo.setJitterEnabled(it)
                        Toast.makeText(context, if (it) "Jitter akurasi diaktifkan" else "Jitter dinonaktifkan", Toast.LENGTH_SHORT).show()
                    }
                )

                // Interval chips
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timelapse, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Tick Interval", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLowest)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(250L to "250ms (Ultra)", 500L to "500ms (Balanced)", 1000L to "1000ms (Eco)").forEach { (interval, label) ->
                            val isSel = updateInterval == interval
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) PrimaryCyan else SurfaceContainerLowest)
                                    .clickable {
                                        settingsRepo.setUpdateInterval(interval)
                                        Toast.makeText(context, "Interval set ke: $label", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color(0xFF003543) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BEHAVIOR & PERSISTENCE
            // ==========================================
            SettingsSectionHeader(title = "Behavior & Persistence", icon = Icons.Default.AllInclusive)
            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "Auto-start on Boot",
                    subtitle = "Jalankan simulasi otomatis saat perangkat Android selesai reboot",
                    checked = autoStartBoot,
                    onCheckedChange = {
                        settingsRepo.setAutoStartBoot(it)
                        Toast.makeText(context, if (it) "Auto-start on boot aktif" else "Auto-start on boot nonaktif", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsToggleItem(
                    icon = Icons.Default.BatterySaver,
                    title = "Background Persistence",
                    subtitle = "Mencegah OS membunuh layanan simulasi dengan WakeLock & Foreground Service",
                    checked = backgroundPersistence,
                    onCheckedChange = {
                        settingsRepo.setBackgroundPersistence(it)
                        Toast.makeText(context, if (it) "Background persistence aktif" else "Persistence nonaktif", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsToggleItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Ongoing Status Notification",
                    subtitle = "Tampilkan notifikasi berjalan dengan tombol kontrol langsung",
                    checked = notificationEnabled,
                    onCheckedChange = {
                        settingsRepo.setNotificationEnabled(it)
                        Toast.makeText(context, if (it) "Notifikasi status aktif" else "Notifikasi status disembunyikan", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsToggleItem(
                    icon = Icons.Default.Security,
                    title = "Anti-Detection Heuristics",
                    subtitle = "Noise konstelasi satelit acak & simulasi bearing dinamis",
                    checked = antiDetection,
                    onCheckedChange = {
                        settingsRepo.setAntiDetectionHeuristics(it)
                        Toast.makeText(context, if (it) "Anti-detection aktif" else "Anti-detection nonaktif", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ==========================================
            // PERMISSIONS & OS SETUP
            // ==========================================
            SettingsSectionHeader(title = "Permissions & OS Setup", icon = Icons.Default.VerifiedUser)
            SettingsCard {
                SettingsActionItem(
                    icon = Icons.Default.DeveloperMode,
                    title = "Mock Location App Assignment",
                    subtitle = if (diagnostic.isMockAppConfigured) "GeoShift terdaftar di Opsi Pengembang" else "Belum dipilih sebagai aplikasi lokasi tiruan",
                    badgeText = if (diagnostic.isMockAppConfigured) "CONFIGURED" else "OPEN SETUP",
                    badgeColor = if (diagnostic.isMockAppConfigured) TertiaryGreen else WarningAmber,
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
                SettingsActionItem(
                    icon = Icons.Default.Build,
                    title = "Battery Optimization Whitelist",
                    subtitle = "Kecualikan GeoShift dari pembatasan daya latar belakang OS",
                    badgeText = "WHITELIST",
                    badgeColor = SecondaryBlue,
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
            }

            // ==========================================
            // SYSTEM & BUILD INFO
            // ==========================================
            SettingsSectionHeader(title = "System & Engine Info", icon = Icons.Default.Info)
            SettingsCard {
                SettingsInfoRow(label = "GeoShift Core Version", value = "v1.4.2-tactical (build 104)")
                SettingsInfoRow(label = "Kernel Mock Architecture", value = "FusedLocationProvider + GNSS Hardware")
                SettingsInfoRow(label = "Target Platform", value = "Android 14/15/16 (API 34/35/36)")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==========================================
    // DIALOG: INTERFACE APPEARANCE (8 PILIHAN TEMA)
    // ==========================================
    if (showThemeDialog) {
        val themes = listOf(
            "1. Modern Material 3" to "Bersih, modern, standar terbaru",
            "2. Dark OLED" to "Gelap pekat hemat baterai",
            "3. Glassmorphism 2.0" to "Transparan, blur premium",
            "4. Aurora Gradient" to "Gradient lembut, modern",
            "5. Neo-Minimal" to "Minimal elegan",
            "6. Soft Neumorphism" to "Lembut, modern, ramah",
            "7. CyberTech" to "Teknologi futuristik",
            "8. Dynamic Color" to "Warna mengikuti sistem (Monet)"
        )

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Pilih Tema Antarmuka") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    themes.forEach { (themeTitle, themeDesc) ->
                        val isSelected = uiTheme.contains(themeTitle.substringAfter(". ").trim(), ignoreCase = true) ||
                                (uiTheme.startsWith(themeTitle.take(1)))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryCyan.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    settingsRepo.setUiTheme(themeTitle)
                                    showThemeDialog = false
                                    Toast.makeText(context, "Tema: $themeTitle diterapkan", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    settingsRepo.setUiTheme(themeTitle)
                                    showThemeDialog = false
                                    Toast.makeText(context, "Tema: $themeTitle diterapkan", Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = themeTitle,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = themeDesc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup", color = PrimaryCyan)
                }
            },
            containerColor = SurfaceContainer
        )
    }

    // ==========================================
    // DIALOG: MAP RENDERING THEME
    // ==========================================
    if (showMapStyleDialog) {
        val styles = listOf("Google Roadmap", "Google Satelit", "Google Hybrid", "OpenStreetMap Standard")
        AlertDialog(
            onDismissRequest = { showMapStyleDialog = false },
            title = { Text("Pilih Tipe Tampilan Peta") },
            text = {
                Column {
                    styles.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepo.setMapStyle(style)
                                    showMapStyleDialog = false
                                    Toast.makeText(context, "Layer peta: $style diterapkan", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mapStyle == style,
                                onClick = {
                                    settingsRepo.setMapStyle(style)
                                    showMapStyleDialog = false
                                    Toast.makeText(context, "Layer peta: $style diterapkan", Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapStyleDialog = false }) {
                    Text("Tutup", color = PrimaryCyan)
                }
            },
            containerColor = SurfaceContainer
        )
    }

    // ==========================================
    // DIALOG: DEFAULT ANCHOR LOCATION
    // ==========================================
    if (showAnchorDialog) {
        var anchorNameInput by remember { mutableStateOf(defaultAnchorName) }
        var anchorLatInput by remember { mutableStateOf("-8.1724") }
        var anchorLngInput by remember { mutableStateOf("113.7007") }

        AlertDialog(
            onDismissRequest = { showAnchorDialog = false },
            title = { Text("Atur Lokasi Awal Bawaan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih lokasi default saat aplikasi pertama dibuka:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = anchorNameInput,
                        onValueChange = { anchorNameInput = it },
                        label = { Text("Nama Lokasi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = anchorLatInput,
                        onValueChange = { anchorLatInput = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = anchorLngInput,
                        onValueChange = { anchorLngInput = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                anchorNameInput = "Kopi Kenangan Ruko Gajah Mada"
                                anchorLatInput = "-8.1840"
                                anchorLngInput = "113.6633"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh)
                        ) {
                            Text("Jember", fontSize = 10.sp, color = PrimaryCyan)
                        }
                        Button(
                            onClick = {
                                anchorNameInput = "Monas Jakarta"
                                anchorLatInput = "-6.1754"
                                anchorLngInput = "106.8272"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh)
                        ) {
                            Text("Jakarta", fontSize = 10.sp, color = PrimaryCyan)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = anchorLatInput.toDoubleOrNull() ?: -8.1724
                        val lng = anchorLngInput.toDoubleOrNull() ?: 113.7007
                        settingsRepo.setDefaultAnchor(anchorNameInput, lat, lng)
                        viewModel.setLocation(lat, lng, anchorNameInput, "Default Anchor")
                        showAnchorDialog = false
                        Toast.makeText(context, "Lokasi bawaan disimpan: $anchorNameInput", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color(0xFF003543))
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnchorDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = SurfaceContainer
        )
    }

    // ==========================================
    // DIALOG: TELEMETRY UNITS
    // ==========================================
    if (showUnitsDialog) {
        val units = listOf("Metric (km/jam, m)", "Imperial (mph, ft)")
        AlertDialog(
            onDismissRequest = { showUnitsDialog = false },
            title = { Text("Pilih Satuan Telemetri") },
            text = {
                Column {
                    units.forEach { unit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepo.setTelemetryUnits(unit)
                                    showUnitsDialog = false
                                    Toast.makeText(context, "Satuan: $unit dipilih", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = telemetryUnits == unit,
                                onClick = {
                                    settingsRepo.setTelemetryUnits(unit)
                                    showUnitsDialog = false
                                    Toast.makeText(context, "Satuan: $unit dipilih", Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(unit, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUnitsDialog = false }) {
                    Text("Tutup", color = PrimaryCyan)
                }
            },
            containerColor = SurfaceContainer
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SecondaryBlue,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineHairline, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = trailingText, style = MaterialTheme.typography.labelSmall, color = SecondaryBlue, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF003543),
                checkedTrackColor = TertiaryGreen,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = SurfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}
