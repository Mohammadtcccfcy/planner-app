package com.example.myapplication

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val TODAY = "today"
    const val WEEKLY = "weekly"
    const val CALENDAR = "calendar"
    const val GOALS = "goals"
    const val SETTINGS = "settings"
    const val CALLS = "calls"
    const val PROFILE = "profile"
}

data class DrawerItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

object DrawerItems {
    val all: List<DrawerItem> = listOf(
        DrawerItem(Routes.TODAY, R.string.nav_today, Icons.Filled.Today),
        DrawerItem(Routes.WEEKLY, R.string.nav_weekly, Icons.Filled.ViewWeek),
        DrawerItem(Routes.CALENDAR, R.string.nav_calendar, Icons.Filled.CalendarMonth),
        DrawerItem(Routes.GOALS, R.string.nav_goals, Icons.Filled.Flag),
        DrawerItem(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
        DrawerItem(Routes.CALLS, R.string.calls_title, Icons.Filled.Call),
        DrawerItem(Routes.PROFILE, R.string.profile_title, Icons.Filled.Person),
    )
}
