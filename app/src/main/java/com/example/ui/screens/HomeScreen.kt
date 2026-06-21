package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Site
import com.example.ui.SolarViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SolarViewModel,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sites by viewModel.sites.collectAsState()
    val selectedSite by viewModel.selectedSite.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val alerts by viewModel.alerts.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }

    val activeStorms = sites.filter { it.status == "Storm mode" || it.currentWindSpeed > 50.0 }
    val isStormActive = activeStorms.isNotEmpty()

    val formatTime = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Solar Dashboard",
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NaturalBg
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshWeather() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = NaturalGreenAccent)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NaturalTextPrimary)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storm Warning Banner
            AnimatedVisibility(visible = isStormActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NaturalAlertBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NaturalAlertBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAlerts() }
                        .testTag("storm_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(NaturalAlertAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Storm warning icon",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "CRITICAL STORM EMERGENCY",
                                color = NaturalAlertDarkText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Morbi & nearby arrays stowed. High wind warning active.",
                                color = NaturalAlertSubText,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Go to emergency protocols",
                            tint = NaturalAlertAccent
                        )
                    }
                }
            }

            // Site Selector Dropdown
            selectedSite?.let { site ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                            .testTag("home_site_dropdown_trigger"),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, NaturalBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location pointer icon",
                                tint = SolarBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = site.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = NaturalTextPrimary
                                )
                                Text(
                                    text = "Lat: ${site.latitude}, Lng: ${site.longitude} • Rajkot",
                                    fontSize = 12.sp,
                                    color = NaturalTextSecondary
                                )
                            }
                            Icon(
                                if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown icon",
                                tint = NaturalTextPrimary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color.White)
                    ) {
                        sites.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            s.name,
                                            fontWeight = if (s.id == site.id) FontWeight.Bold else FontWeight.Normal,
                                            color = NaturalTextPrimary
                                        )
                                        StatusBadgeMini(s.status)
                                    }
                                },
                                onClick = {
                                    viewModel.selectSite(s.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Live Weather Meter Card
            selectedSite?.let { site ->
                Text(
                    text = "LIVE DATA & WEATHER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NaturalTextSecondary,
                    letterSpacing = 1.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NaturalBorder, RoundedCornerShape(12.dp))
                        .testTag("weather_metrics_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header temperature & status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.WbSunny,
                                    contentDescription = "sun weather",
                                    tint = SolarYellow,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${String.format("%.1f", site.currentTemp)}°C",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NaturalTextPrimary
                                )
                            }
                            StatusBadgeLarge(site.status)
                        }

                        HorizontalDivider(color = NaturalBorder.copy(alpha = 0.5f))

                        // Standard 4 metrics with outdoor bold legible layout
                        Row(modifier = Modifier.fillMaxWidth()) {
                            WeatherMetricItem(
                                icon = Icons.Default.Bolt,
                                label = "Sunlight",
                                value = "${String.format("%.0f", site.currentSolarGHI)} W/m²",
                                color = SolarYellow,
                                modifier = Modifier.weight(1f)
                            )
                            VerticalDivider()
                            WeatherMetricItem(
                                icon = Icons.Default.Air,
                                label = "Wind Speed",
                                value = "${String.format("%.1f", site.currentWindSpeed)} km/h",
                                color = if (site.currentWindSpeed > 50.0) SolarRed else if (site.currentWindSpeed >= 30) SolarOrangeDark else SolarBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            WeatherMetricItem(
                                icon = Icons.Default.Cloud,
                                label = "Cloud Cover",
                                value = "${String.format("%.0f", site.currentCloudCover)}%",
                                color = SolarGray,
                                modifier = Modifier.weight(1f)
                            )
                            VerticalDivider()
                            WeatherMetricItem(
                                icon = Icons.Default.SettingsInputAntenna,
                                label = "Coordinates",
                                value = "${String.format("%.3f", site.latitude)}, ${String.format("%.3f", site.longitude)}",
                                color = SolarGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Solar Pivot Angle Visualizer Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, NaturalBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PANEL TILT CONTROL POSITION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NaturalTextSecondary
                                )
                                Text(
                                    text = site.currentMode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = getModeColor(site.currentMode)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(getModeColor(site.currentMode).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(getModeColor(site.currentMode))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${String.format("%.1f", site.currentAngle)}°",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = getModeColor(site.currentMode)
                                )
                            }
                        }

                        // Drawing Pivot Graphic
                        SolarAngleWidget(
                            angle = site.currentAngle,
                            mode = site.currentMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("-50° East", fontSize = 11.sp, color = NaturalTextSecondary, fontWeight = FontWeight.SemiBold)
                            Text("0° Horizon (Stow)", fontSize = 11.sp, color = NaturalTextSecondary, fontWeight = FontWeight.SemiBold)
                            Text("+50° West", fontSize = 11.sp, color = NaturalTextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Update Info and Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = "clock time",
                            tint = NaturalTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Last sync: ${formatTime.format(Date(site.lastUpdated))}",
                            fontSize = 11.sp,
                            color = NaturalTextSecondary
                        )
                    }

                    if (site.manualOverride != "None") {
                        SuggestionChip(
                            onClick = { viewModel.applyManualOverride(site.id, "None") },
                            label = { Text("Reset Override") },
                            icon = { Icon(Icons.Default.Close, contentDescription = "Clear override", modifier = Modifier.size(12.dp)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = NaturalAlertAccent
                            )
                        )
                    }
                }
            }
        }
    }
}

