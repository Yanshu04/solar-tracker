package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SolarViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SolarViewModel,
    modifier: Modifier = Modifier
) {
    var companyName by remember { mutableStateOf("SolarTrack Corp") }
    var useFahrenheit by remember { mutableStateOf(false) }
    var useMph by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NaturalBg)
            )
        },
        containerColor = NaturalBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("General Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NaturalTextSecondary)
            
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = NaturalBorder)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Temperature Unit", fontWeight = FontWeight.Bold)
                    Text("Show temperature in ${if (useFahrenheit) "Fahrenheit" else "Celsius"}", fontSize = 12.sp, color = NaturalTextSecondary)
                }
                Switch(checked = useFahrenheit, onCheckedChange = { useFahrenheit = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wind Speed Unit", fontWeight = FontWeight.Bold)
                    Text("Show wind speed in ${if (useMph) "mph" else "km/h"}", fontSize = 12.sp, color = NaturalTextSecondary)
                }
                Switch(checked = useMph, onCheckedChange = { useMph = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifications", fontWeight = FontWeight.Bold)
                    Text("Receive alerts for storms and faults", fontSize = 12.sp, color = NaturalTextSecondary)
                }
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
