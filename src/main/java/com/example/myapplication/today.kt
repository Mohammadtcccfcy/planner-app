package com.example.myapplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/*
✅ تغییر این نسخه نسبت به کد شما:
- تاریخِ هدر Today فقط بر اساس CalendarType انتخاب‌شده در Settings نمایش داده می‌شود.
  (جلالی / میلادی / هجری قمری)
- نام ماه‌ها از strings.xml خوانده می‌شوند (نه صرفاً "Hijri" و "Gregorian")
- بقیه‌ی رفتارهای قبلی (locale live، timezone، ساعت HH:mm) دست‌نخورده باقی مانده.
*/

private fun newId(): Long = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

private sealed interface ReminderRecurrence {
    data object Daily : ReminderRecurrence
    data class Weekly(val days: Set<DayOfWeek>) : ReminderRecurrence
    data class Monthly(val dayOfMonth: Int) : ReminderRecurrence
    data class Yearly(val month: Int, val dayOfMonth: Int) : ReminderRecurrence
}

private data class DailyReminder(
    val id: Long = newId(),
    val title: String,
    val time: LocalTime,
    val recurrence: ReminderRecurrence = ReminderRecurrence.Daily,
    val lastDoneOn: LocalDate? = null
)

/* ---------- Saveable helpers ---------- */
private val LocalDateSaver: Saver<LocalDate, String> = Saver(
    save = { it.toString() },
    restore = { LocalDate.parse(it) }
)

private val LocalTimeSaver: Saver<LocalTime, Int> = Saver(
    save = { it.toSecondOfDay() },
    restore = { LocalTime.ofSecondOfDay(it.toLong()) }
)

private val NullableLocalTimeSaver: Saver<LocalTime?, Int> = Saver(
    save = { it?.toSecondOfDay() ?: -1 },
    restore = { secs -> if (secs == -1) null else LocalTime.ofSecondOfDay(secs.toLong()) }
)

/* ---------- Helpers ---------- */
private fun hhmmFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", locale)

private fun safeZoneId(id: String?): ZoneId {
    if (id.isNullOrBlank()) return ZoneId.systemDefault()
    return runCatching { ZoneId.of(id) }.getOrElse { ZoneId.systemDefault() }
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

// ---------------- Date formatting by selected calendar ----------------

private data class JalaliDate(val year: Int, val month: Int, val day: Int)

// Minimal Jalali conversion (same algorithm you used in Calendar file, scoped here)
private fun toJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
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
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + (days / 31)
        jd = 1 + (days % 31)
    } else {
        jm = 7 + ((days - 186) / 30)
        jd = 1 + ((days - 186) % 30)
    }
    return JalaliDate(jy, jm, jd)
}

@Composable
private fun headlineForSelectedCalendar(
    date: LocalDate,
    today: LocalDate,
    calendarType: CalendarType,
    locale: Locale,
    todayWord: String,
    tomorrowWord: String,
    yesterdayWord: String,
): String {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    val prefix = when (date) {
        today -> todayWord
        today.plusDays(1) -> tomorrowWord
        today.minusDays(1) -> yesterdayWord
        else -> null
    }

    val dateText = when (calendarType) {
        CalendarType.GREGORIAN -> {
            val d = date.dayOfMonth
            val mName = gregorianMonthName(date.monthValue)
            val y = date.year
            "$d $mName $y"
        }

        CalendarType.HIJRI -> {
            val h = java.time.chrono.HijrahChronology.INSTANCE.date(date)
            val y = h.get(ChronoField.YEAR_OF_ERA)
            val m = h.get(ChronoField.MONTH_OF_YEAR)
            val d = h.get(ChronoField.DAY_OF_MONTH)
            val mName = hijriMonthName(m)
            "$d $mName $y"
        }

        CalendarType.JALALI -> {
            val j = toJalali(date.year, date.monthValue, date.dayOfMonth)
            val mName = jalaliMonthName(j.month)
            "${j.day} $mName ${j.year}"
        }
    }

    return if (prefix != null) "$prefix $dateText ($dayName)" else "$dateText ($dayName)"
}

private fun occursOn(rem: DailyReminder, date: LocalDate): Boolean =
    when (val r = rem.recurrence) {
        is ReminderRecurrence.Daily -> true
        is ReminderRecurrence.Weekly -> date.dayOfWeek in r.days
        is ReminderRecurrence.Monthly -> date.dayOfMonth == r.dayOfMonth
        is ReminderRecurrence.Yearly -> date.monthValue == r.month && date.dayOfMonth == r.dayOfMonth
    }

