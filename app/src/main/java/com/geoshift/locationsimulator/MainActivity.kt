package com.geoshift.locationsimulator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.geoshift.locationsimulator.ui.components.BottomNavigationShell
import com.geoshift.locationsimulator.ui.components.DiagnosticDialog
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.screens.DashboardScreen
import com.geoshift.locationsimulator.ui.screens.LocationHistoryScreen
import com.geoshift.locationsimulator.ui.screens.LocationSearchScreen
import com.geoshift.locationsimulator.ui.screens.RouteSimulationScreen
import com.geoshift.locationsimulator.ui.screens.SettingsScreen
import com.geoshift.locationsimulator.ui.theme.CanvasBase
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.GeoShiftTheme
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GeoShiftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bar sistem transparan dengan ikon terang: konten peta tampil penuh di balik status & nav bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val settingsRepo = remember { com.geoshift.locationsimulator.GeoShiftApplication.instance.settingsRepository }
            val uiTheme by settingsRepo.uiTheme.collectAsState()

            GeoShiftTheme(uiTheme = uiTheme) {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: GeoShiftViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.HOME) }
    val showDiagnostic by viewModel.showDiagnosticDialog.collectAsState()
    val diagnostic by viewModel.diagnostic.collectAsState()
    val errorMessage by viewModel.statusErrorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    // Simpan state rememberSaveable tiap tab (input rute, kueri cari, layer peta) saat berpindah tab
    val tabStateHolder = rememberSaveableStateHolder()

    // Runtime Permission Request for Location & Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            viewModel.syncWithRealLocation(context)
        } else {
            coroutineScope.launch {
                val action = snackbarHostState.showSnackbar(
                    message = "Izin lokasi ditolak. Lokasi GPS asli & status satelit tidak tersedia.",
                    actionLabel = "Pengaturan",
                    duration = SnackbarDuration.Long
                )
                if (action == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
        viewModel.syncWithRealLocation(context)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBase),
        bottomBar = {
            BottomNavigationShell(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    action = {
                        val actionLabel = data.visuals.actionLabel
                        if (actionLabel != null) {
                            TextButton(onClick = { data.performAction() }) {
                                Text(actionLabel, color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { data.dismiss() }) {
                                Text("Tutup", color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    // Geser sedikit ke arah tab tujuan + pudar; terasa spasial tanpa berat
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    val enter = fadeIn(tween(220, delayMillis = 60)) +
                        slideInHorizontally(tween(320, easing = EaseOutCubic)) { w -> dir * w / 10 }
                    val exit = fadeOut(tween(120)) +
                        slideOutHorizontally(tween(320, easing = EaseOutCubic)) { w -> -dir * w / 10 }
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "TabTransition"
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab.name) {
                when (tab) {
                    NavigationTab.HOME -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { selectedTab = it }
                    )
                    NavigationTab.SEARCH -> LocationSearchScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { selectedTab = it }
                    )
                    NavigationTab.ROUTES -> RouteSimulationScreen(
                        viewModel = viewModel
                    )
                    NavigationTab.GPS_STATUS -> com.geoshift.locationsimulator.ui.screens.GpsStatusScreen(
                        viewModel = viewModel
                    )
                    NavigationTab.HISTORY -> LocationHistoryScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { selectedTab = it }
                    )
                    NavigationTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
                }
            }

            // Diagnostic Dialog
            if (showDiagnostic) {
                DiagnosticDialog(
                    diagnostic = diagnostic,
                    onDismiss = { viewModel.closeDiagnostic() },
                    onCheckAgain = { viewModel.refreshDiagnostic() }
                )
            }
        }
    }
}
