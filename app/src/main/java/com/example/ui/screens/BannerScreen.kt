package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NaturalGreenAccent
import com.example.ui.theme.NaturalBg

@Composable
fun BannerScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NaturalBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SolarTrack",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = NaturalGreenAccent
            )
            Text(
                text = "Advanced Solar Asset Management",
                fontSize = 20.sp,
                color = Color(0xFF455A64),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
