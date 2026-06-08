package com.gonec009.meshizandaka.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.ui.home.HomeRoute
import com.gonec009.meshizandaka.ui.quickrecord.QuickRecordRoute
import com.gonec009.meshizandaka.ui.recordedit.RecordEditRoute
import com.gonec009.meshizandaka.ui.settings.SettingsRoute
import com.gonec009.meshizandaka.ui.template.TemplateManagementRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshiZandakaAppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = when {
        currentRoute == AppDestination.Home.route -> AppDestination.Home
        currentRoute == AppDestination.QuickRecord.route -> AppDestination.QuickRecord
        currentRoute == AppDestination.Templates.route -> AppDestination.Templates
        currentRoute == AppDestination.Settings.route -> AppDestination.Settings
        currentRoute?.startsWith("record_edit/") == true -> AppDestination.RecordEdit
        else -> AppDestination.Home
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentDestination.titleResId)) },
                navigationIcon = {
                    if (currentDestination != AppDestination.Home) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Text(text = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
        ) {
            composable(AppDestination.Home.route) {
                HomeRoute(
                    container = container,
                    innerPadding = innerPadding,
                    snackbarHostState = snackbarHostState,
                    onQuickRecordClick = { navController.navigate(AppDestination.QuickRecord.route) },
                    onTemplateManagementClick = { navController.navigate(AppDestination.Templates.route) },
                    onSettingsClick = { navController.navigate(AppDestination.Settings.route) },
                    onRecordClick = { navController.navigate(AppDestination.RecordEdit.route(it)) },
                )
            }
            composable(AppDestination.QuickRecord.route) {
                QuickRecordRoute(
                    container = container,
                    innerPadding = innerPadding,
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(AppDestination.Templates.route) {
                TemplateManagementRoute(
                    container = container,
                    innerPadding = innerPadding,
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute(
                    container = container,
                    innerPadding = innerPadding,
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(
                route = AppDestination.RecordEdit.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
            ) { backStack ->
                RecordEditRoute(
                    container = container,
                    innerPadding = innerPadding,
                    recordId = backStack.arguments?.getLong("recordId") ?: 0L,
                    snackbarHostState = snackbarHostState,
                    onDeleteComplete = { navController.popBackStack() },
                )
            }
        }
    }
}
