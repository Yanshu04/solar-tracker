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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Alert
import com.example.ui.SolarViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: SolarViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val formatTime = rememberDateFormatter()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Operations Alerts",
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NaturalBg
                ),
                actions = {
                    if (alerts.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearAlerts() },
                            modifier = Modifier.testTag("clear_alerts_button")
                        ) {
                            Text("Clear All", color = NaturalAlertAccent, fontWeight = FontWeight.Bold)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header Label
                item {
                    Text(
                        text = "ACTIVE ALERTS FEED (${alerts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // If no alerts
                if (alerts.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, NaturalBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("no_alerts_placeholder")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Safe checklist icon",
                                    tint = NaturalGreenAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "ALL SYSTEMS STANDARD",
                                    fontWeight = FontWeight.Bold,
                                    color = NaturalTextPrimary,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "No critical failures, offline delays, or high winds reported in Rajkot zones.",
                                    fontSize = 12.sp,
                                    color = NaturalTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(alerts, key = { it.id }) { alert ->
                        AlertRow(alert, formatTime)
                    }
                }

                // Field simulation toolbox
                item {
                    Text(
                        text = "FIELD TELEMETRY SIMULATOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NaturalTextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, NaturalBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Simulate active state changes at local tracker sites to test stowing protocols and alarms.",
                                fontSize = 12.sp,
                                color = NaturalTextSecondary,
                                lineHeight = 16.sp
                            )

                            // Storm Simulation Btn
                            Button(
                                onClick = {
                                    viewModel.postCustomAlert(
                                        siteId = "4",
                                        siteName = "Kalawad Road Hub",
                                        alertType = "Storm warning",
                                        message = "TORNADO WINDS: High-velocity cyclonic gust of 58 km/h recorded at Kalawad Road Hub array. Structure ordered to AUTO-STOW.",
                                        severity = "High"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NaturalAlertAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("simulate_storm_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Thunderstorm, "Storm icon", tint = Color.White)
                                    Text("Simulate Gale Storm (58 km/h)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Motor stall simulation
                            Button(
                                onClick = {
                                    viewModel.postCustomAlert(
                                        siteId = "2",
                                        siteName = "Metoda GIDC Plant",
                                        alertType = "Motor fault",
                                        message = "DRIVE OVERCURRENT: Dynamic thermal fuses cut off current to axis gear 14 at Metoda GIDC. Sector stowing halt.",
                                        severity = "Medium"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SolarOrangeDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("simulate_fault_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ReportProblem, "Warning icon", tint = Color.White)
                                    Text("Simulate Motor Axis Stall", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Telemetry drop simulation
                            Button(
                                onClick = {
                                    viewModel.postCustomAlert(
                                        siteId = "8",
                                        siteName = "University Campus Array",
                                        alertType = "Site offline",
                                        message = "COMMUNICATION BREAKDOWN: High thermal noise jammed transceivers at University Campus Array. Uplink offline.",
                                        severity = "Low"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NaturalMuted),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("simulate_offline_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.WifiOff, "Offline icon", tint = Color.White)
                                    Text("Simulate Telemetry Gateway drop", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertRow(alert: Alert, formatTime: SimpleDateFormat) {
    val (severityColor, severityBg) = getSeverityColorPalette(alert.severity)
    val isHighSeverity = alert.severity == "High"

    val cardColor = if (isHighSeverity) NaturalAlertBg else Color.White
    val borderColor = if (isHighSeverity) NaturalAlertBorder else NaturalBorder
    val contentColor = if (isHighSeverity) NaturalAlertDarkText else NaturalTextPrimary
    val messageColor = if (isHighSeverity) NaturalAlertSubText else NaturalTextPrimary

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_item_${alert.alertType.replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHighSeverity) {
                        // High warning badge circle visual layout from design HTML
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NaturalAlertAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Thunderstorm,
                                contentDescription = alert.alertType,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            when (alert.alertType) {
                                "Storm warning" -> Icons.Default.Thunderstorm
                                "Motor fault" -> Icons.Default.PrecisionManufacturing
                                else -> Icons.Default.SyncProblem
                            },
                            contentDescription = alert.alertType,
                            tint = severityColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = alert.alertType.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = contentColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isHighSeverity) NaturalAlertAccent else severityBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = alert.severity.uppercase(),
                        color = if (isHighSeverity) Color.White else severityColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alert.message,
                fontSize = 13.sp,
                color = messageColor,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = NaturalBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Site: ${alert.siteName}",
                    fontSize = 11.sp,
                    color = if (isHighSeverity) NaturalAlertSubText else NaturalTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTime.format(Date(alert.timestamp)),
                    fontSize = 11.sp,
                    color = if (isHighSeverity) NaturalAlertSubText.copy(alpha = 0.8f) else NaturalTextSecondary
                )
            }
        }
    }
}

fun getSeverityColorPalette(severity: String): Pair<Color, Color> {
    return when (severity) {
        "High" -> Pair(NaturalAlertAccent, NaturalAlertBg)
        "Medium" -> Pair(SolarOrangeDark, NaturalGreenPill)
        else -> Pair(NaturalGreenAccent, NaturalGreenPill)
    }
}

@Composable
fun rememberDateFormatter(): SimpleDateFormat {
    return remember { SimpleDateFormat("hh:mm a • dd MMM", Locale.getDefault()) }
}
