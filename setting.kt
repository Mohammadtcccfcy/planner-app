package com.example.myapplication

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.ZoneId
import java.util.Locale

private const val SETTINGS_PAGE_MAIN = 0
private const val SETTINGS_PAGE_LANG_TIME = 1
private const val SETTINGS_PAGE_EVENT_PREFS = 2
private const val SETTINGS_PAGE_CALENDAR_PREFS = 3

/**
 * - گزینه "System default" حذف شد.
 * - زبان پیش‌فرض انگلیسی است (اگر مقدار DB null باشد، UI آن را English فرض می‌کند).
 */
private const val DEFAULT_LANGUAGE_TAG = "en"

private val LANGUAGES = listOf(
    "English" to "en",
    "فارسی" to "fa",
    "العربية" to "ar",
    "Français" to "fr",
    "Deutsch" to "de",
    "Türkçe" to "tr",
    "Ελληνικά" to "el",
    "中文 (简体)" to "zh-CN",
    "Español" to "es",
    "हिन्दी" to "hi"
)

private val CALENDAR_TYPES = listOf(
    CalendarType.JALALI,
    CalendarType.GREGORIAN,
    CalendarType.HIJRI
)

private val WEEKEND_MODES = listOf(
    WeekendMode.THU_FRI,
    WeekendMode.SAT_SUN
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val settings by PlannerData.observeSettings().collectAsState(initial = SettingsEntity())

    @Suppress("UNUSED_VARIABLE")
    val _config = LocalConfiguration.current

    var page by remember { mutableIntStateOf(SETTINGS_PAGE_MAIN) }

    BackHandler(enabled = page != SETTINGS_PAGE_MAIN) {
        page = SETTINGS_PAGE_MAIN
    }

    when (page) {
        SETTINGS_PAGE_MAIN -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ===== Event preferences =====
                item {
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { page = SETTINGS_PAGE_EVENT_PREFS }
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stringResource(R.string.settings_event_prefs_title), fontWeight = FontWeight.SemiBold)

                            val mins = settings.defaultReminderMinutes
                            val vib = if (settings.vibrationOnReminder) {
                                stringResource(R.string.common_on)
                            } else {
                                stringResource(R.string.common_off)
                            }
                            val snd = if (settings.soundOnReminder) {
                                stringResource(R.string.common_on)
                            } else {
                                stringResource(R.string.common_off)
                            }

                            Text(
                                text = stringResource(R.string.settings_event_prefs_summary, mins, vib, snd),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ===== Language & time =====
                item {
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { page = SETTINGS_PAGE_LANG_TIME }
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stringResource(R.string.settings_language_time_title), fontWeight = FontWeight.SemiBold)

                            val langTag = settings.languageTag ?: DEFAULT_LANGUAGE_TAG
                            val langLabel = LANGUAGES.firstOrNull { it.second == langTag }?.first ?: "English"

                            val tzLabel = settings.timeZoneId ?: "${ZoneId.systemDefault().id}"

                            Text(
                                text = stringResource(R.string.settings_language_time_summary, langLabel, tzLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ===== Calendar preferences =====
                item {
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { page = SETTINGS_PAGE_CALENDAR_PREFS }
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stringResource(R.string.settings_calendar_prefs_title), fontWeight = FontWeight.SemiBold)

                            val calLabel = calendarTypeLabel(settings.defaultCalendarType)
                            val weekendLabel = weekendModeLabel(settings.weekendMode)

                            Text(
                                text = stringResource(R.string.settings_calendar_prefs_summary, calLabel, weekendLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ===== Reset factory =====
                item {
                    var confirmReset by rememberSaveable { mutableStateOf(false) }

                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(stringResource(R.string.settings_reset_factory_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = stringResource(R.string.settings_reset_factory_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { confirmReset = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.settings_reset))
                            }
                        }
                    }

                    if (confirmReset) {
                        AlertDialog(
                            onDismissRequest = { confirmReset = false },
                            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
                            text = { Text(stringResource(R.string.settings_reset_confirm_desc)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        scope.launch { PlannerData.resetFactory() }
                                        confirmReset = false
                                    }
                                ) { Text(stringResource(R.string.settings_reset)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.common_cancel)) }
                            }
                        )
                    }
                }
            }
        }

        SETTINGS_PAGE_EVENT_PREFS -> {
            EventPreferencesScreen(
                settings = settings,
                onBack = { page = SETTINGS_PAGE_MAIN },
                onSetDefaultReminderMinutes = { m ->
                    scope.launch { PlannerData.upsertSettings { it.copy(defaultReminderMinutes = m) } }
                },
                onSetVibration = { v ->
                    scope.launch { PlannerData.upsertSettings { it.copy(vibrationOnReminder = v) } }
                },
                onSetSound = { v ->
                    scope.launch { PlannerData.upsertSettings { it.copy(soundOnReminder = v) } }
                }
            )
        }

        SETTINGS_PAGE_LANG_TIME -> {
            LanguageTimeScreen(
                settings = settings,
                onBack = { page = SETTINGS_PAGE_MAIN },
                onSetLanguage = { tag ->
                    val safeTag = tag.ifBlank { DEFAULT_LANGUAGE_TAG }
                    scope.launch { PlannerData.upsertSettings { it.copy(languageTag = safeTag) } }
                },
                onSetTimeZone = { zoneId ->
                    scope.launch { PlannerData.upsertSettings { it.copy(timeZoneId = zoneId) } }
                }
            )
        }

        SETTINGS_PAGE_CALENDAR_PREFS -> {
            CalendarPreferencesScreen(
                settings = settings,
                onBack = { page = SETTINGS_PAGE_MAIN },
                onSetDefaultCalendarType = { picked ->
                    scope.launch { PlannerData.upsertSettings { it.copy(defaultCalendarType = picked) } }
                },
                onSetWeekendMode = { picked ->
                    scope.launch { PlannerData.upsertSettings { it.copy(weekendMode = picked) } }
                }
            )
        }
    }
}

