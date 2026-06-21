package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Site
import com.example.ui.SolarViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesListScreen(
    viewModel: SolarViewModel,
    onNavigateToDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sites by viewModel.sites.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    var activeSubTab by remember { mutableStateOf("Solar Plants") } // "Solar Plants" or "Explore Weather"
    var prefilledName by remember { mutableStateOf("") }
    var prefilledLat by remember { mutableStateOf("") }
    var prefilledLng by remember { mutableStateOf("") }

    // Filter sites based on tab selected and search query
    val filteredSites = remember(sites, selectedFilter, searchQuery) {
        val tabFiltered = when (selectedFilter) {
            "Active" -> sites.filter { it.status == "Active" }
            "Alerts" -> sites.filter { it.status == "Storm mode" || it.status == "Fault" }
            "Offline" -> sites.filter { it.status == "Offline" }
            else -> sites
        }
        if (searchQuery.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (activeSubTab == "Solar Plants") "Solar Plants" else "Global Meteorology",
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NaturalBg
                )
            )
        },
        floatingActionButton = {
            if (activeSubTab == "Solar Plants") {
                FloatingActionButton(
                    onClick = {
                        prefilledName = ""
                        prefilledLat = ""
                        prefilledLng = ""
                        showAddDialog = true
                    },
                    containerColor = NaturalGreenAccent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_site_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom site"
                    )
                }
            }
        },
        containerColor = NaturalBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(NaturalBg)
        ) {
            // High visibility Sub-tabs: Solar Plants vs Explore Weather
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, NaturalBorder), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Solar Plants", "Explore Any Weather").forEach { tabName ->
                    val isTabSelected = activeSubTab == tabName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTabSelected) NaturalGreenAccent else Color.Transparent)
                            .clickable { activeSubTab = tabName }
                            .padding(vertical = 10.dp)
                            .testTag("sub_tab_" + tabName.replace(" ", "_")),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (tabName == "Solar Plants") Icons.Default.SolarPower else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (isTabSelected) Color.White else NaturalTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tabName,
                                color = if (isTabSelected) Color.White else NaturalTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (activeSubTab == "Solar Plants") {
                // Sleek Search Bar for Quick Filtering
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_bar"),
                    placeholder = {
                        Text(
                            text = "Search client sites...",
                            color = NaturalTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NaturalTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = NaturalTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = NaturalGreenAccent,
                        unfocusedBorderColor = NaturalBorder,
                        focusedTextColor = NaturalTextPrimary,
                        unfocusedTextColor = NaturalTextPrimary,
                        cursorColor = NaturalGreenAccent
                    )
                )

                // High visibility horizontal filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Active", "Alerts", "Offline").forEach { filterName ->
                        val isSelected = selectedFilter == filterName
                        val chipColor = if (isSelected) NaturalGreenAccent else NaturalGreenPill
                        val textColor = if (isSelected) Color.White else NaturalGreenDarkText

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipColor)
                                .clickable { selectedFilter = filterName }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("filter_chip_$filterName")
                        ) {
                            Text(
                                text = filterName,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Text(
                    text = "LICENSED RAJKOT SITES (${filteredSites.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NaturalTextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                )

                // Dynamic grid layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredSites, key = { it.id }) { site ->
                        SiteCard(
                            site = site,
                            onClick = {
                                viewModel.selectSite(site.id)
                                onNavigateToDetail()
                            }
                        )
                    }
                }
            } else {
                // --- Explore Any Weather Tab Layout ---
                val exploreSearchQuery by viewModel.exploreSearchQuery.collectAsState()
                val searchResults by viewModel.searchResults.collectAsState()
                val isSearching by viewModel.isSearching.collectAsState()

                val exploredWeather by viewModel.exploredWeather.collectAsState()
                val isExploreLoading by viewModel.isExploreLoading.collectAsState()
                val exploredLocationName by viewModel.exploredLocationName.collectAsState()
                val exploredLat by viewModel.exploredLat.collectAsState()
                val exploredLng by viewModel.exploredLng.collectAsState()

                var manualLat by remember { mutableStateOf("") }
                var manualLng by remember { mutableStateOf("") }
                var manualError by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "GLOBAL METEOROLOGY FINDER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NaturalTextSecondary,
                        letterSpacing = 1.sp
                    )

                    // Card for Search & Coordinate Input
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, NaturalBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Search by City or Region Name",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NaturalTextPrimary
                            )

                            OutlinedTextField(
                                value = exploreSearchQuery,
                                onValueChange = { viewModel.updateExploreQuery(it) },
                                modifier = Modifier.fillMaxWidth().testTag("explore_name_input"),
                                placeholder = { Text("e.g. Paris, Tokyo, Mumbai", fontSize = 12.sp, color = NaturalTextSecondary) },
                                leadingIcon = { Icon(Icons.Default.LocationCity, null, modifier = Modifier.size(18.dp), tint = NaturalTextSecondary) },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NaturalGreenAccent,
                                    unfocusedBorderColor = NaturalBorder
                                )
                            )

                            if (isSearching) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                    color = NaturalGreenAccent
                                )
                            }

                            if (searchResults.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NaturalBg),
                                    border = BorderStroke(1.dp, NaturalBorder.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                                ) {
                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        items(searchResults) { result ->
                                            Text(
                                                text = "${result.name} (${result.admin1 ?: ""}, ${result.country ?: ""}) - ${String.format("%.2f", result.latitude)}, ${String.format("%.2f", result.longitude)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NaturalGreenDarkText,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.loadExploredWeather(
                                                            "${result.name}, ${result.country ?: ""}",
                                                            result.latitude,
                                                            result.longitude
                                                        )
                                                        viewModel.updateExploreQuery("")
                                                    }
                                                    .padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = NaturalBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Or Search Specific Coordinates",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NaturalTextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualLat,
                                    onValueChange = {
                                        manualLat = it
                                        manualError = ""
                                    },
                                    label = { Text("Latitude", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("explore_lat_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NaturalGreenAccent,
                                        unfocusedBorderColor = NaturalBorder
                                    )
                                )

                                OutlinedTextField(
                                    value = manualLng,
                                    onValueChange = {
                                        manualLng = it
                                        manualError = ""
                                    },
                                    label = { Text("Longitude", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("explore_lng_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NaturalGreenAccent,
                                        unfocusedBorderColor = NaturalBorder
                                    )
                                )

                                Button(
                                    onClick = {
                                        val latD = manualLat.trim().toDoubleOrNull()
                                        val lngD = manualLng.trim().toDoubleOrNull()
                                        if (latD == null || latD < -90.0 || latD > 90.0) {
                                            manualError = "Invalid Lat (-90 to 90)"
                                        } else if (lngD == null || lngD < -180.0 || lngD > 180.0) {
                                            manualError = "Invalid Lng (-180 to 180)"
                                        } else {
                                            viewModel.loadExploredWeather("Custom Location", latD, lngD)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NaturalGreenAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("explore_coord_search_button")
                                ) {
                                    Icon(Icons.Default.Search, "Search coords", modifier = Modifier.size(18.dp))
                                }
                            }

                            if (manualError.isNotEmpty()) {
                                Text(manualError, color = NaturalAlertAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Curated Quick Locations PRESETS
                    Text(
                        text = "POPULAR METEOROLOGICAL LOCATIONS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NaturalTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    val worldPresets = listOf(
                        Triple("London, United Kingdom", 51.5074, -0.1278),
                        Triple("New York, USA", 40.7128, -74.0060),
                        Triple("Sahara Desert, Africa", 23.8000, 11.3000),
                        Triple("Tokyo, Japan", 35.6762, 139.6503),
                        Triple("Atacama Desert, CL", -23.8634, -69.1359),
                        Triple("Sydney, Australia", -33.8688, 151.2093),
                        Triple("Srinagar, Kashmir", 34.0837, 74.7973)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        worldPresets.forEach { (label, lat, lng) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(NaturalGreenPill)
                                    .clickable {
                                        viewModel.loadExploredWeather(label, lat, lng)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label.substringBefore(","),
                                    fontSize = 12.sp,
                                    color = NaturalGreenDarkText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Weather Result Display Card
                    if (isExploreLoading) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, NaturalBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = NaturalGreenAccent, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Fetching Meteorological Telemetry...", fontSize = 13.sp, color = NaturalTextSecondary)
                            }
                        }
                    } else if (exploredWeather != null) {
                        val w = exploredWeather!!
                        val hourly = w.hourly
                        val currentTemp = hourly?.temperature2m?.firstOrNull() ?: 15.0
                        val currentWind = hourly?.windSpeed10m?.firstOrNull() ?: 10.0
                        val currentCloud = hourly?.cloudCover?.firstOrNull() ?: 0.0
                        val currentRain = hourly?.precipitationProbability?.firstOrNull() ?: 0.0
                        val currentGHI = hourly?.shortwaveRadiation?.firstOrNull() ?: 0.0

                        // Determine condition name and icon
                        val condition = when {
                            currentGHI < 100 && currentCloud > 80 -> "cloudy"
                            currentRain > 50 -> "rain"
                            currentWind > 45 && currentRain > 30 -> "storm"
                            else -> "sunny"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, NaturalBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("explored_weather_result")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Title Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exploredLocationName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = NaturalTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Coordinates: ${String.format("%.4f", exploredLat)}, ${String.format("%.4f", exploredLng)}",
                                            fontSize = 12.sp,
                                            color = NaturalTextSecondary
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (condition) {
                                                    "storm" -> NaturalAlertBg
                                                    "rain" -> NaturalGreenPill
                                                    "cloudy" -> NaturalGrayNav
                                                    else -> LightYellowBg
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = condition.uppercase(),
                                            color = when (condition) {
                                                "storm" -> NaturalAlertAccent
                                                "rain" -> NaturalGreenDarkText
                                                "cloudy" -> NaturalTextSecondary
                                                else -> SolarYellow
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Large Temperature Indicator and Main Graphic
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (condition) {
                                            "storm" -> Icons.Default.Thunderstorm
                                            "rain" -> Icons.Default.Umbrella
                                            "cloudy" -> Icons.Default.CloudQueue
                                            else -> Icons.Default.WbSunny
                                        },
                                        contentDescription = "Condition",
                                        tint = when (condition) {
                                            "storm" -> NaturalAlertAccent
                                            "rain" -> SolarBlue
                                            "cloudy" -> NaturalMuted
                                            else -> SolarYellow
                                        },
                                        modifier = Modifier.size(54.dp)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = "${String.format("%.1f", currentTemp)}°C",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 32.sp,
                                            color = NaturalTextPrimary
                                        )
                                        Text(
                                            text = "GHI Solar Power: ${String.format("%.0f", currentGHI)} W/m²",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NaturalGreenAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                HorizontalDivider(color = NaturalBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Grid of meteorological details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Outlined.WbSunny, "GHI", tint = SolarYellow, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Solar GHI", fontSize = 10.sp, color = NaturalTextSecondary)
                                        Text("${String.format("%.0f", currentGHI)} W", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextPrimary)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Outlined.Air, "Wind", tint = NaturalGreenAccent, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Wind", fontSize = 10.sp, color = NaturalTextSecondary)
                                        Text("${String.format("%.1f", currentWind)} km/h", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextPrimary)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Outlined.Cloud, "Cloud", tint = NaturalMuted, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Cloud Cover", fontSize = 10.sp, color = NaturalTextSecondary)
                                        Text("${String.format("%.0f", currentCloud)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextPrimary)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Outlined.WaterDrop, "Rain", tint = SolarBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Rain Prob", fontSize = 10.sp, color = NaturalTextSecondary)
                                        Text("${String.format("%.0f", currentRain)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Solar Panel Feasibility Check Card
                                val viabilityLabel = when {
                                    currentGHI > 500 && currentWind < 20 -> "EXCELLENT SOLAR VIABILITY"
                                    currentGHI > 200 && currentWind < 35 -> "HIGH SOLAR VIABILITY"
                                    currentGHI > 50 && currentWind < 45 -> "MODERATE VIABILITY"
                                    currentWind >= 45 -> "HIGH WIND RISKS (STOW REQUIRED!)"
                                    else -> "LOW SOLAR SENSITIVITY (STORM/NIGHT)"
                                }

                                val viabilityColor = when {
                                    currentGHI > 200 && currentWind < 35 -> NaturalGreenAccent
                                    currentWind >= 45 -> NaturalAlertAccent
                                    else -> SolarGray
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = viabilityColor.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, viabilityColor.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (viabilityColor == NaturalGreenAccent) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = viabilityColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = viabilityLabel,
                                            color = viabilityColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Hourly forecast horizontal scroll inside result card
                                Text(
                                    text = "24-HOUR CLIMATE TRENDS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NaturalTextSecondary,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val times = hourly?.time ?: emptyList()
                                    val temps = hourly?.temperature2m ?: emptyList()
                                    val clouds = hourly?.cloudCover ?: emptyList()
                                    val solar = hourly?.shortwaveRadiation ?: emptyList()

                                    times.take(24).forEachIndexed { index, timeStr ->
                                        val temp = temps.getOrNull(index) ?: 20.0
                                        val sol = solar.getOrNull(index) ?: 0.0

                                        val rawHour = timeStr.substringAfter("T").substringBefore(":")
                                        val hrInt = rawHour.toIntOrNull() ?: 0
                                        val formattedHour = when {
                                            hrInt == 0 -> "12 AM"
                                            hrInt == 12 -> "12 PM"
                                            hrInt > 12 -> "${hrInt - 12} PM"
                                            else -> "$hrInt AM"
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NaturalBg)
                                                .border(BorderStroke(1.dp, NaturalBorder.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(formattedHour, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("${String.format("%.0f", temp)}°C", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = NaturalTextPrimary)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Bolt, null, tint = SolarYellow, modifier = Modifier.size(10.dp))
                                                    Text("${String.format("%.0f", sol)} W", fontSize = 9.sp, color = NaturalGreenAccent)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // CTA Button: "Install Solar Tracker Here"
                                Button(
                                    onClick = {
                                        prefilledName = exploredLocationName
                                        prefilledLat = exploredLat.toString()
                                        prefilledLng = exploredLng.toString()
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("setup_tracker_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NaturalGreenAccent)
                                ) {
                                    Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Install Co-located Solar Plant Here", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        // Empty Exploration Prompt
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, NaturalBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.Cloud, null, tint = NaturalGreenAccent.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No meteorology explored yet. Let's enter a city or tap any preset above to instantly load live global metrics!",
                                    fontSize = 13.sp,
                                    color = NaturalTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showAddDialog) {
        var addName by remember { mutableStateOf(prefilledName) }
        var addLat by remember { mutableStateOf(prefilledLat) }
        var addLng by remember { mutableStateOf(prefilledLng) }
        var errorText by remember { mutableStateOf("") }
        var hasSolarPlant by remember { mutableStateOf(true) }
        var isManualSetup by remember { mutableStateOf(false) }

        // Manual setup parameters
        var manualTemp by remember { mutableStateOf("25.0") }
        var manualWind by remember { mutableStateOf("12.0") }
        var manualCloud by remember { mutableStateOf("15.0") }
        var manualGhi by remember { mutableStateOf("450.0") }
        var manualStatus by remember { mutableStateOf("Active") } // Active, Fault, Offline

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Solar Plant/Site Manually",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NaturalTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Design a solar tracker plant or pure meteorological station. You can either auto-fetch dynamic weather or manually configure fixed telemetry values.",
                        fontSize = 11.sp,
                        color = NaturalTextSecondary,
                        lineHeight = 15.sp
                    )

                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Site/Plant Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_site_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addLat,
                            onValueChange = { addLat = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_site_lat")
                        )
                        OutlinedTextField(
                            value = addLng,
                            onValueChange = { addLng = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_site_lng")
                        )
                    }

                    HorizontalDivider(color = NaturalBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                    // Toggle: Has Solar Plant trackers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = "Install Solar plant array",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NaturalTextPrimary
                            )
                            Text(
                                text = "Allows full tracker status, tilt controls & predictive timelines",
                                fontSize = 10.sp,
                                color = NaturalTextSecondary,
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = hasSolarPlant,
                            onCheckedChange = { hasSolarPlant = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NaturalGreenAccent)
                        )
                    }

                    // Toggle: Manual Setup mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = "Manual Setup & Telemetry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NaturalTextPrimary
                            )
                            Text(
                                text = "Define static custom metrics instead of fetching from Open-Meteo",
                                fontSize = 10.sp,
                                color = NaturalTextSecondary,
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = isManualSetup,
                            onCheckedChange = { isManualSetup = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NaturalGreenAccent)
                        )
                    }

                    // Expansible manual fields
                    if (isManualSetup) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NaturalBorder.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "MANUAL TELEMETRY PARAMETERS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NaturalTextSecondary,
                                    letterSpacing = 0.5.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = manualTemp,
                                        onValueChange = { manualTemp = it },
                                        label = { Text("Temp (°C)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = manualWind,
                                        onValueChange = { manualWind = it },
                                        label = { Text("Wind (km/h)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = manualCloud,
                                        onValueChange = { manualCloud = it },
                                        label = { Text("Cloud (%)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = manualGhi,
                                        onValueChange = { manualGhi = it },
                                        label = { Text("Solar GHI") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    text = "INITIAL STATUS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NaturalTextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Active", "Fault", "Offline").forEach { status ->
                                        val isSelected = manualStatus == status
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) NaturalGreenAccent else NaturalBorder.copy(alpha = 0.3f))
                                                .clickable { manualStatus = status }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else NaturalTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = NaturalAlertAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "QUICK PRESET LOCATIONS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NaturalTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    val presets = listOf(
                        Triple("Rajkot Solar Center", 22.3039, 70.8022),
                        Triple("Mojave Array", 35.0110, -118.1720),
                        Triple("Atacama Solar Hub", -23.8634, -69.1359),
                        Triple("Bhadla Power Unit", 27.5397, 71.9189)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, lat, lng) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NaturalGreenPill)
                                    .clickable {
                                        addName = label
                                        addLat = lat.toString()
                                        addLng = lng.toString()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = NaturalGreenDarkText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val latVal = addLat.toDoubleOrNull()
                        val lngVal = addLng.toDoubleOrNull()
                        if (addName.isBlank()) {
                            errorText = "Name cannot be blank"
                        } else if (latVal == null || latVal < -90.0 || latVal > 90.0) {
                            errorText = "Latitude must be between -90 and 90"
                        } else if (lngVal == null || lngVal < -180.0 || lngVal > 180.0) {
                            errorText = "Longitude must be between -180 and 180"
                        } else if (isManualSetup) {
                            val tempVal = manualTemp.toDoubleOrNull()
                            val windVal = manualWind.toDoubleOrNull()
                            val cloudVal = manualCloud.toDoubleOrNull()
                            val ghiVal = manualGhi.toDoubleOrNull()
                            if (tempVal == null) {
                                errorText = "Temperature must be a valid number"
                            } else if (windVal == null || windVal < 0) {
                                errorText = "Wind speed must be 0 or higher"
                            } else if (cloudVal == null || cloudVal < 0 || cloudVal > 100) {
                                errorText = "Cloud cover must be between 0% and 100%"
                            } else if (ghiVal == null || ghiVal < 0) {
                                errorText = "Solar GHI must be 0 or higher"
                            } else {
                                viewModel.insertCustomSite(
                                    name = addName,
                                    latitude = latVal,
                                    longitude = lngVal,
                                    hasSolarPlant = hasSolarPlant,
                                    isManualSetup = isManualSetup,
                                    temp = tempVal,
                                    wind = windVal,
                                    cloud = cloudVal,
                                    ghi = ghiVal,
                                    status = manualStatus
                                )
                                showAddDialog = false
                            }
                        } else {
                            viewModel.insertCustomSite(
                                name = addName,
                                latitude = latVal,
                                longitude = lngVal,
                                hasSolarPlant = hasSolarPlant,
                                isManualSetup = isManualSetup
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalGreenAccent)
                ) {
                    Text("Add Site", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = NaturalTextSecondary)
                }
            }
        )
    }  }
}

@Composable
fun SiteCard(
    site: Site,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, NaturalBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("site_card_${site.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = site.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = NaturalTextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (site.isManualSetup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SolarOrangeDark.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MANUAL",
                                    fontSize = 8.sp,
                                    color = SolarOrangeDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gujarat Area Grid • ${String.format("%.3f", site.latitude)}, ${String.format("%.3f", site.longitude)}",
                        fontSize = 11.sp,
                        color = NaturalTextSecondary
                    )
                }
                StatusBadgeMini(site.status)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = NaturalBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Short summary panel metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Solar and Wind highlights
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sun highlight
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "Power GHI",
                            tint = SolarYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.0f", site.currentSolarGHI)} W/m²",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextPrimary
                        )
                    }

                    // Wind highlight
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Air,
                            contentDescription = "Wind speed",
                            tint = if (site.currentWindSpeed > 50) NaturalAlertAccent else NaturalGreenAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", site.currentWindSpeed)} km/h",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextPrimary
                        )
                    }
                }

                // Show target tracker pivoting mode or weather station indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (site.hasSolarPlant) getModeColor(site.currentMode).copy(alpha = 0.12f) else SolarBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (site.hasSolarPlant) site.currentMode.uppercase() else "WEATHER ONLY",
                        color = if (site.hasSolarPlant) getModeColor(site.currentMode) else SolarBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
