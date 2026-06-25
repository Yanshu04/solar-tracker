package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ForecastHourEntity
import com.example.ui.SolarViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    viewModel: SolarViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSite by viewModel.selectedSite.collectAsState()
    val forecastList by viewModel.selectedSiteForecast.collectAsState()
    val aiInsight by viewModel.aiInsight.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    // Determine high risk hours
    val hasStormRisk = forecastList.any { it.weatherCondition == "storm" || it.actionColor == "red" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Tomorrow's Panel Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NaturalTextPrimary
                        )
                        selectedSite?.let {
                            Text(
                                "Forecast for ${it.name}",
                                fontSize = 12.sp,
                                color = NaturalTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NaturalBg
                ),
                actions = {
                    IconButton(
                        onClick = {
                            selectedSite?.let { site ->
                                viewModel.generateAiInsight(site, forecastList)
                            }
                        },
                        enabled = !isAiLoading && selectedSite != null && forecastList.isNotEmpty()
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NaturalGreenAccent, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Generate AI Insight", tint = NaturalGreenAccent)
                        }
                    }
                }
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
            // LazyColumn to encompass both headers (as items/widgets) and prediction rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // AI Insight Section
                aiInsight?.let { insight ->
                    item {
                        Text(
                            text = "AI SITE ANALYSIS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NaturalTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NaturalGreenAccent.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, NaturalGreenAccent.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NaturalGreenAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = insight,
                                    fontSize = 13.sp,
                                    color = NaturalTextPrimary,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Header Label
                item {
                    Text(
                        text = "SOLAR PERFORMANCE SUMMARIES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NaturalTextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Grid of 4 summaries in 2 rows
                item {
                    val sunrise = selectedSite?.let { site ->
                        val base = 6.0 - (site.longitude / 15.0 % 1.0)
                        val hr = base.toInt()
                        val min = ((base - hr) * 60).toInt()
                        String.format("%02d:%02d AM", hr, min)
                    } ?: "06:00 AM"

                    val sunset = selectedSite?.let { site ->
                        val base = 18.0 - (site.longitude / 15.0 % 1.0)
                        val hr = base.toInt() - 12
                        val min = ((base - base.toInt()) * 60).toInt()
                        String.format("%02d:%02d PM", hr, min)
                    } ?: "06:00 PM"

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryCard(
                                icon = Icons.Default.WbTwilight,
                                title = "Sunrise",
                                value = sunrise,
                                labelColor = SolarYellow,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                icon = Icons.Default.NightsStay,
                                title = "Sunset",
                                value = sunset,
                                labelColor = SolarBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryCard(
                                icon = Icons.Default.SolarPower,
                                title = "Peak Sun",
                                value = "12 PM - 2 PM",
                                labelColor = NaturalGreenAccent,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                icon = Icons.Default.Security,
                                title = "Storm Risk",
                                value = if (hasStormRisk) "HIGH" else "LOW RISK",
                                labelColor = if (hasStormRisk) NaturalAlertAccent else NaturalGreenAccent,
                                isBoldValue = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Title label for timeline
                item {
                    Text(
                        text = "HOUR-BY-HOUR PLAN (05:00 AM - 08:00 PM)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NaturalTextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                // Empty state or Hourly List Items
                if (forecastList.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, NaturalBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Text(
                                "No prediction cached. Press refresh at dashboard top-right to sync telemetry data.",
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = NaturalTextSecondary
                            )
                        }
                    }
                } else {
                    items(forecastList) { hourItem ->
                        ForecastHourRow(hourItem)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    labelColor: Color,
    isBoldValue: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, NaturalBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(labelColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = labelColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    fontSize = 11.sp,
                    color = NaturalTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    value,
                    fontSize = 13.sp,
                    color = if (isBoldValue) labelColor else NaturalTextPrimary,
                    fontWeight = if (isBoldValue) FontWeight.ExtraBold else FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ForecastHourRow(item: ForecastHourEntity) {
    val rowColor = when (item.actionColor.lowercase()) {
        "green" -> NaturalGreenAccent
        "blue" -> SolarBlue
        "orange" -> SolarOrangeDark
        "red" -> NaturalAlertAccent
        else -> NaturalMuted
    }

    val actionLabel = when (item.panelAction) {
        "Auto stow (Storm)" -> "AUTO STOW"
        "Auto stow (Rain)" -> "AUTO STOW"
        else -> item.panelAction.uppercase()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NaturalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("forecast_row_${item.hourTime.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High visibility Left color-coded vertical strip indicator
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(rowColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Time & Weather Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (item.weatherCondition) {
                            "storm" -> Icons.Default.Thunderstorm
                            "rain" -> Icons.Default.Umbrella
                            "cloudy" -> Icons.Default.CloudQueue
                            else -> Icons.Default.WbSunny
                        },
                        contentDescription = "Condition ${item.weatherCondition}",
                        tint = when (item.weatherCondition) {
                            "storm" -> NaturalAlertAccent
                            "rain" -> SolarBlue
                            "cloudy" -> NaturalMuted
                            else -> SolarYellow
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.hourTime,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = NaturalTextPrimary
                        )
                        val ghiDisplay = if (item.solarGHI > 0) "${String.format("%.0f", item.solarGHI)} W/m²" else "No Sunlight"
                        Text(
                            text = "GHI: $ghiDisplay • ${String.format("%.0f", item.temperature)}°C",
                            fontSize = 11.sp,
                            color = NaturalTextSecondary
                        )
                    }
                }

                // Action indicator on the right
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(rowColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = actionLabel,
                        color = rowColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
