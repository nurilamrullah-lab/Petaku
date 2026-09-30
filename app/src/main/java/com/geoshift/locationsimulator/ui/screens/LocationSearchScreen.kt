package com.geoshift.locationsimulator.ui.screens

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.geoshift.locationsimulator.ui.components.BottomNavigationShell
import com.geoshift.locationsimulator.ui.components.NavigationTab
import com.geoshift.locationsimulator.ui.components.TactileHeader
import com.geoshift.locationsimulator.ui.theme.CanvasBase
import com.geoshift.locationsimulator.ui.theme.OutlineHairline
import com.geoshift.locationsimulator.ui.theme.PrimaryCyan
import com.geoshift.locationsimulator.ui.theme.SecondaryBlue
import com.geoshift.locationsimulator.ui.theme.SurfaceContainer
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerHigh
import com.geoshift.locationsimulator.ui.theme.SurfaceContainerLowest
import com.geoshift.locationsimulator.ui.theme.TertiaryGreen
import com.geoshift.locationsimulator.util.LocationSearchHelper
import com.geoshift.locationsimulator.util.SearchResultItem
import com.geoshift.locationsimulator.viewmodel.GeoShiftViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PresetSearchLocation(
    val city: String,
    val place: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val icon: ImageVector
)

@Composable
fun LocationSearchScreen(
    viewModel: GeoShiftViewModel,
    onNavigateToTab: (NavigationTab) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val simulationState by viewModel.simulationState.collectAsState()
    val simulationMode by viewModel.simulationMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var isSearching by remember { mutableStateOf(false) }
    var onlineResults by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val presetLocations = remember {
        listOf(
            PresetSearchLocation("Jember, Jawa Timur", "Alun-Alun Jember", "Pusat Kota", -8.1724, 113.7007, Icons.Default.Park),
            PresetSearchLocation("Jakarta, DKI Jakarta", "Monas • Monumen Nasional", "Landmark", -6.1754, 106.8272, Icons.Default.LocationOn),
            PresetSearchLocation("Surabaya, Jawa Timur", "Tunjungan Plaza", "Mall", -7.2625, 112.7383, Icons.Default.Storefront),
            PresetSearchLocation("Bandung, Jawa Barat", "Gedung Sate", "Pemerintahan", -6.9025, 107.6186, Icons.Default.AccountBalance),
            PresetSearchLocation("Yogyakarta, DIY", "Jalan Malioboro", "Budaya", -7.7928, 110.3658, Icons.Default.LocationOn),
            PresetSearchLocation("Bali, Indonesia", "Pantai Kuta • Badung", "Wisata", -8.7185, 115.1686, Icons.Default.Explore),
            PresetSearchLocation("Semarang, Jawa Tengah", "Simpang Lima", "Pusat Kota", -6.9904, 110.4229, Icons.Default.Park),
            PresetSearchLocation("Medan, Sumatera Utara", "Istana Maimun", "Budaya", 3.5753, 98.6837, Icons.Default.AccountBalance)
        )
    }

    fun triggerSearch(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            isSearching = false
            onlineResults = emptyList()
            return
        }

        // Cek langsung koordinat
        val direct = LocationSearchHelper.parseDirectCoordinates(q)
        if (direct != null) {
            onlineResults = listOf(direct)
            isSearching = false
            return
        }

        isSearching = true
        searchJob = coroutineScope.launch {
            delay(400L) // debounce
            val res = LocationSearchHelper.searchLocation(context, q)
            onlineResults = res
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBase)
    ) {
        TactileHeader(
            subtitle = "Pencarian Lokasi & Koordinat",
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
            // Search Input Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, OutlineHairline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryCyan,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(22.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                triggerSearch(it)
                            },
                            placeholder = {
                                Text(
                                    "Cari kota, jalan, tempat, atau lat,lng...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = PrimaryCyan
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    triggerSearch(searchQuery)
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryCyan
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                onlineResults = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrEmpty()) {
                                    searchQuery = clip
                                    triggerSearch(clip)
                                    Toast.makeText(context, "Koordinat/teks ditempel", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = SecondaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Category Chips
            item {
                val categories = listOf("Semua", "Pusat Kota", "Mall", "Wisata", "Budaya")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryCyan else SurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryCyan else OutlineHairline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF003543) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Hasil Pencarian Online / Koordinat
            if (searchQuery.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HASIL PENCARIAN (${onlineResults.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryCyan
                        )
                    }
                }

                if (onlineResults.isEmpty() && !isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainer)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada lokasi ditemukan untuk \"$searchQuery\".\nCoba ketik nama jalan, kota, atau format koordinat lat,lng.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(onlineResults) { item ->
                    SearchResultCard(
                        title = item.title,
                        subtitle = item.address,
                        category = item.category,
                        latitude = item.latitude,
                        longitude = item.longitude,
                        onSelectOnMap = {
                            viewModel.setLocation(item.latitude, item.longitude, item.title, item.address)
                            onNavigateToTab(NavigationTab.HOME)
                        },
                        onTeleport = {
                            viewModel.teleportLocation(item.latitude, item.longitude, item.title, item.address, context)
                            onNavigateToTab(NavigationTab.HOME)
                        }
                    )
                }
            } else {
                // Preset Lokasi Populer Indonesia
                item {
                    Text(
                        text = "LOKASI POPULER INDONESIA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val filteredPresets = if (selectedCategory == "Semua") presetLocations
                else presetLocations.filter { it.category == selectedCategory }

                items(filteredPresets) { item ->
                    SearchResultCard(
                        title = item.place,
                        subtitle = item.city,
                        category = item.category,
                        latitude = item.latitude,
                        longitude = item.longitude,
                        onSelectOnMap = {
                            viewModel.setLocation(item.latitude, item.longitude, item.place, item.city)
                            onNavigateToTab(NavigationTab.HOME)
                        },
                        onTeleport = {
                            viewModel.teleportLocation(item.latitude, item.longitude, item.place, item.city, context)
                            onNavigateToTab(NavigationTab.HOME)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SearchResultCard(
    title: String,
    subtitle: String,
    category: String,
    latitude: Double,
    longitude: Double,
    onSelectOnMap: () -> Unit,
    onTeleport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineHairline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryCyan
                    )
                }
            }

            // Koordinat Pill
            Text(
                text = "${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SecondaryBlue
            )

            // Aksi Tombol
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
                        containerColor = PrimaryCyan,
                        contentColor = Color(0xFF003543)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Teleport", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
