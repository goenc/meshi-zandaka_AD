package com.gonec009.meshizandaka.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.HomeDashboardData
import com.gonec009.meshizandaka.ui.home.HomeRoute
import com.gonec009.meshizandaka.ui.quickrecord.QuickRecordRoute
import com.gonec009.meshizandaka.ui.recordedit.RecordEditRoute
import com.gonec009.meshizandaka.ui.settings.SettingsRoute
import com.gonec009.meshizandaka.ui.template.TemplateManagementRoute
import com.gonec009.meshizandaka.util.formatOneDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshiZandakaAppRoot(
    container: AppContainer,
    onDriveConnect: () -> Unit,
    onHomeReady: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val dashboard by container.observeDashboardUseCase().collectAsState(initial = HomeDashboardData())
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
            if (currentDestination != AppDestination.QuickRecord) {
                TopAppBar(
                    title = {
                        if (currentDestination == AppDestination.Home) {
                            val titleFontSize = MaterialTheme.typography.titleLarge.fontSize.value
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    Text(stringResource(currentDestination.titleResId))
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    fontSize = (titleFontSize + 2f).sp,
                                                ),
                                            ) {
                                                append(dashboard.summary.todayBalanceCalories.toString())
                                            }
                                            append("/")
                                            withStyle(
                                                SpanStyle(
                                                    fontSize = (titleFontSize - 2f).sp,
                                                ),
                                            ) {
                                                append(
                                                    (dashboard.summary.todayConsumedCalories + dashboard.summary.todayBalanceCalories)
                                                        .toString(),
                                                )
                                                append(" kcal")
                                            }
                                        },
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        R.string.today_pfc_grams,
                                        formatOneDecimal(dashboard.summary.todayProteinGrams),
                                        formatOneDecimal(dashboard.summary.todayFatGrams),
                                        formatOneDecimal(dashboard.summary.todayCarbGrams),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else if (currentDestination != AppDestination.Templates) {
                            Text(stringResource(currentDestination.titleResId))
                        }
                    },
                    navigationIcon = {
                        if (currentDestination != AppDestination.Home) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Text(text = stringResource(R.string.back))
                            }
                        }
                    },
                    actions = {
                        if (currentDestination == AppDestination.Home) {
                            IconButton(onClick = { navController.navigate(AppDestination.Settings.route) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = stringResource(R.string.go_to_settings),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    },
                )
            }
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
                    onReady = onHomeReady,
                    onQuickRecordClick = { navController.navigate(AppDestination.QuickRecord.route) },
                    onTemplateManagementClick = { navController.navigate(AppDestination.Templates.route) },
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
                    onDriveConnect = onDriveConnect,
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute(
                    container = container,
                    innerPadding = innerPadding,
                    snackbarHostState = snackbarHostState,
                    onDriveConnect = onDriveConnect,
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
