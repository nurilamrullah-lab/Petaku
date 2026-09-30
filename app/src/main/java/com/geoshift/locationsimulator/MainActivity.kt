package com.geoshift.locationsimulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerHigh
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GeoShiftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }
    val showDiagnostic by viewModel.showDiagnosticDialog.collectAsState()
    val diagnostic by viewModel.diagnostic.collectAsState()
    val errorMessage by viewModel.statusErrorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime Permission Request for Location & Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted or denied handled gracefully without crashing
    }

    val context = androidx.compose.ui.platform.LocalContext.current

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
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = SurfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    action = {
                        TextButton(onClick = { data.dismiss() }) {
                            Text("Dismiss", color = PrimaryCyan)
                        }
                    }
                ) {
                    Text(data.visuals.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "TabTransition"
            ) { tab ->
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
