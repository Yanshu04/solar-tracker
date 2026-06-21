package com.example.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.SolarViewModel
import com.example.ui.theme.*

@Composable
fun MainNavigationControl(
    viewModel: SolarViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NaturalGrayNav,
                tonalElevation = 0.dp
            ) {
                listOf(
                    NavigationTabItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
                    NavigationTabItem("prediction", "Tomorrow", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
                    NavigationTabItem("sites", "Sites", Icons.Filled.List, Icons.Outlined.List),
                    NavigationTabItem("sitedetail", "Detail", Icons.Filled.Settings, Icons.Outlined.Settings),
                    NavigationTabItem("alerts", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications)
                ).forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NaturalGreenDarkText,
                            unselectedIconColor = NaturalTextSecondary.copy(alpha = 0.6f),
                            selectedTextColor = NaturalGreenDarkText,
                            unselectedTextColor = NaturalTextSecondary.copy(alpha = 0.6f),
                            indicatorColor = NaturalGreenPill
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { 
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ) 
                        },
                        modifier = Modifier.testTag("nav_tab_${item.route}")
                    )
                }
            }
        },
        containerColor = NaturalBg,
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAlerts = { navController.navigate("alerts") }
                )
            }
            composable("prediction") {
                PredictionScreen(viewModel = viewModel)
            }
            composable("sites") {
                SitesListScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { navController.navigate("sitedetail") }
                )
            }
            composable("sitedetail") {
                SiteDetailScreen(viewModel = viewModel)
            }
            composable("alerts") {
                AlertsScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavigationTabItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
