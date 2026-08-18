package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun AppDrawer(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Image(
                painter = rememberVectorPainter(Icons.Filled.Person),
                contentDescription = stringResource(R.string.profile_title),
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.drawer_welcome), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))

            DrawerItem(
                label = stringResource(R.string.nav_today),
                selected = selectedRoute == Routes.TODAY,
                onClick = { onRouteSelected(Routes.TODAY) },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
            )

            DrawerItem(
                label = stringResource(R.string.nav_weekly),
                selected = selectedRoute == Routes.WEEKLY,
                onClick = { onRouteSelected(Routes.WEEKLY) },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
            )

            DrawerItem(
                label = stringResource(R.string.nav_calendar),
                selected = selectedRoute == Routes.CALENDAR,
                onClick = { onRouteSelected(Routes.CALENDAR) },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
            )

            DrawerItem(
                label = stringResource(R.string.calls_title),
                selected = selectedRoute == Routes.CALLS,
                onClick = { onRouteSelected(Routes.CALLS) },
                icon = { Icon(Icons.Filled.Call, contentDescription = null) }
            )

            DrawerItem(
                label = stringResource(R.string.nav_goals),
                selected = selectedRoute == Routes.GOALS,
                onClick = { onRouteSelected(Routes.GOALS) },
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
            )

            DrawerItem(
                label = stringResource(R.string.profile_title),
                selected = selectedRoute == Routes.PROFILE,
                onClick = { onRouteSelected(Routes.PROFILE) },
                icon = { Icon(Icons.Filled.Person, contentDescription = null) }
            )

            DrawerItem(
                label = stringResource(R.string.nav_settings),
                selected = selectedRoute == Routes.SETTINGS,
                onClick = { onRouteSelected(Routes.SETTINGS) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = icon
    )
}

@Preview(showBackground = true)
@Composable
private fun DrawerPreview() {
    MyApplicationTheme {
        AppDrawer(selectedRoute = Routes.TODAY, onRouteSelected = {})
    }
}
