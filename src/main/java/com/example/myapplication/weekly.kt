package com.example.myapplication

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.min

private const val END_OF_DAY_MIN = 24 * 60

private fun hhmmFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", locale)

private fun minutesLabel(mins: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", (mins / 60) % 24, mins % 60)

private fun timeToMinutes(t: LocalTime): Int = t.hour * 60 + t.minute

private fun buildSlots(startMinutes: Int, blockMinutes: Int): List<Int> {
    val block = blockMinutes.coerceAtLeast(60)
    val first = if (startMinutes < END_OF_DAY_MIN) startMinutes else (END_OF_DAY_MIN - block).coerceAtLeast(0)
    val out = mutableListOf<Int>()
    var t = first
    while (t < END_OF_DAY_MIN) {
        out += t
        t += block
    }
    return out
}

private fun orderedWeek(start: DayOfWeek): List<DayOfWeek> =
    (0 until 7).map { DayOfWeek.of(((start.value - 1 + it) % 7) + 1) }

private fun startOfWeekDate(today: LocalDate, startDay: DayOfWeek): LocalDate =
    today.with(TemporalAdjusters.previousOrSame(startDay))

private fun dateForPage(startOfWeek: LocalDate, pageIndex: Int): LocalDate =
    startOfWeek.plusDays(pageIndex.toLong())

class WeeklyScheduleState(
    startDay: DayOfWeek = DayOfWeek.MONDAY,
    startMinutes: Int = 8 * 60,
    blockMinutes: Int = 60
) {
    var startDay by mutableStateOf(startDay)
    var startMinutes by mutableStateOf(startMinutes)
    var blockMinutes by mutableStateOf(blockMinutes)
    var scheduleCreated by mutableStateOf(false)
}

@Composable
fun rememberWeeklyScheduleState(): WeeklyScheduleState = remember { WeeklyScheduleState() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekdayDropdown(locale: Locale, selectedDay: DayOfWeek, onDaySelected: (DayOfWeek) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val days = remember { DayOfWeek.values().toList() }
    val label = stringResource(R.string.weekly_start_weekday_label)

    Box(
        modifier = Modifier
            .widthIn(min = 180.dp)
            .clickable { expanded = true }
    ) {
        TextField(
            readOnly = true,
            value = selectedDay.getDisplayName(TextStyle.FULL, locale),
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            days.forEach { day ->
                DropdownMenuItem(
                    text = { Text(day.getDisplayName(TextStyle.FULL, locale)) },
                    onClick = { onDaySelected(day); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartTimePickerMinutes(locale: Locale, minutes: Int, onMinutes: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }

    val sDayStarts = stringResource(R.string.weekly_day_starts_label)
    val sSelectStartTime = stringResource(R.string.weekly_select_start_time_title)
    val sOk = stringResource(R.string.common_ok)
    val sCancel = stringResource(R.string.common_cancel)

    val hour = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    val fmt = remember(locale) { hhmmFormatter(locale) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(sDayStarts, style = MaterialTheme.typography.bodyMedium)
        FilledTonalButton(onClick = { open = true }) {
            Text(LocalTime.of(hour, minute).format(fmt))
        }
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(sSelectStartTime) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                Button(onClick = {
                    onMinutes(pickerState.hour * 60 + pickerState.minute)
                    open = false
                }) { Text(sOk) }
            },
            dismissButton = {
                Button(onClick = { open = false }) { Text(sCancel) }
            }
        )
    }
}

@Composable
private fun BlockSizePickerMinutes(blockMinutes: Int, onChange: (Int) -> Unit) {
    val sBlockSize = stringResource(R.string.weekly_block_size_label)
    val s1h = stringResource(R.string.weekly_block_1h)
    val s2h = stringResource(R.string.weekly_block_2h)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(sBlockSize, style = MaterialTheme.typography.bodyMedium)
        FilterChip(selected = blockMinutes == 60, onClick = { onChange(60) }, label = { Text(s1h) })
        FilterChip(selected = blockMinutes == 120, onClick = { onChange(120) }, label = { Text(s2h) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeeklyScheduleScreen(state: WeeklyScheduleState) {
    val configuration = LocalConfiguration.current
    val locale: Locale = remember(configuration) {
        val list = configuration.locales
        if (list.isEmpty) Locale.getDefault() else list[0]
    }

    val sWeeklySetup = stringResource(R.string.weekly_setup_title)
    val sCreate = stringResource(R.string.weekly_create_schedule)
    val sSwipeHint = stringResource(R.string.weekly_swipe_hint)
    val sEndsAtMidnight = stringResource(R.string.weekly_ends_at_midnight)

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.scheduleCreated) {
            Text(sWeeklySetup, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            WeekdayDropdown(locale = locale, selectedDay = state.startDay, onDaySelected = { state.startDay = it })
            StartTimePickerMinutes(locale = locale, minutes = state.startMinutes, onMinutes = { state.startMinutes = it })
            BlockSizePickerMinutes(blockMinutes = state.blockMinutes, onChange = { state.blockMinutes = it })

            Spacer(Modifier.height(8.dp))
            Button(onClick = { state.scheduleCreated = true }) { Text(sCreate) }

            Text(
                sSwipeHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val pages = remember(state.startDay) { orderedWeek(state.startDay) }
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { 7 })

            val today = remember { LocalDate.now() }
            val startOfWeek = remember(today, state.startDay) { startOfWeekDate(today, state.startDay) }

            Text(
                pages[pagerState.currentPage].getDisplayName(TextStyle.FULL, locale),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                sEndsAtMidnight,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val day = pages[page]
                val date = remember(startOfWeek, page) { dateForPage(startOfWeek, page) }
                DayPage(
                    locale = locale,
                    day = day,
                    date = date,
                    startMinutes = state.startMinutes,
                    blockMinutes = state.blockMinutes
                )
            }
        }
    }

    LaunchedEffect(locale) { }
}

@Composable
private fun DayPage(
    locale: Locale,
    day: DayOfWeek,
    date: LocalDate,
    startMinutes: Int,
    blockMinutes: Int
) {
    val slotsForDay by PlannerData.observeWeekly(day).collectAsState(initial = emptyList())
    val blocks = remember(startMinutes, blockMinutes) { buildSlots(startMinutes, blockMinutes) }

    val goalsForDate by PlannerData.observeGoalsForDate(date).collectAsState(initial = emptyList())
    val timedGoalsForThisDate = remember(goalsForDate) {
        goalsForDate
            .filter { it.time != null }
            .sortedWith(compareBy<UiGoal>({ it.time!!.hour }, { it.time!!.minute }))
    }

    val goalsByBlock by remember(timedGoalsForThisDate, blocks) {
        derivedStateOf {
            if (blocks.isEmpty()) return@derivedStateOf emptyMap<Int, List<UiGoal>>()
            val map = mutableMapOf<Int, MutableList<UiGoal>>()
            val firstBlockStart = blocks.first()

            timedGoalsForThisDate.forEach { g ->
                val m = timeToMinutes(g.time!!)
                val blockStart = blocks.lastOrNull { it <= m } ?: firstBlockStart
                map.getOrPut(blockStart) { mutableListOf() }.add(g)
            }

            map.mapValues { (_, v) ->
                v.sortedWith(compareBy<UiGoal>({ it.time!!.hour }, { it.time!!.minute }))
            }
        }
    }

    val fmt = remember(locale) { hhmmFormatter(locale) }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(blocks) { start ->
            val titlesForBlock = slotsForDay.filter { it.first == start }.map { it.second }
            val goalsForBlock = goalsByBlock[start].orEmpty()

            TimeBlockCard(
                timeFormatter = fmt,
                day = day,
                startMinutes = start,
                blockMinutes = blockMinutes,
                goals = goalsForBlock,
                titles = titlesForBlock
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TimeBlockCard(
    timeFormatter: DateTimeFormatter,
    day: DayOfWeek,
    startMinutes: Int,
    blockMinutes: Int,
    goals: List<UiGoal>,
    titles: List<String>
) {
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var pendingRemoveIndex by remember { mutableStateOf<Int?>(null) }

    val end = min(startMinutes + blockMinutes, END_OF_DAY_MIN)
    val label = "${minutesLabel(startMinutes)}–${minutesLabel(end)}"

    val sAddEvent = stringResource(R.string.weekly_add_event)
    val sRemoveEvent = stringResource(R.string.weekly_remove_event)
    val sNoEvents = stringResource(R.string.weekly_no_events)
    val sTitle = stringResource(R.string.common_title)

    val sOk = stringResource(R.string.common_ok)
    val sCancel = stringResource(R.string.common_cancel)

    val sDlgRemoveTitle = stringResource(R.string.dialog_remove_event_title)
    val sDlgRemoveBody = stringResource(R.string.dialog_remove_event_body)

    val sGoalChip = stringResource(R.string.goals_goal_chip)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAdd = true }, enabled = titles.size < 2) {
                    Surface(tonalElevation = 3.dp, modifier = Modifier.clip(CircleShape)) {
                        Icon(Icons.Filled.Add, contentDescription = sAddEvent)
                    }
                }
            }

            if (goals.isNotEmpty()) {
                goals.forEach { g ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(sGoalChip) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(g.title)
                        }
                        Text(
                            g.time?.format(timeFormatter) ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
            }

            if (titles.isEmpty()) {
                Text(sNoEvents, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                titles.forEachIndexed { index, title ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(title)
                        IconButton(onClick = { pendingRemoveIndex = index }) {
                            Surface(tonalElevation = 2.dp, modifier = Modifier.clip(CircleShape)) {
                                Icon(Icons.Filled.Delete, contentDescription = sRemoveEvent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; newTitle = "" },
            title = { Text(sAddEvent) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text(sTitle) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = newTitle.trim()
                        if (t.isNotEmpty() && titles.size < 2) {
                            scope.launch { PlannerData.addWeeklySlot(day, startMinutes, t) }
                        }
                        newTitle = ""
                        showAdd = false
                    },
                    enabled = titles.size < 2 && newTitle.isNotBlank()
                ) { Text(sOk) }
            },
            dismissButton = {
                Button(onClick = { newTitle = ""; showAdd = false }) { Text(sCancel) }
            }
        )
    }

    pendingRemoveIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { pendingRemoveIndex = null },
            title = { Text(sDlgRemoveTitle) },
            text = { Text(sDlgRemoveBody) },
            confirmButton = {
                Button(onClick = {
                    scope.launch { PlannerData.deleteWeeklySlotByIndex(day, startMinutes, idx) }
                    pendingRemoveIndex = null
                }) { Text(sOk) }
            },
            dismissButton = {
                Button(onClick = { pendingRemoveIndex = null }) { Text(sCancel) }
            }
        )
    }
}