private fun minutesLabel(mins: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", (mins / 60) % 24, mins % 60)

@Composable
private fun recurrenceSummary(
    r: ReminderRecurrence,
    locale: Locale,
    everyDay: String,
    weeklyNoDays: String,
    weeklyPrefix: String,
    monthlyPrefix: String,
    yearlyPrefix: String
): String = when (r) {
    is ReminderRecurrence.Daily -> everyDay
    is ReminderRecurrence.Weekly -> {
        if (r.days.isEmpty()) weeklyNoDays
        else "$weeklyPrefix • " + r.days.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }

    is ReminderRecurrence.Monthly -> "$monthlyPrefix • day ${r.dayOfMonth}"
    is ReminderRecurrence.Yearly -> {
        val m = Month.of(r.month).getDisplayName(TextStyle.SHORT, locale)
        "$yearlyPrefix • $m ${r.dayOfMonth}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: WeeklyScheduleState,
    modifier: Modifier = Modifier,
    onAddEvent: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // ✅ تغییر زبان باعث تغییر LocalConfiguration می‌شود -> Recompose
    val configuration = LocalConfiguration.current
    val locale: Locale = remember(configuration) {
        val list = configuration.locales
        if (list.isEmpty) Locale.getDefault() else list[0]
    }

    // ✅ observe settings تا هم زبان و هم timezone و هم calendarType از Planner باعث refresh شوند
    val settings by PlannerData.observeSettings().collectAsState(initial = SettingsEntity())

    val zone: ZoneId = remember(settings.timeZoneId) { safeZoneId(settings.timeZoneId) }

    // ✅ تقویم انتخاب‌شده در Settings
    val calendarType: CalendarType = settings.defaultCalendarType ?: CalendarType.JALALI

    val today = remember { LocalDate.now() }
    var currentDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(today) }

    val fmt = remember(locale) { hhmmFormatter(locale) }

    // ✅ ساعت فعلی برای Header (فقط HH:mm) — timezone-aware
    var nowTime by remember(zone) {
        mutableStateOf(ZonedDateTime.now(zone).truncatedTo(ChronoUnit.MINUTES).toLocalTime())
    }

    // ✅ آپدیت دقیقاً سرِ هر دقیقه (نه هر ثانیه)
    LaunchedEffect(zone) {
        while (true) {
            val now = ZonedDateTime.now(zone)
            val nextMinute = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
            val sleepMs = Duration.between(now, nextMinute).toMillis().coerceAtLeast(250)
            delay(sleepMs)
            nowTime = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.MINUTES).toLocalTime()
        }
    }

    // DB-backed today events (timezone-aware)
    val events by PlannerData.observeTodayEvents(currentDate, zone).collectAsState(initial = emptyList())
    val weeklyToday by PlannerData.observeWeekly(currentDate.dayOfWeek).collectAsState(initial = emptyList())
    val goalsToday by PlannerData.observeGoalsForDate(currentDate).collectAsState(initial = emptyList())

    // In-memory reminders (فعلاً بدون DB)
    val remindersState = remember { mutableStateListOf<DailyReminder>() }
    val visibleReminders by remember(currentDate, remindersState) {
        derivedStateOf { remindersState.filter { occursOn(it, currentDate) } }
    }

    // Dialog toggles
    var showAddEvent by rememberSaveable { mutableStateOf(false) }
    var showAddReminder by rememberSaveable { mutableStateOf(false) }

    // Central TimePicker
    var timePickerOpen by rememberSaveable { mutableStateOf(false) }
    var timePickerInitial by rememberSaveable(stateSaver = LocalTimeSaver) { mutableStateOf(LocalTime.now()) }
    var timePickerOnConfirm by remember { mutableStateOf<(LocalTime) -> Unit>({}) }

    // Event delete/edit
    var confirmDeleteEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var editEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var editEventTitle by rememberSaveable { mutableStateOf("") }

    // Reminder delete/edit
    var confirmDeleteReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editReminderTitle by rememberSaveable { mutableStateOf("") }

    @Suppress("UNUSED_VARIABLE") val _weeklyState = state

    // ---------------- Strings ----------------
    val sPrev = stringResource(R.string.common_prev)
    val sNext = stringResource(R.string.common_next)

    val sTodayWord = stringResource(R.string.common_today)
    val sTomorrowWord = stringResource(R.string.common_tomorrow)
    val sYesterdayWord = stringResource(R.string.common_yesterday)

    val sNoTime = stringResource(R.string.common_no_time)

    val sAdd = stringResource(R.string.common_add)
    val sEdit = stringResource(R.string.common_edit)
    val sDelete = stringResource(R.string.common_delete)

    val sOk = stringResource(R.string.common_ok)
    val sCancel = stringResource(R.string.common_cancel)

    val sectionEvents = stringResource(R.string.today_section_events)
    val sectionWeekly = stringResource(R.string.today_section_weekly_schedule)
    val sectionGoals = stringResource(R.string.today_section_goals_today)
    val sectionReminders = stringResource(R.string.today_section_daily_reminders)

    val todayEmpty = stringResource(R.string.today_empty)
    val weeklyEmpty = stringResource(R.string.weekly_empty)
    val goalsEmpty = stringResource(R.string.goals_empty)
    val anyTime = stringResource(R.string.goals_any_time)

    val remindersEmpty = stringResource(R.string.reminders_empty_for_day)

    val dlgDeleteEventTitle = stringResource(R.string.dialog_delete_event_title)
    val dlgDeleteEventBody = stringResource(R.string.dialog_delete_event_body)
    val dlgDeleteReminderTitle = stringResource(R.string.dialog_delete_reminder_title)
    val dlgDeleteReminderBody = stringResource(R.string.dialog_delete_reminder_body)

    val dlgEditEventTitle = stringResource(R.string.dialog_edit_event_title)
    val dlgEditReminderTitle = stringResource(R.string.dialog_edit_reminder_title)

    val dlgNewEventTitle = stringResource(R.string.dialog_new_event_title)
    val dlgNewReminderTitle = stringResource(R.string.dialog_new_daily_reminder_title)

    val titleHint = stringResource(R.string.today_event_title_hint)

    val reminderDefaultTitle = stringResource(R.string.reminder_default_title)

    val weeklyNoDays = stringResource(R.string.reminder_weekly_no_days)
    val weeklyPrefix = stringResource(R.string.reminder_weekly_prefix)
    val monthlyPrefix = stringResource(R.string.reminder_monthly_prefix)
    val yearlyPrefix = stringResource(R.string.reminder_yearly_prefix)
    val everyDay = stringResource(R.string.reminder_every_day)

    // ---------------- UI ----------------
    val pageScroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(pageScroll)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // ✅ فقط تقویم انتخاب‌شده
                Text(
                    headlineForSelectedCalendar(
                        date = currentDate,
                        today = today,
                        calendarType = calendarType,
                        locale = locale,
                        todayWord = sTodayWord,
                        tomorrowWord = sTomorrowWord,
                        yesterdayWord = sYesterdayWord
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )

                // ✅ فقط ساعت (مثلاً 10:30) — locale-aware via fmt
                Text(
                    text = nowTime.format(fmt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { currentDate = currentDate.minusDays(1) }) { Text(sPrev) }
                TextButton(onClick = { currentDate = currentDate.plusDays(1) }) { Text(sNext) }
            }
        }

        /* ===== Events ===== */
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(sectionEvents, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddEvent = true; onAddEvent() }) {
                        Icon(Icons.Filled.Add, contentDescription = sAdd)
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (events.isEmpty()) {
                    Text(todayEmpty, style = MaterialTheme.typography.bodyMedium)
                } else {
                    val idOrder = remember { mutableStateListOf<String>() }
                    val eventMap = remember(events) { events.associateBy { it.id } }

                    LaunchedEffect(events) {
                        events.forEach { if (!idOrder.contains(it.id)) idOrder.add(it.id) }
                        val toRemove = idOrder.filter { id -> eventMap[id] == null }
                        idOrder.removeAll(toRemove.toSet())
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        idOrder.forEach { id ->
                            val ev = eventMap[id] ?: return@forEach
                            EventRow(
                                title = ev.title,
                                done = ev.done,
                                timeLabel = ev.time?.format(fmt) ?: sNoTime,
                                onChecked = { checked -> scope.launch { PlannerData.setEventDone(ev.id, checked) } },
                                onEditTitle = {
                                    editEventId = ev.id
                                    editEventTitle = ev.title
                                },
                                onEditTime = {
                                    timePickerInitial = ev.time ?: LocalTime.now()
                                    timePickerOnConfirm = { t ->
                                        scope.launch {
                                            PlannerData.deleteEvent(ev.id)
                                            PlannerData.addTodayEvent(currentDate, ev.title, t, zone)
                                        }
                                    }
                                    timePickerOpen = true
                                },
                                onDelete = { confirmDeleteEventId = ev.id },
                                editContentDesc = sEdit,
                                editTimeContentDesc = stringResource(R.string.common_pick_time),
                                deleteContentDesc = sDelete
                            )
                        }
                    }
                }
            }
        }

        /* ===== Weekly schedule ===== */
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(sectionWeekly, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (weeklyToday.isEmpty()) {
                    Text(weeklyEmpty, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weeklyToday.forEach { (startMin, title) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    minutesLabel(startMin),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        /* ===== Goals today ===== */
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(sectionGoals, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (goalsToday.isEmpty()) {
                    Text(goalsEmpty, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        goalsToday.forEach { g ->
                            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(g.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            g.time?.format(fmt) ?: anyTime,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /* ===== Daily reminders ===== */
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(sectionReminders, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddReminder = true }) {
                        Icon(Icons.Filled.Add, contentDescription = sAdd)
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (visibleReminders.isEmpty()) {
                    Text(remindersEmpty, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        visibleReminders.forEach { r ->
                            val checkedToday = r.lastDoneOn == currentDate
                            val recSummary = recurrenceSummary(
                                r = r.recurrence,
                                locale = locale,
                                everyDay = everyDay,
                                weeklyNoDays = weeklyNoDays,
                                weeklyPrefix = weeklyPrefix,
                                monthlyPrefix = monthlyPrefix,
                                yearlyPrefix = yearlyPrefix
                            )
                            ReminderRow(
                                title = r.title,
                                subtitle = "$recSummary • ${r.time.format(fmt)}",
                                checked = checkedToday,
                                onChecked = { checked ->
                                    val i = remindersState.indexOfFirst { it.id == r.id }
                                    if (i != -1) {
                                        remindersState[i] = remindersState[i].copy(
                                            lastDoneOn = if (checked) currentDate else null
                                        )
                                    }
                                },
                                onEdit = {
                                    editReminderId = r.id
                                    editReminderTitle = r.title
                                },
                                onEditTime = {
                                    timePickerInitial = r.time
                                    timePickerOnConfirm = { t ->
                                        val i = remindersState.indexOfFirst { it.id == r.id }
                                        if (i != -1) remindersState[i] = remindersState[i].copy(time = t)
                                    }
                                    timePickerOpen = true
                                },
                                onDelete = { confirmDeleteReminderId = r.id },
                                editContentDesc = sEdit,
                                editTimeContentDesc = stringResource(R.string.common_pick_time),
                                deleteContentDesc = sDelete
                            )
                        }
                    }
                }
            }
        }
    }

    /* ===== Add dialogs ===== */
    if (showAddEvent) {
        AddEventDialog(
            dialogTitle = dlgNewEventTitle,
            titleLabel = titleHint,
            addLabel = sAdd,
            cancelLabel = sCancel,
            fmt = fmt,
            onDismiss = { showAddEvent = false },
            onPickTime = { setter ->
                timePickerInitial = LocalTime.now()
                timePickerOnConfirm = setter
                timePickerOpen = true
            },
            onAdd = { title, time ->
                val t = title.trim()
                scope.launch {
                    if (t.isNotEmpty()) PlannerData.addTodayEvent(currentDate, t, time, zone)
                }
                showAddEvent = false
            }
        )
    }

    if (showAddReminder) {
        AddDailyReminderDialog(
            dialogTitle = dlgNewReminderTitle,
            titleLabel = titleHint,
            defaultTitle = reminderDefaultTitle,
            addLabel = sAdd,
            cancelLabel = sCancel,
            fmt = fmt,
            locale = locale,
            onDismiss = { showAddReminder = false },
            onPickTime = { current, setter ->
                timePickerInitial = current
                timePickerOnConfirm = setter
                timePickerOpen = true
            },
            onAdd = { title, time, recurrence ->
                val t = title.ifBlank { reminderDefaultTitle }.trim()
                remindersState.add(DailyReminder(title = t, time = time, recurrence = recurrence))
                showAddReminder = false
            }
        )
    }

    /* ===== Central 24h TimePicker ===== */
    if (timePickerOpen) {
        val pickerState = rememberTimePickerState(
            initialHour = timePickerInitial.hour,
            initialMinute = timePickerInitial.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { timePickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    timePickerOnConfirm(LocalTime.of(pickerState.hour, pickerState.minute))
                    timePickerOpen = false
                }) { Text(sOk) }
            },
            dismissButton = {
                TextButton(onClick = { timePickerOpen = false }) { Text(sCancel) }
            },
            text = { TimePicker(state = pickerState) }
        )
    }

    /* ===== Confirm DELETE: Event ===== */
    confirmDeleteEventId?.let { delId ->
        AlertDialog(
            onDismissRequest = { confirmDeleteEventId = null },
            title = { Text(dlgDeleteEventTitle) },
            text = { Text(dlgDeleteEventBody) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { PlannerData.deleteEvent(delId) }
                    confirmDeleteEventId = null
                }) { Text(sDelete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteEventId = null }) { Text(sCancel) }
            }
        )
    }

    /* ===== EDIT: Event title ===== */
    LaunchedEffect(editEventId, events) {
        val id = editEventId ?: return@LaunchedEffect
        if (events.none { it.id == id }) {
            editEventId = null
            editEventTitle = ""
        }
    }

    editEventId?.let { id ->
        val ev = events.firstOrNull { it.id == id }
        if (ev != null) {
            AlertDialog(
                onDismissRequest = {
                    editEventId = null
                    editEventTitle = ""
                },
                title = { Text(dlgEditEventTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editEventTitle,
                            onValueChange = { editEventTitle = it },
                            label = { Text(titleHint) },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newT = editEventTitle.trim()
                        if (newT.isNotEmpty()) scope.launch { PlannerData.renameEvent(id, newT) }
                        editEventId = null
                        editEventTitle = ""
                    }) { Text(sOk) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        editEventId = null
                        editEventTitle = ""
                    }) { Text(sCancel) }
                }
            )
        }
    }

    /* ===== Confirm DELETE: Reminder ===== */
    confirmDeleteReminderId?.let { rid ->
        AlertDialog(
            onDismissRequest = { confirmDeleteReminderId = null },
            title = { Text(dlgDeleteReminderTitle) },
            text = { Text(dlgDeleteReminderBody) },
            confirmButton = {
                TextButton(onClick = {
                    remindersState.removeAll { it.id == rid }
                    confirmDeleteReminderId = null
                }) { Text(sDelete) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteReminderId = null }) { Text(sCancel) } }
        )
    }

    /* ===== EDIT: Reminder title ===== */
    LaunchedEffect(editReminderId, remindersState) {
        val id = editReminderId ?: return@LaunchedEffect
        if (remindersState.none { it.id == id }) {
            editReminderId = null
            editReminderTitle = ""
        }
    }

    editReminderId?.let { id ->
        val r = remindersState.firstOrNull { it.id == id }
        if (r != null) {
            AlertDialog(
                onDismissRequest = {
                    editReminderId = null
                    editReminderTitle = ""
                },
                title = { Text(dlgEditReminderTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editReminderTitle,
                            onValueChange = { editReminderTitle = it },
                            label = { Text(titleHint) },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val t = editReminderTitle.trim()
                        if (t.isNotEmpty()) {
                            val i = remindersState.indexOfFirst { it.id == id }
                            if (i != -1) remindersState[i] = remindersState[i].copy(title = t)
                        }
                        editReminderId = null
                        editReminderTitle = ""
                    }) { Text(sOk) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        editReminderId = null
                        editReminderTitle = ""
                    }) { Text(sCancel) }
                }
            )
        }
    }

    // ✅ no-op: تضمین می‌کند تغییر زبان/زون/تقویم از Settings باعث “alive بودن” این Composable شود
    LaunchedEffect(settings.languageTag, settings.timeZoneId, settings.defaultCalendarType) { /* no-op */ }
}

