package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinalBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SolarTrack",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF386B01)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Advanced Solar Asset Management",
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 200)
@Composable
fun FinalBannerPreview() {
    FinalBanner()
}