// Custom Pivoting Solar Panel Widget Graphics
@Composable
fun SolarAngleWidget(
    angle: Float,
    mode: String,
    modifier: Modifier = Modifier
) {
    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "angleAnimator"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Ground line
        val groundY = height * 0.85f
        drawLine(
            color = Color(0xFFCFD8DC),
            start = Offset(0f, groundY),
            end = Offset(width, groundY),
            strokeWidth = 3f
        )

        // Draw structural solar foundation pillar (Rajkot region high strength design)
        val pillarWidth = 14f
        val pillarLength = height * 0.45f
        val pivotX = width / 2f
        val pivotY = groundY - pillarLength

        drawLine(
            color = Color(0xFF78909C),
            start = Offset(pivotX, groundY),
            end = Offset(pivotX, pivotY),
            strokeWidth = pillarWidth,
            cap = StrokeCap.Round
        )

        // Base bolt elements for realism
        drawRect(
            color = Color(0xFF455A64),
            topLeft = Offset(pivotX - 16f, groundY - 6f),
            size = androidx.compose.ui.geometry.Size(32f, 10f)
        )

        // Pivot bearing center circle
        drawCircle(
            color = Color(0xFF37474F),
            radius = 18f,
            center = Offset(pivotX, pivotY)
        )

        // Pivot angle calculation and transformation
        withTransform({
            // pivotX, pivotY is center of rotated engine
            rotate(degrees = animatedAngle, pivot = Offset(pivotX, pivotY))
        }) {
            // Draw Panel crossbar truss
            val panelLength = width * 0.45f
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(pivotX - panelLength / 2, pivotY),
                end = Offset(pivotX + panelLength / 2, pivotY),
                strokeWidth = 16f,
                cap = StrokeCap.Square
            )

            // Draw Silicon Photovoltaic cells layers (blue stripes)
            val subCellsCount = 4
            val cellSpace = panelLength / subCellsCount
            for (i in 0 until subCellsCount) {
                val startX = (pivotX - panelLength / 2) + (i * cellSpace) + 4f
                val sizeCell = cellSpace - 8f
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(startX, pivotY - 20f),
                    size = androidx.compose.ui.geometry.Size(sizeCell, 20f)
                )

                // Glass shining grid lines
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(startX + 6f, pivotY - 18f),
                    end = Offset(startX + sizeCell - 6f, pivotY - 2f),
                    strokeWidth = 2f
                )
            }
        }

        // Draw dynamic shining Sun positioned relative to current tracker status
        if (mode == "Following sun" || mode == "Holding") {
            // Estimate Solar sky coordinates based on pivot degree
            // angle goes from -50 (East) to +50 (West)
            val rads = Math.toRadians((animatedAngle - 90).toDouble())
            val sunDist = height * 0.45f
            val sunX = pivotX + (sunDist * cos(rads)).toFloat()
            val sunY = pivotY + (sunDist * sin(rads)).toFloat()

            // Outer rays glow
            drawCircle(
                color = Color(0xFFFBC02D).copy(alpha = 0.2f),
                radius = 34f,
                center = Offset(sunX, sunY)
            )

            // Inner solar core
            drawCircle(
                color = Color(0xFFFFF176),
                radius = 18f,
                center = Offset(sunX, sunY)
            )

            // Dotted ray pointing from Sun to panel center pivot
            drawLine(
                color = Color(0xFFFBC02D).copy(alpha = 0.4f),
                start = Offset(sunX, sunY),
                end = Offset(pivotX, pivotY),
                strokeWidth = 2.5f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
    }
}

@Composable
fun WeatherMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = NaturalTextSecondary, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NaturalTextPrimary)
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(48.dp)
            .width(1.dp)
            .background(NaturalBorder)
    )
}

@Composable
fun StatusBadgeMini(status: String) {
    val (color, bgColor) = getStatusColorPalette(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(status, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusBadgeLarge(status: String) {
    val (color, bgColor) = getStatusColorPalette(status)
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Text(
                status,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

fun getStatusColorPalette(status: String): Pair<Color, Color> {
    return when (status) {
        "Active" -> Pair(NaturalGreenAccent, NaturalGreenPill)
        "Storm mode" -> Pair(NaturalAlertAccent, NaturalAlertBg)
        "Fault" -> Pair(SolarOrangeDark, Color(0xFFFFF3E0))
        else -> Pair(NaturalMuted, NaturalBg)
    }
}

fun getModeColor(mode: String): Color {
    return when {
        mode.contains("Follow") -> NaturalGreenAccent
        mode.contains("Hold") -> SolarBlue
        mode.contains("Safe") -> SolarOrangeDark
        mode.contains("Stow") -> NaturalAlertAccent
        else -> NaturalMuted
    }
}
