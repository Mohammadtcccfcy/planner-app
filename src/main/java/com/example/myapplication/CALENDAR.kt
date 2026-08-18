@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * IMPORTANT:
 * ✅ CalendarType enum باید فقط یک بار در پروژه تعریف شود.
 * شما قبلاً آن را در PlannerDatabase.kt دارید.
 * بنابراین این فایل فقط از همان CalendarType استفاده می‌کند و اینجا دوباره تعریفش نمی‌کنیم.
 */

private fun safeZoneId(id: String?): ZoneId {
    if (id.isNullOrBlank()) return ZoneId.systemDefault()
    return runCatching { ZoneId.of(id) }.getOrElse { ZoneId.systemDefault() }
}

@Composable
private fun appLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val list = configuration.locales
        if (list.isEmpty) Locale.getDefault() else list[0]
    }
}

// ---------------- Month names from strings.xml ----------------

@Composable
private fun jalaliMonthName(jm: Int): String {
    val resId = when (jm) {
        1 -> R.string.jalali_month_1
        2 -> R.string.jalali_month_2
        3 -> R.string.jalali_month_3
        4 -> R.string.jalali_month_4
        5 -> R.string.jalali_month_5
        6 -> R.string.jalali_month_6
        7 -> R.string.jalali_month_7
        8 -> R.string.jalali_month_8
        9 -> R.string.jalali_month_9
        10 -> R.string.jalali_month_10
        11 -> R.string.jalali_month_11
        else -> R.string.jalali_month_12
    }
    return stringResource(resId)
}

@Composable
private fun gregorianMonthName(gm: Int): String {
    val resId = when (gm) {
        1 -> R.string.month_gregorian_01
        2 -> R.string.month_gregorian_02
        3 -> R.string.month_gregorian_03
        4 -> R.string.month_gregorian_04
        5 -> R.string.month_gregorian_05
        6 -> R.string.month_gregorian_06
        7 -> R.string.month_gregorian_07
        8 -> R.string.month_gregorian_08
        9 -> R.string.month_gregorian_09
        10 -> R.string.month_gregorian_10
        11 -> R.string.month_gregorian_11
        else -> R.string.month_gregorian_12
    }
    return stringResource(resId)
}

@Composable
private fun hijriMonthName(hm: Int): String {
    val resId = when (hm) {
        1 -> R.string.month_hijri_01
        2 -> R.string.month_hijri_02
        3 -> R.string.month_hijri_03
        4 -> R.string.month_hijri_04
        5 -> R.string.month_hijri_05
        6 -> R.string.month_hijri_06
        7 -> R.string.month_hijri_07
        8 -> R.string.month_hijri_08
        9 -> R.string.month_hijri_09
        10 -> R.string.month_hijri_10
        11 -> R.string.month_hijri_11
        else -> R.string.month_hijri_12
    }
    return stringResource(resId)
}

// ---------------- Header gradients ----------------

private fun headerGradientForJalaliMonth(jm: Int): Pair<Color, Color> {
    // Spring: 1-3, Summer: 4-6, Autumn: 7-9, Winter: 10-12
    return when (jm) {
        1, 2, 3 -> Color(0xFF2E7D32) to Color(0xFF66BB6A)
        4, 5, 6 -> Color(0xFFAFB42B) to Color(0xFFFFEB3B)
        7, 8, 9 -> Color(0xFFEF6C00) to Color(0xFFFFB74D)
        else -> Color(0xFF0B86C7) to Color(0xFF0AA0D8)
    }
}

private fun headerGradientForGregorianMonth(gm: Int): Pair<Color, Color> {
    // simple season mapping for Gregorian
    return when (gm) {
        3, 4, 5 -> Color(0xFF2E7D32) to Color(0xFF66BB6A)      // spring
        6, 7, 8 -> Color(0xFFAFB42B) to Color(0xFFFFEB3B)      // summer
        9, 10, 11 -> Color(0xFFEF6C00) to Color(0xFFFFB74D)    // autumn
        else -> Color(0xFF0B86C7) to Color(0xFF0AA0D8)          // winter
    }
}

