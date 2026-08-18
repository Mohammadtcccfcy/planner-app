package com.example.myapplication

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.core.PermissionsManager
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PlannerData.init(applicationContext)

        // ✅ Runtime Permission Check (VERY IMPORTANT)
        if (!PermissionsManager.hasAll(this)) {
            PermissionsManager.request(this)
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {

                val weeklyState = rememberWeeklyScheduleState()

                val settings by PlannerData.observeSettings()
                    .map { it ?: SettingsEntity() }
                    .collectAsState(initial = SettingsEntity())

                LaunchedEffect(settings.firstDayOfWeek) {
                    weeklyState.startDay = settings.firstDayOfWeek
                }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                var selectedRoute by rememberSaveable { mutableStateOf(Routes.TODAY) }

                // Localized titles
                val titleToday = stringResource(R.string.nav_today)
                val titleWeekly = stringResource(R.string.nav_weekly)
                val titleCalendar = stringResource(R.string.nav_calendar)
                val titleGoals = stringResource(R.string.nav_goals)
                val titleSettings = stringResource(R.string.nav_settings)
                val titleCalls = stringResource(R.string.calls_title)
                val titleProfile = stringResource(R.string.profile_title)
                val appName = stringResource(R.string.app_name)
                val menuCd = stringResource(R.string.common_menu)

                val topTitle = when (selectedRoute) {
                    Routes.TODAY -> titleToday
                    Routes.WEEKLY -> titleWeekly
                    Routes.CALENDAR -> titleCalendar
                    Routes.GOALS -> titleGoals
                    Routes.SETTINGS -> titleSettings
                    Routes.CALLS -> titleCalls
                    Routes.PROFILE -> titleProfile
                    else -> appName
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            selectedRoute = selectedRoute,
                            onRouteSelected = { routeKey ->
                                selectedRoute = routeKey
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(topTitle) },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = menuCd
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                        ) {
                            when (selectedRoute) {
                                Routes.TODAY -> TodayScreen(state = weeklyState)
                                Routes.WEEKLY -> WeeklyScheduleScreen(state = weeklyState)
                                Routes.CALENDAR -> CalendarScreen()
                                Routes.GOALS -> GoalsScreen()
                                Routes.SETTINGS -> SettingsScreen()
                                Routes.CALLS -> CallsScreen()
                                Routes.PROFILE -> DefaultSection(titleProfile)
                                else -> DefaultSection(appName)
                            }
                        }
                    }
                }

                LaunchedEffect(settings.languageTag, settings.timeZoneId) {
                    // Reserved for future use
                }
            }
        }
    }

    // ✅ Permission Result Handling
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionsManager.REQUEST_CODE) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                // TODO: Show warning dialog if needed
            }
        }
    }
}

@Composable
fun DefaultSection(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}

@Composable
fun CallsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Call, contentDescription = null)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.calls_body))
    }
}