// =====================
// Calendar Preferences UI
// =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarPreferencesScreen(
    settings: SettingsEntity,
    onBack: () -> Unit,
    onSetDefaultCalendarType: (CalendarType) -> Unit,
    onSetWeekendMode: (WeekendMode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_calendar_prefs_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) } }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Default calendar
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_default_calendar_label), fontWeight = FontWeight.SemiBold)
                        DefaultCalendarDropdown(
                            selected = settings.defaultCalendarType,
                            onPick = onSetDefaultCalendarType
                        )
                    }
                }
            }

            // Weekend mode
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_weekend_mode_label), fontWeight = FontWeight.SemiBold)
                        WeekendModeDropdown(
                            selected = settings.weekendMode,
                            onPick = onSetWeekendMode
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultCalendarDropdown(
    selected: CalendarType,
    onPick: (CalendarType) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = calendarTypeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_default_calendar_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CALENDAR_TYPES.forEach { t ->
                DropdownMenuItem(
                    text = { Text(calendarTypeLabel(t)) },
                    onClick = { onPick(t); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekendModeDropdown(
    selected: WeekendMode,
    onPick: (WeekendMode) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = weekendModeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_weekend_mode_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            WEEKEND_MODES.forEach { m ->
                DropdownMenuItem(
                    text = { Text(weekendModeLabel(m)) },
                    onClick = { onPick(m); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun calendarTypeLabel(t: CalendarType): String = when (t) {
    CalendarType.JALALI -> stringResource(R.string.calendar_type_jalali)
    CalendarType.GREGORIAN -> stringResource(R.string.calendar_type_gregorian)
    CalendarType.HIJRI -> stringResource(R.string.calendar_type_hijri)
}

@Composable
private fun weekendModeLabel(m: WeekendMode): String = when (m) {
    WeekendMode.THU_FRI -> stringResource(R.string.weekend_mode_thu_fri)
    WeekendMode.SAT_SUN -> stringResource(R.string.weekend_mode_sat_sun)
}

// =====================
// Existing screens (unchanged)
// =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventPreferencesScreen(
    settings: SettingsEntity,
    onBack: () -> Unit,
    onSetDefaultReminderMinutes: (Int) -> Unit,
    onSetVibration: (Boolean) -> Unit,
    onSetSound: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_event_prefs_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) } }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(stringResource(R.string.settings_default_reminder_minutes), fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30).forEach { m ->
                                FilterChip(
                                    selected = settings.defaultReminderMinutes == m,
                                    onClick = { onSetDefaultReminderMinutes(m) },
                                    label = { Text("${m}m") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_vibration_on_reminder), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = stringResource(R.string.settings_used_by_reminders_when_they_trigger),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.vibrationOnReminder,
                            onCheckedChange = onSetVibration
                        )
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_sound_on_reminder), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = stringResource(R.string.settings_used_by_reminders_when_they_trigger),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.soundOnReminder,
                            onCheckedChange = onSetSound
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageTimeScreen(
    settings: SettingsEntity,
    onBack: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetTimeZone: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load tzdb catalog once (IO)
    val catalogState = produceState<Tzdb.Catalog?>(initialValue = null, key1 = Unit) {
        value = Tzdb.load(context)
    }

    // Search
    var query by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = remember(query) { query.trim().lowercase(Locale.ROOT) }

    // Expanded state per country code
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    val catalog = catalogState.value

    // Expand during search to show results
    LaunchedEffect(normalizedQuery, catalog) {
        if (normalizedQuery.isNotEmpty() && catalog != null) {
            catalog.groups.forEach { g ->
                expanded[g.countryCode] = true
            }
        }
    }

    // Build a single flat list of rows for LazyColumn
    val visibleRows by remember(catalog, normalizedQuery, expanded, settings.timeZoneId) {
        derivedStateOf {
            val groups = catalog?.groups.orEmpty()

            val filteredGroups = if (normalizedQuery.isEmpty()) {
                groups
            } else {
                groups.mapNotNull { g ->
                    val countryMatch = g.countryDisplay.lowercase(Locale.ROOT).contains(normalizedQuery)
                    if (countryMatch) return@mapNotNull g

                    val zones = g.zones.filter { z ->
                        val cityToken = z.substringAfterLast('/').replace('_', ' ')
                        z.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                                cityToken.lowercase(Locale.ROOT).contains(normalizedQuery)
                    }

                    if (zones.isEmpty()) null else g.copy(zones = zones)
                }
            }

            val rows = ArrayList<TzRow>(filteredGroups.size * 3)

            filteredGroups.forEach { g ->
                val isExpanded = expanded[g.countryCode] ?: false
                rows += TzRow.CountryHeader(
                    countryCode = g.countryCode,
                    countryDisplay = g.countryDisplay,
                    zoneCount = g.zones.size,
                    expanded = isExpanded
                )

                if (isExpanded) {
                    g.zones.forEach { zoneId ->
                        rows += TzRow.ZoneItem(
                            zoneId = zoneId,
                            selected = (settings.timeZoneId == zoneId)
                        )
                    }
                }
            }

            rows
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language_time_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) } }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language card
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_language_label), fontWeight = FontWeight.SemiBold)
                        LanguageDropdown(
                            selectedTag = settings.languageTag ?: DEFAULT_LANGUAGE_TAG,
                            onPick = onSetLanguage
                        )
                        Text(
                            text = stringResource(R.string.settings_language_saved_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Timezone header card
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_time_zone_label), fontWeight = FontWeight.SemiBold)

                        val sys = ZoneId.systemDefault().id
                        val selectedLabel = settings.timeZoneId ?: sys

                        Text(
                            text = "${stringResource(R.string.settings_selected)} $selectedLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            label = { Text("Search") },
                            placeholder = { Text("Country, city, or ZoneId") }
                        )

                        if (catalog == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Expandable list (headers + zones)
            itemsIndexed(
                items = visibleRows,
                key = { _, row ->
                    when (row) {
                        is TzRow.CountryHeader -> "H_${row.countryCode}"
                        is TzRow.ZoneItem -> "Z_${row.zoneId}"
                    }
                }
            ) { _, row ->
                when (row) {
                    is TzRow.CountryHeader -> {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val now = expanded[row.countryCode] ?: false
                                        expanded[row.countryCode] = !now
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.countryDisplay, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${row.zoneCount} time zones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(if (row.expanded) "▲" else "▼")
                            }
                        }
                    }

                    is TzRow.ZoneItem -> {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ZoneRow(
                                zoneId = row.zoneId,
                                selected = row.selected,
                                onSelect = {
                                    scope.launch { onSetTimeZone(row.zoneId) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface TzRow {
    data class CountryHeader(
        val countryCode: String,
        val countryDisplay: String,
        val zoneCount: Int,
        val expanded: Boolean
    ) : TzRow

    data class ZoneItem(
        val zoneId: String,
        val selected: Boolean
    ) : TzRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    selectedTag: String,
    onPick: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val selectedLabel = LANGUAGES.firstOrNull { it.second == selectedTag }?.first
        ?: "English"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_app_language_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LANGUAGES.forEach { (label, tag) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onPick(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------
// Time Zone Catalog (Countries -> Zones) | Offline
// ------------------------------------------------------------

private object Tzdb {
    private const val ASSET_PATH = "tzdb/zone1970.tab"
    private const val PSEUDO_OTHER_CC = "ZZ" // pseudo-country for zones not present in zone1970.tab

    data class CountryGroup(
        val countryCode: String,
        val countryDisplay: String,
        val zones: List<String>
    )

    data class Catalog(
        val groups: List<CountryGroup>
    )

    suspend fun load(context: android.content.Context): Catalog = withContext(Dispatchers.IO) {
        val countryToZones = parseZone1970Tab(context)

        val allZones = ZoneId.getAvailableZoneIds()
        val knownZones = countryToZones.values.asSequence().flatten().toSet()
        val unknownZones = (allZones - knownZones).toList().sorted()

        val groups = mutableListOf<CountryGroup>()

        // Countries from tzdb
        countryToZones.keys.sorted().forEach { cc ->
            val display = CountryNameResolver.resolveDisplay(cc)
            val zones = countryToZones[cc].orEmpty().toList().sorted()
            groups += CountryGroup(cc, display, zones)
        }

        // Unknown zones inside the same list (as a pseudo-country)
        if (unknownZones.isNotEmpty()) {
            groups += CountryGroup(
                countryCode = PSEUDO_OTHER_CC,
                countryDisplay = "Other (Global)",
                zones = unknownZones
            )
        }

        Catalog(groups = groups)
    }

    private fun parseZone1970Tab(context: android.content.Context): Map<String, Set<String>> {
        val result = linkedMapOf<String, MutableSet<String>>()

        // If asset is missing -> empty map
        val input = runCatching { context.assets.open(ASSET_PATH) }.getOrNull() ?: return emptyMap()

        input.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith('#')) return@forEach

                    val cols = line.split('\t')
                    if (cols.size < 3) return@forEach

                    val ccPart = cols[0].trim()
                    val zoneId = cols[2].trim()
                    if (ccPart.isEmpty() || zoneId.isEmpty()) return@forEach

                    val countryCodes = ccPart.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    for (cc in countryCodes) {
                        val set = result.getOrPut(cc) { linkedSetOf() }
                        set.add(zoneId)
                    }
                }
            }
        }

        return result.mapValues { it.value.toSet() }
    }
}

private object CountryNameResolver {
    /**
     * ICU-based country display:
     * - English-speaking country => "Canada"
     * - Otherwise => "Germany (Deutschland)"
     */
    fun resolveDisplay(countryCode: String): String {
        val cc = countryCode.uppercase(Locale.ROOT)
        val english = Locale("", cc).getDisplayCountry(Locale.ENGLISH).trim().ifEmpty { cc }

        // ICU is only reliably available API 24+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return english

        val native = runCatching {
            val base = android.icu.util.ULocale("und_$cc")
            val likely = android.icu.util.ULocale.addLikelySubtags(base)
            val nativeLocale = Locale.forLanguageTag(likely.toLanguageTag())
            Locale("", cc).getDisplayCountry(nativeLocale).trim().takeIf { it.isNotEmpty() }
        }.getOrNull()

        val likelyLang = runCatching {
            val likely = android.icu.util.ULocale.addLikelySubtags(android.icu.util.ULocale("und_$cc"))
            likely.language
        }.getOrNull()

        val isEnglishSpeaking = (likelyLang == "en")

        return if (isEnglishSpeaking || native.isNullOrBlank() || native.equals(english, ignoreCase = true)) {
            english
        } else {
            "$english ($native)"
        }
    }
}

@Composable
private fun ZoneRow(
    zoneId: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val city = remember(zoneId) { zoneId.substringAfterLast('/').replace('_', ' ') }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(city, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(
                zoneId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = selected, onClick = onSelect)
    }
}