private fun headerGradientForHijriMonth(hm: Int): Pair<Color, Color> {
    // Hijri months are lunar; use a calm rotating palette
    return when (hm) {
        1, 2, 3 -> Color(0xFF6A1B9A) to Color(0xFF8E24AA)
        4, 5, 6 -> Color(0xFF1565C0) to Color(0xFF1E88E5)
        7, 8, 9 -> Color(0xFF2E7D32) to Color(0xFF66BB6A)
        else -> Color(0xFFEF6C00) to Color(0xFFFFB74D)
    }
}

// ---------------- Month navigation helpers ----------------

private data class MonthState(val year: Int, val month: Int)

private fun incMonth(ym: MonthState): MonthState =
    if (ym.month == 12) MonthState(ym.year + 1, 1) else MonthState(ym.year, ym.month + 1)

private fun decMonth(ym: MonthState): MonthState =
    if (ym.month == 1) MonthState(ym.year - 1, 12) else MonthState(ym.year, ym.month - 1)

// ============================================================
//  MAIN SCREEN
// ============================================================

@Composable
fun CalendarScreen(today: LocalDate = LocalDate.now()) {
    // Force LTR for the whole calendar UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val scope = rememberCoroutineScope()
        val locale = appLocale()

        val settings by PlannerData.observeSettings().collectAsState(initial = SettingsEntity())
        val zone = remember(settings.timeZoneId) { safeZoneId(settings.timeZoneId) }

        // ✅ Default calendar type from planner settings
        // اگر SettingsEntity این فیلد را nullable نگه داشته: fallback می‌زنیم.
        val calendarType: CalendarType = settings.defaultCalendarType ?: CalendarType.JALALI

        var selected by remember { mutableStateOf(today) }
        var tab by remember { mutableStateOf(1) }

        val events by PlannerData.observeTodayEvents(selected, zone).collectAsState(initial = emptyList())

        // Month state shown in the colored header (depends on calendarType)
        var currentYM by remember { mutableStateOf(MonthState(0, 0)) }

        // When user changes defaultCalendarType OR selected day, rebuild the visible month
        LaunchedEffect(calendarType, selected) {
            currentYM = when (calendarType) {
                CalendarType.JALALI -> {
                    val j = JalaliCalendar.fromGregorian(selected)
                    MonthState(j.year, j.month)
                }

                CalendarType.GREGORIAN -> MonthState(selected.year, selected.monthValue)

                CalendarType.HIJRI -> {
                    val h = HijrahChronology.INSTANCE.date(selected)
                    MonthState(h.get(ChronoField.YEAR_OF_ERA), h.get(ChronoField.MONTH_OF_YEAR))
                }
            }
        }

        val sPrev = stringResource(R.string.common_prev)
        val sNext = stringResource(R.string.common_next)
        val sTabEvents = stringResource(R.string.calendar_tab_events)
        val sTabCalendar = stringResource(R.string.calendar_tab_calendar)

        val (c1, c2) = remember(calendarType, currentYM) {
            when (calendarType) {
                CalendarType.JALALI -> headerGradientForJalaliMonth(currentYM.month)
                CalendarType.GREGORIAN -> headerGradientForGregorianMonth(currentYM.month)
                CalendarType.HIJRI -> headerGradientForHijriMonth(currentYM.month)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(c1, c2)))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentYM = decMonth(currentYM) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = sPrev, tint = Color.White)
                        }

                        Text(
                            text = headerTitle(calendarType, currentYM),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        IconButton(onClick = { currentYM = incMonth(currentYM) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = sNext, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    WeekHeader(locale)
                    Spacer(Modifier.height(6.dp))

                    when (calendarType) {
                        CalendarType.JALALI -> {
                            val selectedJ = remember(selected) { JalaliCalendar.fromGregorian(selected) }
                            val todayJ = remember(today) { JalaliCalendar.fromGregorian(today) }

                            MonthGridJalali(
                                jy = currentYM.year,
                                jm = currentYM.month,
                                selectedJ = selectedJ,
                                todayJ = todayJ,
                                onSelect = { picked ->
                                    selected = JalaliCalendar.toGregorian(picked.year, picked.month, picked.day)
                                    currentYM = MonthState(picked.year, picked.month)
                                }
                            )
                        }

                        CalendarType.GREGORIAN -> {
                            MonthGridGregorian(
                                gy = currentYM.year,
                                gm = currentYM.month,
                                selected = selected,
                                today = today,
                                onSelect = { picked ->
                                    selected = picked
                                    currentYM = MonthState(picked.year, picked.monthValue)
                                }
                            )
                        }

                        CalendarType.HIJRI -> {
                            MonthGridHijri(
                                hy = currentYM.year,
                                hm = currentYM.month,
                                selected = selected,
                                today = today,
                                onSelect = { picked ->
                                    // picked is a Gregorian LocalDate (easy for planner)
                                    selected = picked
                                    val h = HijrahChronology.INSTANCE.date(picked)
                                    currentYM = MonthState(
                                        h.get(ChronoField.YEAR_OF_ERA),
                                        h.get(ChronoField.MONTH_OF_YEAR)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    TabRow(
                        selectedTabIndex = tab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }) {
                            Text(sTabEvents, color = Color.White, modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = tab == 1, onClick = { tab = 1 }) {
                            Text(sTabCalendar, color = Color.White, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            when (tab) {
                0 -> EventsListSection(
                    locale = locale,
                    selected = selected,
                    events = events,
                    onAdd = { title, time ->
                        scope.launch { PlannerData.addTodayEvent(selected, title, time, zone) }
                    },
                    onRename = { id, newTitle -> scope.launch { PlannerData.renameEvent(id, newTitle) } },
                    onDelete = { id -> scope.launch { PlannerData.deleteEvent(id) } },
                    onToggleDone = { id, checked -> scope.launch { PlannerData.setEventDone(id, checked) } }
                )

                1 -> CalendarTripleRow(
                    locale = locale,
                    gDate = selected
                )
            }
        }

        LaunchedEffect(settings.languageTag, settings.timeZoneId, settings.defaultCalendarType) { }
    }
}

@Composable
private fun headerTitle(type: CalendarType, ym: MonthState): String {
    return when (type) {
        CalendarType.JALALI -> "${jalaliMonthName(ym.month)} ${ym.year}"
        CalendarType.GREGORIAN -> "${gregorianMonthName(ym.month)} ${ym.year}"
        CalendarType.HIJRI -> "${hijriMonthName(ym.month)} ${ym.year}"
    }
}

// ============================================================
//  WEEK HEADER
// ============================================================

@Composable
private fun WeekHeader(locale: Locale) {
    val days = remember {
        listOf(
            java.time.DayOfWeek.SATURDAY,
            java.time.DayOfWeek.SUNDAY,
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { d ->
            Text(d.getDisplayName(java.time.format.TextStyle.SHORT, locale), color = Color.White.copy(alpha = 0.9f))
        }
    }
}

// ============================================================
//  DAY CELL
// ============================================================

@Composable
private fun DayCell(
    text: String,
    isSelected: Boolean,
    isToday: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isToday -> Color.Black
        isSelected -> Color.White.copy(alpha = 0.28f)
        dimmed -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.18f)
    }
    Column(
        Modifier
            .size(40.dp)
            .background(bg, shape = MaterialTheme.shapes.small)
            .padding(4.dp)
            .noRippleClickable(onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = Color.White)
    }
}

@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) { onClick() }

// ============================================================
//  MONTH GRIDS
// ============================================================

@Composable
private fun MonthGridJalali(
    jy: Int,
    jm: Int,
    selectedJ: JalaliCalendar,
    todayJ: JalaliCalendar,
    onSelect: (JalaliCalendar) -> Unit
) {
    val leading = JalaliCalendar.leadingOffsetForMonth(jy, jm)
    val daysInMonth = JalaliCalendar.monthLength(jy, jm)

    val rows = buildList {
        var row = mutableListOf<Int?>()
        repeat(leading) { row.add(null) }
        for (d in 1..daysInMonth) {
            row.add(d)
            if (row.size == 7) {
                add(row)
                row = mutableListOf()
            }
        }
        if (row.isNotEmpty()) {
            while (row.size < 7) row.add(null)
            add(row)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                r.forEach { d ->
                    val isSelected = d != null && selectedJ.year == jy && selectedJ.month == jm && selectedJ.day == d
                    val isToday = d != null && todayJ.year == jy && todayJ.month == jm && todayJ.day == d
                    DayCell(
                        text = d?.toString() ?: "",
                        isSelected = isSelected,
                        isToday = isToday,
                        dimmed = d == null,
                        onClick = { if (d != null) onSelect(JalaliCalendar(jy, jm, d)) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MonthGridGregorian(
    gy: Int,
    gm: Int,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val first = LocalDate.of(gy, gm, 1)
    val daysInMonth = first.lengthOfMonth()

    val leading = when (first.dayOfWeek) {
        java.time.DayOfWeek.SATURDAY -> 0
        java.time.DayOfWeek.SUNDAY -> 1
        java.time.DayOfWeek.MONDAY -> 2
        java.time.DayOfWeek.TUESDAY -> 3
        java.time.DayOfWeek.WEDNESDAY -> 4
        java.time.DayOfWeek.THURSDAY -> 5
        java.time.DayOfWeek.FRIDAY -> 6
    }

    val rows = buildList {
        var row = mutableListOf<Int?>()
        repeat(leading) { row.add(null) }
        for (d in 1..daysInMonth) {
            row.add(d)
            if (row.size == 7) {
                add(row)
                row = mutableListOf()
            }
        }
        if (row.isNotEmpty()) {
            while (row.size < 7) row.add(null)
            add(row)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                r.forEach { d ->
                    val isSelected = d != null && selected.year == gy && selected.monthValue == gm && selected.dayOfMonth == d
                    val isToday = d != null && today.year == gy && today.monthValue == gm && today.dayOfMonth == d
                    DayCell(
                        text = d?.toString() ?: "",
                        isSelected = isSelected,
                        isToday = isToday,
                        dimmed = d == null,
                        onClick = {
                            if (d != null) onSelect(LocalDate.of(gy, gm, d))
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MonthGridHijri(
    hy: Int,
    hm: Int,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    fun hijriDateToGregorianSafe(y: Int, m: Int, d: Int): LocalDate? {
        return runCatching {
            val h = HijrahChronology.INSTANCE.date(y, m, d)
            LocalDate.from(h)
        }.getOrNull()
    }

    val length = (30 downTo 1).firstOrNull { hijriDateToGregorianSafe(hy, hm, it) != null } ?: 29

    val gFirst = hijriDateToGregorianSafe(hy, hm, 1) ?: return
    val leading = when (gFirst.dayOfWeek) {
        java.time.DayOfWeek.SATURDAY -> 0
        java.time.DayOfWeek.SUNDAY -> 1
        java.time.DayOfWeek.MONDAY -> 2
        java.time.DayOfWeek.TUESDAY -> 3
        java.time.DayOfWeek.WEDNESDAY -> 4
        java.time.DayOfWeek.THURSDAY -> 5
        java.time.DayOfWeek.FRIDAY -> 6
    }

    val rows = buildList {
        var row = mutableListOf<Int?>()
        repeat(leading) { row.add(null) }
        for (d in 1..length) {
            row.add(d)
            if (row.size == 7) {
                add(row)
                row = mutableListOf()
            }
        }
        if (row.isNotEmpty()) {
            while (row.size < 7) row.add(null)
            add(row)
        }
    }

    val selectedH = remember(selected) { HijrahChronology.INSTANCE.date(selected) }
    val todayH = remember(today) { HijrahChronology.INSTANCE.date(today) }

    Column(Modifier.fillMaxWidth()) {
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                r.forEach { d ->
                    val isSelected = d != null &&
                            selectedH.get(ChronoField.YEAR_OF_ERA) == hy &&
                            selectedH.get(ChronoField.MONTH_OF_YEAR) == hm &&
                            selectedH.get(ChronoField.DAY_OF_MONTH) == d

                    val isToday = d != null &&
                            todayH.get(ChronoField.YEAR_OF_ERA) == hy &&
                            todayH.get(ChronoField.MONTH_OF_YEAR) == hm &&
                            todayH.get(ChronoField.DAY_OF_MONTH) == d

                    DayCell(
                        text = d?.toString() ?: "",
                        isSelected = isSelected,
                        isToday = isToday,
                        dimmed = d == null,
                        onClick = {
                            if (d != null) {
                                hijriDateToGregorianSafe(hy, hm, d)?.let(onSelect)
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ============================================================
//  TRIPLE ROW (Hijri + Gregorian + Jalali) with MONTH NAMES
// ============================================================

@Composable
private fun CalendarTripleRow(
    locale: Locale,
    gDate: LocalDate
) {
    val jj = JalaliCalendar.fromGregorian(gDate)

    val hijri = HijrahChronology.INSTANCE.date(gDate)
    val hYear = hijri.get(ChronoField.YEAR_OF_ERA)
    val hMonth = hijri.get(ChronoField.MONTH_OF_YEAR)
    val hDay = hijri.get(ChronoField.DAY_OF_MONTH)

    val gYear = gDate.year
    val gMonth = gDate.monthValue
    val gDay = gDate.dayOfMonth

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TripleItem(
                big = hDay.toString(),
                label = hijriMonthName(hMonth),
                sub = "$hYear/$hMonth/$hDay"
            )

            TripleItem(
                big = gDay.toString(),
                label = gregorianMonthName(gMonth),
                sub = "$gYear/$gMonth/$gDay"
            )

            TripleItem(
                big = jj.day.toString(),
                label = jalaliMonthName(jj.month),
                sub = "${jj.year}/${jj.month}/${jj.day}"
            )
        }
    }
}

@Composable
private fun TripleItem(big: String, label: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(sub, style = MaterialTheme.typography.bodySmall)
    }
}

// ============================================================
//  EVENTS SECTION (Planner-connected)
// ============================================================

@Composable
private fun EventsListSection(
    locale: Locale,
    selected: LocalDate,
    events: List<UiTodayEvent>,
    onAdd: (String, LocalTime?) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleDone: (String, Boolean) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<UiTodayEvent?>(null) }

    val sEventsTitle = stringResource(R.string.calendar_events_title)
    val sNoEvents = stringResource(R.string.calendar_no_events_for_day)
    val sAdd = stringResource(R.string.common_add)
    val sEdit = stringResource(R.string.common_edit)
    val sDelete = stringResource(R.string.common_delete)

    val idOrder = remember { mutableStateListOf<String>() }
    val eventMap = remember(events) { events.associateBy { it.id } }

    LaunchedEffect(events) {
        events.forEach { if (!idOrder.contains(it.id)) idOrder.add(it.id) }
        val toRemove = idOrder.filter { id -> eventMap[id] == null }
        idOrder.removeAll(toRemove.toSet())
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(sEventsTitle, style = MaterialTheme.typography.titleMedium)
        FilledTonalButton(onClick = { showAdd = true }) { Text(sAdd) }
    }

    if (events.isEmpty()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(sNoEvents, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = idOrder, key = { it }) { id ->
                val ev = eventMap[id] ?: return@items
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FA))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ev.title, style = MaterialTheme.typography.bodyLarge)
                            ev.time?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                        }
                        Checkbox(
                            checked = ev.done,
                            onCheckedChange = { checked -> onToggleDone(ev.id, checked) }
                        )
                        IconButton(onClick = { editTarget = ev }) {
                            Icon(Icons.Filled.Edit, contentDescription = sEdit)
                        }
                        IconButton(onClick = { onDelete(ev.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = sDelete)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        EventEditorDialog(
            dialogTitle = stringResource(R.string.dialog_new_event_title),
            initialTitle = "",
            initialTime = null,
            onCancel = { showAdd = false },
            onConfirm = { t, time ->
                onAdd(t, time)
                showAdd = false
            }
        )
    }

    editTarget?.let { ev ->
        EventEditorDialog(
            dialogTitle = stringResource(R.string.dialog_edit_event_title),
            initialTitle = ev.title,
            initialTime = ev.time,
            onCancel = { editTarget = null },
            onConfirm = { t, _ ->
                onRename(ev.id, t)
                editTarget = null
            }
        )
    }
}

@Composable
private fun EventEditorDialog(
    dialogTitle: String,
    initialTitle: String,
    initialTime: LocalTime?,
    onCancel: () -> Unit,
    onConfirm: (String, LocalTime?) -> Unit
) {
    val sTitle = stringResource(R.string.common_title)
    val sTime = stringResource(R.string.common_time_label)
    val sSave = stringResource(R.string.common_ok)
    val sCancel = stringResource(R.string.common_cancel)

    var text by remember { mutableStateOf(initialTitle) }
    val timeState = rememberTimePickerState(
        initialHour = initialTime?.hour ?: 9,
        initialMinute = initialTime?.minute ?: 0,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(sTitle) },
                    singleLine = true
                )
                Text(sTime)
                TimePicker(state = timeState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = LocalTime.of(timeState.hour, timeState.minute)
                    onConfirm(text.trim(), picked)
                }
            ) { Text(sSave) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(sCancel) } }
    )
}

// ============================================================
//  JalaliCalendar (unchanged from your version)
// ============================================================

private data class JalaliCalendar(val year: Int, val month: Int, val day: Int) {
    companion object {
        fun fromGregorian(date: LocalDate): JalaliCalendar = toJalali(date.year, date.monthValue, date.dayOfMonth)

        fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
            val jy1 = jy + 1595
            var days = -355668 + (365 * jy1) + ((jy1 / 33) * 8) + (((jy1 % 33) + 3) / 4) +
                    jd + if (jm < 7) ((jm - 1) * 31) else (((jm - 7) * 30) + 186)
            var gy = 400 * (days / 146097)
            days %= 146097
            if (days > 36524) {
                gy += 100 * (--days / 36524)
                days %= 36524
                if (days >= 365) days++
            }
            gy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                gy += ((days - 1) / 365)
                days = (days - 1) % 365
            }
            var gd = days + 1
            val salA = intArrayOf(
                0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
                31, 30, 31, 30, 31, 31, 30, 31, 30, 31
            )
            var gm = 0
            while (gm < 13 && gd > salA[gm]) gd -= salA[gm++]
            return LocalDate.of(gy, gm, gd)
        }

        fun toJalali(gy: Int, gm: Int, gd: Int): JalaliCalendar {
            val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            val gy2 = if (gm > 2) gy + 1 else gy
            var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
            var jy = -1595 + 33 * (days / 12053)
            days %= 12053
            jy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                jy += ((days - 1) / 365)
                days = (days - 1) % 365
            }
            val jmOut: Int
            val jdOut: Int
            if (days < 186) {
                jmOut = 1 + (days / 31)
                jdOut = 1 + (days % 31)
            } else {
                jmOut = 7 + ((days - 186) / 30)
                jdOut = 1 + ((days - 186) % 30)
            }
            return JalaliCalendar(jy, jmOut, jdOut)
        }

        fun isLeap(jy: Int): Boolean {
            val a = (jy + 38) % 2820
            return a == 0 || a % 4 == 0
        }

        fun monthLength(jy: Int, jm: Int): Int = when (jm) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isLeap(jy)) 30 else 29
            else -> 30
        }

        fun leadingOffsetForMonth(jy: Int, jm: Int): Int {
            val g = toGregorian(jy, jm, 1)
            return when (g.dayOfWeek) {
                java.time.DayOfWeek.SATURDAY -> 0
                java.time.DayOfWeek.SUNDAY -> 1
                java.time.DayOfWeek.MONDAY -> 2
                java.time.DayOfWeek.TUESDAY -> 3
                java.time.DayOfWeek.WEDNESDAY -> 4
                java.time.DayOfWeek.THURSDAY -> 5
                java.time.DayOfWeek.FRIDAY -> 6
            }
        }

        fun incMonth(jy: Int, jm: Int): Pair<Int, Int> = if (jm == 12) (jy + 1) to 1 else jy to (jm + 1)
        fun decMonth(jy: Int, jm: Int): Pair<Int, Int> = if (jm == 1) (jy - 1) to 12 else jy to (jm - 1)
    }
}
