package com.geoshift.locationsimulator.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.geoshift.locationsimulator.data.model.DeviceDiagnostic
import com.geoshift.locationsimulator.data.model.SimulationMode
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.OnPrimary
import com.geoshift.locationsimulator.ui.theme.OutlineHairline
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SecondaryBlue
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerHigh
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.ui.theme.TertiaryGreen
import com.geoshift.locationsimulator.ui.theme.WarningAmber

@Composable
fun DiagnosticDialog(
    diagnostic: DeviceDiagnostic,
    onDismiss: () -> Unit,
    onCheckAgain: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Device & Environment",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Compatibility Diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Diagnostic metrics table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagnosticRow("Device", diagnostic.deviceModel, PrimaryCyan)
                    DiagnosticRow("Android OS", "${diagnostic.androidVersion} (SDK ${diagnostic.sdkInt})", MaterialTheme.colorScheme.onSurface)
                    DiagnosticRow("Architecture", diagnostic.architecture, SecondaryBlue)
                    DiagnosticRow(
                        "Root Status",
                        if (diagnostic.isRooted) "YES (Detected)" else "NO",
                        if (diagnostic.isRooted) TertiaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DiagnosticRow(
                        "Mock Location",
                        if (diagnostic.isMockAppConfigured) "READY" else "NOT CONFIGURED",
                        if (diagnostic.isMockAppConfigured) TertiaryGreen else WarningAmber
                    )
                    DiagnosticRow(
                        "Framework / Patch",
                        if (diagnostic.isSmaliPatcherDetected) "YES (Smali Patcher)" else "NOT DETECTED",
                        if (diagnostic.isSmaliPatcherDetected) TertiaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DiagnosticRow(
                        "Active Mode",
                        diagnostic.activeMode.displayName,
                        Color(diagnostic.activeMode.badgeColor)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Explanatory card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerHigh)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (diagnostic.isMockAppConfigured || diagnostic.isSmaliPatcherDetected) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (diagnostic.isMockAppConfigured || diagnostic.isSmaliPatcherDetected) TertiaryGreen else WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = diagnostic.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!diagnostic.isMockAppConfigured && !diagnostic.isRooted) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryCyan,
                                contentColor = OnPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.DeveloperMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Developer Options", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = onCheckAgain,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Again", style = MaterialTheme.typography.labelMedium, color = PrimaryCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor,
            fontSize = 11.sp
        )
    }
}
