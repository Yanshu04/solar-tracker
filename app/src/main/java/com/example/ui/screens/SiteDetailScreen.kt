package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun SiteDetailScreen(
    viewModel: SolarViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSite by viewModel.selectedSite.collectAsState()
    val forecastList by viewModel.selectedSiteForecast.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Site Operations",
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NaturalBg
                )
            )
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
            if (selectedSite == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PinDrop, "Pin", tint = NaturalTextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Select a client site from list tab to view operations.",
                            fontSize = 14.sp,
                            color = NaturalTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                val site = selectedSite!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Site Name & Location header
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, NaturalBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("site_detail_header")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = site.name,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 22.sp,
                                                color = NaturalTextPrimary,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { showEditDialog = true },
                                                modifier = Modifier.size(24.dp).testTag("edit_location_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit site location",
                                                    tint = NaturalTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Site Coordinates • ${site.latitude}, ${site.longitude}",
                                            fontSize = 12.sp,
                                            color = NaturalTextSecondary
                                        )
                                    }
                                    StatusBadgeLarge(site.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = NaturalBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DetailHeaderMetric("Temp", "${String.format("%.1f", site.currentTemp)}°C", NaturalAlertAccent)
                                    DetailHeaderMetric("Wind", "${String.format("%.1f", site.currentWindSpeed)} km/h", SolarBlue)
                                    DetailHeaderMetric("GHI", "${String.format("%.0f", site.currentSolarGHI)} W/m²", SolarYellow)
                                    if (site.hasSolarPlant) {
                                        DetailHeaderMetric("Angle", "${String.format("%.0f", site.currentAngle)}°", getModeColor(site.currentMode))
                                    } else {
                                        DetailHeaderMetric("Cloud", "${String.format("%.0f", site.currentCloudCover)}%", getModeColor(site.currentMode))
                                    }
                                }
                            }
                        }
                    }

                    // Manual overrides label
                    if (site.hasSolarPlant) {
                        item {
                            Text(
                                text = "MANUAL MOTOR OVERRIDES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NaturalTextSecondary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        // Overrides command buttons
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, NaturalBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Send structural override commands to drivers. Stowing or holding manually stops automatic tracking logic until set back to AUTO.",
                                        fontSize = 12.sp,
                                        color = NaturalTextSecondary,
                                        lineHeight = 16.sp
                                    )

                                    val activeOverride = site.manualOverride // "None", "Follow", "Hold", "Stow"

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OverrideButton(
                                            label = "AUTO",
                                            active = activeOverride == "None",
                                            activeBgColor = NaturalGreenAccent,
                                            onClick = { viewModel.applyManualOverride(site.id, "None") },
                                            modifier = Modifier.weight(1f).testTag("override_auto")
                                        )
                                        OverrideButton(
                                            label = "FOLLOW",
                                            active = activeOverride == "Follow",
                                            activeBgColor = SolarBlue,
                                            onClick = { viewModel.applyManualOverride(site.id, "Follow") },
                                            modifier = Modifier.weight(1f).testTag("override_follow")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OverrideButton(
                                            label = "HOLD",
                                            active = activeOverride == "Hold",
                                            activeBgColor = SolarOrangeDark,
                                            onClick = { viewModel.applyManualOverride(site.id, "Hold") },
                                            modifier = Modifier.weight(1f).testTag("override_hold")
                                        )
                                        OverrideButton(
                                            label = "STOW",
                                            active = activeOverride == "Stow",
                                            activeBgColor = NaturalAlertAccent,
                                            onClick = { viewModel.applyManualOverride(site.id, "Stow") },
                                            modifier = Modifier.weight(1f).testTag("override_stow")
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NaturalBorder.copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, NaturalBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Meteorology Only",
                                        tint = SolarBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "METEOROLOGY RESEARCH STATION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SolarBlue,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "This saving point is configured as a pure atmospheric tracking hub. There are no solar photovoltaic tracking motors or panel tilting gear installed here.",
                                            fontSize = 12.sp,
                                            color = NaturalTextPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Forecast schedule subtitle
                    item {
                        Text(
                            text = if (site.hasSolarPlant) "TOMORROW'S PREDICTIVE TRACKING SCHEDULE" else "TOMORROW'S METEOROLOGICAL FORECAST",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NaturalTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }

                    // Compact hourly timeline inside detail screen or empty placeholder
                    if (forecastList.isEmpty()) {
                        item {
                            Text(
                                "No tracking schedule cached for this site.",
                                fontSize = 12.sp,
                                color = NaturalTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(forecastList.take(6)) { hourRow ->
                            ForecastHourRow(hourRow)
                        }
                        item {
                            Text(
                                text = if (site.hasSolarPlant) {
                                    "Showing first 6 daylight plan hours. Switch to Tomorrow tab to view the complete 5:00 AM to 08:00 PM timeline."
                                } else {
                                    "Showing tomorrow's atmospheric predictions. Full hour list is accessible in the Tomorrow tab."
                                },
                                fontSize = 11.sp,
                                color = NaturalTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
      if (showEditDialog && selectedSite != null) {
        val site = selectedSite!!
        var editName by remember { mutableStateOf(site.name) }
        var editLat by remember { mutableStateOf(site.latitude.toString()) }
        var editLng by remember { mutableStateOf(site.longitude.toString()) }
        var errorText by remember { mutableStateOf("") }
        var hasSolarPlant by remember { mutableStateOf(site.hasSolarPlant) }
        var isManualSetup by remember { mutableStateOf(site.isManualSetup) }

        // Manual setup parameters prefilled
        var manualTemp by remember { mutableStateOf(site.currentTemp.toString()) }
        var manualWind by remember { mutableStateOf(site.currentWindSpeed.toString()) }
        var manualCloud by remember { mutableStateOf(site.currentCloudCover.toString()) }
        var manualGhi by remember { mutableStateOf(site.currentSolarGHI.toString()) }
        var manualStatus by remember { mutableStateOf(site.status) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Configure Tracker / Site Settings",
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
                        text = "Customize site parameters, install solar tracking PV hardware, or choose static manual telemetry values.",
                        fontSize = 11.sp,
                        color = NaturalTextSecondary,
                        lineHeight = 15.sp
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Site/Plant Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_site_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editLat,
                            onValueChange = { editLat = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("edit_site_lat")
                        )
                        OutlinedTextField(
                            value = editLng,
                            onValueChange = { editLng = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("edit_site_lng")
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
                                text = "Enables predictive structural tracker stowing & dual-axis metrics",
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
                                text = "Bypass Open-Meteo forecasts and force steady telemetry values",
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
                                    text = "MANUAL TELEMETRY OVERRIDES",
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
                                    text = "CURRENT ENGINE STATUS",
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
                                        editName = label
                                        editLat = lat.toString()
                                        editLng = lng.toString()
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
                        val latVal = editLat.toDoubleOrNull()
                        val lngVal = editLng.toDoubleOrNull()
                        if (editName.isBlank()) {
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
                                viewModel.updateSiteLocation(
                                    siteId = site.id,
                                    name = editName,
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
                                showEditDialog = false
                            }
                        } else {
                            viewModel.updateSiteLocation(
                                siteId = site.id,
                                name = editName,
                                latitude = latVal,
                                longitude = lngVal,
                                hasSolarPlant = hasSolarPlant,
                                isManualSetup = isManualSetup
                            )
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalGreenAccent)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = NaturalTextSecondary)
                }
            }
        )
    }    }
}

@Composable
fun DetailHeaderMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = NaturalTextSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun OverrideButton(
    label: String,
    active: Boolean,
    activeBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) activeBgColor else NaturalGreenPill,
            contentColor = if (active) Color.White else NaturalGreenDarkText
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )
    }
}

