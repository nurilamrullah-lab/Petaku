package com.geoshift.locationsimulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.ui.theme.DestructiveRed
import com.geoshift.locationsimulator.ui.theme.OnPrimary
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SecondaryBlue
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerHigh
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.ui.theme.TertiaryGreen

@Composable
fun SimulationControlCard(
    simulationState: SimulationState,
    currentSpeedMultiplier: Float,
    onSpeedSelected: (Float) -> Unit,
    onSetLocationClick: () -> Unit,
    onStartSimulation: () -> Unit,
    onPauseSimulation: () -> Unit,
    onResumeSimulation: () -> Unit,
    onStopSimulation: () -> Unit
) {
    var isJoystickActive by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(14.dp)
    ) {
        Column {
            // Speed header and joystick toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = SecondaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Transit Speed Multiplier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Joystick preview pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isJoystickActive) PrimaryCyan else SurfaceContainerHigh)
                        .clickable { isJoystickActive = !isJoystickActive }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Joystick",
                        tint = if (isJoystickActive) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isJoystickActive) "Joystick: On" else "Joystick: Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isJoystickActive) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speed pills grid (1x, 2x, 5x, 10x)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLowest)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SpeedMultiplierButton(
                    modifier = Modifier.weight(1f),
                    label = "1x Walk",
                    multiplier = 1.0f,
                    isSelected = currentSpeedMultiplier == 1.0f,
                    onClick = { onSpeedSelected(1.0f) }
                )
                SpeedMultiplierButton(
                    modifier = Modifier.weight(1f),
                    label = "2x Bike",
                    multiplier = 2.0f,
                    isSelected = currentSpeedMultiplier == 2.0f,
                    onClick = { onSpeedSelected(2.0f) }
                )
                SpeedMultiplierButton(
                    modifier = Modifier.weight(1f),
                    label = "5x Auto",
                    multiplier = 5.0f,
                    isSelected = currentSpeedMultiplier == 5.0f,
                    onClick = { onSpeedSelected(5.0f) }
                )
                SpeedMultiplierButton(
                    modifier = Modifier.weight(1f),
                    label = "10x Jet",
                    multiplier = 10.0f,
                    isSelected = currentSpeedMultiplier == 10.0f,
                    onClick = { onSpeedSelected(10.0f) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Teleport / Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Set Location / Instant Teleport
                Button(
                    onClick = onSetLocationClick,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        contentColor = OnPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Set Location",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Set Location",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.sp
                    )
                }

                // Play / Pause Simulation
                if (simulationState == SimulationState.RUNNING) {
                    Button(
                        onClick = onPauseSimulation,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerHigh,
                            contentColor = SecondaryBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pause",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp
                        )
                    }
                } else if (simulationState == SimulationState.PAUSED) {
                    Button(
                        onClick = onResumeSimulation,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TertiaryGreen,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resume",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onStartSimulation,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TertiaryGreen,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Start Sim",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp
                        )
                    }
                }

                // Stop Simulation button (always accessible)
                Button(
                    onClick = onStopSimulation,
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = DestructiveRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = DestructiveRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedMultiplierButton(
    modifier: Modifier = Modifier,
    label: String,
    multiplier: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrimaryCyan else SurfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
