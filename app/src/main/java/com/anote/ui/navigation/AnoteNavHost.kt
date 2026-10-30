package com.anote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anote.R
import com.anote.ui.calendar.CalendarScreen
import com.anote.ui.detail.DetailScreen
import com.anote.ui.editor.EditorScreen
import com.anote.ui.list.DiaryListScreen
import com.anote.ui.settings.SettingsScreen

private data class TopDestination(
    val route: String,
    val label: Int,
    val icon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(Routes.LIST, R.string.nav_diary, Icons.AutoMirrored.Filled.MenuBook),
    TopDestination(Routes.CALENDAR, R.string.nav_calendar, Icons.Filled.CalendarMonth),
    TopDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun AnoteNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route == Routes.LIST || route == Routes.CALENDAR || route == Routes.SETTINGS
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    topDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = null)
                            },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LIST) {
                DiaryListScreen(
                    onOpen = { id -> navController.navigate(Routes.detail(id)) },
                    onCreate = { navController.navigate(Routes.editor()) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onOpen = { id -> navController.navigate(Routes.detail(id)) },
                    onCreate = { dateEpoch -> navController.navigate(Routes.editor(dateEpoch = dateEpoch)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("entryId") ?: return@composable
                DetailScreen(
                    entryId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { entryId -> navController.navigate(Routes.editor(entryId = entryId)) },
                )
            }
            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument("entryId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("dateEpoch") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("entryId") ?: -1L
                val dateEpoch = entry.arguments?.getLong("dateEpoch") ?: -1L
                EditorScreen(
                    entryId = id.takeIf { it > 0 },
                    dateEpoch = dateEpoch,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