/* ---------- Rows ---------- */

@Composable
private fun EventRow(
    title: String,
    done: Boolean,
    timeLabel: String,
    onChecked: (Boolean) -> Unit,
    onEditTitle: () -> Unit,
    onEditTime: () -> Unit,
    onDelete: () -> Unit,
    editContentDesc: String,
    editTimeContentDesc: String,
    deleteContentDesc: String
) {
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = done, onCheckedChange = onChecked)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(timeLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditTitle) { Icon(Icons.Filled.Edit, contentDescription = editContentDesc) }
                IconButton(onClick = onEditTime) { Icon(Icons.Filled.AccessTime, contentDescription = editTimeContentDesc) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = deleteContentDesc) }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onEditTime: () -> Unit,
    onDelete: () -> Unit,
    editContentDesc: String,
    editTimeContentDesc: String,
    deleteContentDesc: String
) {
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = onChecked)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = editContentDesc) }
                IconButton(onClick = onEditTime) { Icon(Icons.Filled.AccessTime, contentDescription = editTimeContentDesc) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = deleteContentDesc) }
            }
        }
    }
}

/* ---------- Add dialogs ---------- */

@Composable
private fun AddEventDialog(
    dialogTitle: String,
    titleLabel: String,
    addLabel: String,
    cancelLabel: String,
    fmt: DateTimeFormatter,
    onDismiss: () -> Unit,
    onPickTime: ((LocalTime) -> Unit) -> Unit,
    onAdd: (title: String, time: LocalTime?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var selectedTime by rememberSaveable(stateSaver = NullableLocalTimeSaver) { mutableStateOf<LocalTime?>(null) }

    val pickTimeLabel = stringResource(R.string.common_pick_time)
    val timeNone = stringResource(R.string.common_time_none)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onAdd(title, selectedTime) }) { Text(addLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(titleLabel) },
                    singleLine = true
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = selectedTime?.let { fmt.format(it) } ?: timeNone
                    Text(label)
                    TextButton(onClick = { onPickTime { t -> selectedTime = t } }) { Text(pickTimeLabel) }
                }
            }
        }
    )
}

@Composable
private fun AddDailyReminderDialog(
    dialogTitle: String,
    titleLabel: String,
    defaultTitle: String,
    addLabel: String,
    cancelLabel: String,
    fmt: DateTimeFormatter,
    locale: Locale,
    onDismiss: () -> Unit,
    onPickTime: (current: LocalTime, setter: (LocalTime) -> Unit) -> Unit,
    onAdd: (title: String, time: LocalTime, recurrence: ReminderRecurrence) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(defaultTitle) }
    var selectedTime by rememberSaveable(stateSaver = LocalTimeSaver) { mutableStateOf(LocalTime.of(8, 0)) }

    var repeatExpanded by rememberSaveable { mutableStateOf(false) }
    var recurrence by remember { mutableStateOf<ReminderRecurrence>(ReminderRecurrence.Daily) }

    val weekDays = remember {
        listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
    }
    val weeklyChecked = remember {
        mutableStateMapOf<DayOfWeek, Boolean>().apply { weekDays.forEach { put(it, false) } }
    }

    LaunchedEffect(recurrence) {
        if (recurrence !is ReminderRecurrence.Weekly) {
            weeklyChecked.keys.forEach { weeklyChecked[it] = false }
        }
    }

    var monthlyDay by rememberSaveable { mutableStateOf("1") }
    var yearlyMonth by rememberSaveable { mutableIntStateOf(1) }
    var yearlyDay by rememberSaveable { mutableStateOf("1") }

    val repeatLabel = stringResource(R.string.reminder_repeat)
    val showLabel = stringResource(R.string.common_show)
    val hideLabel = stringResource(R.string.common_hide)

    val everyDay = stringResource(R.string.reminder_every_day)
    val weeklyPick = stringResource(R.string.reminder_weekly_pick_days)
    val monthlyByDay = stringResource(R.string.reminder_monthly_by_day)
    val yearly = stringResource(R.string.reminder_yearly)

    val dayOfMonthLabel = stringResource(R.string.reminder_day_of_month)
    val dayRangeLabel = stringResource(R.string.reminder_day_1_31)
    val monthLabel = stringResource(R.string.reminder_month_label)

    val pickTimeLabel = stringResource(R.string.common_pick_time)
    val timeLabel = stringResource(R.string.common_time_label)

    fun currentRecurrence(): ReminderRecurrence = when (recurrence) {
        is ReminderRecurrence.Daily -> ReminderRecurrence.Daily
        is ReminderRecurrence.Weekly -> {
            val set = weeklyChecked.filterValues { it }.keys
            if (set.isEmpty()) ReminderRecurrence.Daily else ReminderRecurrence.Weekly(set)
        }

        is ReminderRecurrence.Monthly -> {
            val d = monthlyDay.toIntOrNull()?.coerceIn(1, 31) ?: 1
            ReminderRecurrence.Monthly(d)
        }

        is ReminderRecurrence.Yearly -> {
            val d = yearlyDay.toIntOrNull()?.coerceIn(1, 31) ?: 1
            ReminderRecurrence.Yearly(yearlyMonth.coerceIn(1, 12), d)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onAdd(title.trim(), selectedTime, currentRecurrence()) }) { Text(addLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(titleLabel) },
                    singleLine = true
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$timeLabel ${fmt.format(selectedTime)}")
                    TextButton(onClick = { onPickTime(selectedTime) { t -> selectedTime = t } }) { Text(pickTimeLabel) }
                }

                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (repeatExpanded) 8.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(repeatLabel)
                            TextButton(onClick = { repeatExpanded = !repeatExpanded }) {
                                Text(if (repeatExpanded) hideLabel else showLabel)
                            }
                        }

                        if (repeatExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = recurrence is ReminderRecurrence.Daily,
                                        onClick = { recurrence = ReminderRecurrence.Daily }
                                    )
                                    Text(everyDay)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = recurrence is ReminderRecurrence.Weekly,
                                        onClick = { recurrence = ReminderRecurrence.Weekly(emptySet()) }
                                    )
                                    Text(weeklyPick)
                                }
                                if (recurrence is ReminderRecurrence.Weekly) {
                                    weekDays.forEach { d ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = weeklyChecked[d] == true,
                                                onCheckedChange = { weeklyChecked[d] = it }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(d.getDisplayName(TextStyle.SHORT, locale))
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = recurrence is ReminderRecurrence.Monthly,
                                        onClick = { recurrence = ReminderRecurrence.Monthly(1) }
                                    )
                                    Text(monthlyByDay)
                                }
                                if (recurrence is ReminderRecurrence.Monthly) {
                                    OutlinedTextField(
                                        value = monthlyDay,
                                        onValueChange = { monthlyDay = it.filter(Char::isDigit).take(2) },
                                        singleLine = true,
                                        label = { Text(dayOfMonthLabel) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(dayRangeLabel, style = MaterialTheme.typography.bodySmall)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = recurrence is ReminderRecurrence.Yearly,
                                        onClick = { recurrence = ReminderRecurrence.Yearly(1, 1) }
                                    )
                                    Text(yearly)
                                }
                                if (recurrence is ReminderRecurrence.Yearly) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(monthLabel)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(onClick = { yearlyMonth = (yearlyMonth - 1).coerceAtLeast(1) }) { Text("−") }
                                                Text(Month.of(yearlyMonth).getDisplayName(TextStyle.SHORT, locale))
                                                TextButton(onClick = { yearlyMonth = (yearlyMonth + 1).coerceAtMost(12) }) { Text("+") }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = yearlyDay,
                                            onValueChange = { yearlyDay = it.filter(Char::isDigit).take(2) },
                                            singleLine = true,
                                            label = { Text(dayOfMonthLabel) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        } else {
                            val weeklyNoDays2 = stringResource(R.string.reminder_weekly_no_days)
                            val weeklyPrefix2 = stringResource(R.string.reminder_weekly_prefix)
                            val monthlyPrefix2 = stringResource(R.string.reminder_monthly_prefix)
                            val yearlyPrefix2 = stringResource(R.string.reminder_yearly_prefix)
                            val everyDay2 = stringResource(R.string.reminder_every_day)

                            val sum = recurrenceSummary(
                                r = currentRecurrence(),
                                locale = locale,
                                everyDay = everyDay2,
                                weeklyNoDays = weeklyNoDays2,
                                weeklyPrefix = weeklyPrefix2,
                                monthlyPrefix = monthlyPrefix2,
                                yearlyPrefix = yearlyPrefix2
                            )
                            Text(sum, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    val previewState = WeeklyScheduleState().apply { scheduleCreated = true }
    MaterialTheme { TodayScreen(state = previewState) }
}
