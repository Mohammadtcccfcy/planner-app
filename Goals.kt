package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

/* =============================================================
 * Goals UI (connected to PlannerData/Room)
 * - Uses PlannerData.observeGoals()/upsertGoal()/deleteGoal().
 * - Duration (months) text field stays EMPTY by default.
 * - All user-visible text is moved to string resources.
 *
 * NOTE: This file expects you to add these string resources
 * (and translations for other languages):
 *  - goals_summary_format
 *  - goals_duration_short
 *  - goals_preview
 *  - goals_my_goal
 *  - goals_today_empty
 * ============================================================= */

@Composable
fun GoalsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    val goals by PlannerData.observeGoals().collectAsState(initial = emptyList())

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UiGoal?>(null) }

    val sAddGoal = stringResource(R.string.goals_add)
    val sEmpty = stringResource(R.string.goals_empty)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = sAddGoal)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (goals.isEmpty()) {
                item { Text(sEmpty) }
            } else {
                items(goals, key = { it.id }) { g ->
                    GoalCard(
                        goal = g,
                        onEdit = { editing = g; showEditor = true },
                        onDelete = { scope.launch { PlannerData.deleteGoal(g.id) } }
                    )
                }
            }
        }

        if (showEditor) {
            GoalEditorDialog(
                initial = editing,
                onDismiss = { showEditor = false },
                onSave = { draft ->
                    scope.launch { PlannerData.upsertGoal(draft) }
                    showEditor = false
                }
            )
        }
    }
}

@Composable
private fun GoalCard(goal: UiGoal, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sEdit = stringResource(R.string.goals_edit)
    val sDelete = stringResource(R.string.goals_delete)

    ElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(goalSummary(goal), style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = sEdit) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = sDelete) }
            }
        }
    }
}

private fun rotateMonths(startMonth: Int, count: Int): Set<Int> {
    if (count <= 0) return emptySet()
    val out = LinkedHashSet<Int>(count)
    var m = startMonth.coerceIn(1, 12)
    repeat(count.coerceAtMost(120)) {
        out += m
        m = if (m == 12) 1 else m + 1
    }
    return out
}

@Composable
private fun goalSummary(g: UiGoal): String {
    val locale = Locale.getDefault()

    val monthsList = rotateMonths(g.startMonth, g.monthsCount)
        .sorted()
        .joinToString(", ") { Month.of(it).getDisplayName(TextStyle.SHORT, locale) }

    val daysLabel = if (g.everyDay) {
        stringResource(R.string.goals_every_day)
    } else {
        g.daysOfWeek.sortedBy { it.value }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }

    val timeLabel = g.time?.toString() ?: stringResource(R.string.goals_any_time)

    // requires: <string name="goals_summary_format">%1$d %2$s • %3$s • %4$s • %5$s</string>
    // requires: <string name="goals_duration_short">mo</string>
    return stringResource(
        R.string.goals_summary_format,
        g.monthsCount,
        stringResource(R.string.goals_duration_short),
        monthsList,
        daysLabel,
        timeLabel
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GoalEditorDialog(
    initial: UiGoal?,
    onDismiss: () -> Unit,
    onSave: (UiGoal) -> Unit
) {
    val today = LocalDate.now()
    val currentMonth = today.monthValue

    val sNew = stringResource(R.string.goals_new_goal)
    val sEdit = stringResource(R.string.goals_edit)
    val sTitle = stringResource(R.string.goals_title_hint)
    val sStartMonth = stringResource(R.string.goals_start_month)
    val sDuration = stringResource(R.string.goals_duration_months)
    val sEveryDay = stringResource(R.string.goals_every_day)
    val sSetTime = stringResource(R.string.goals_set_time_optional)
    val sSave = stringResource(R.string.goals_save)
    val sCancel = stringResource(R.string.goals_cancel)
    val sAnyTime = stringResource(R.string.goals_any_time)

    // requires: <string name="goals_preview">Preview:</string>
    val sPreview = stringResource(R.string.goals_preview)

    // requires: <string name="goals_my_goal">My goal</string>
    val sMyGoal = stringResource(R.string.goals_my_goal)

    var title by remember { mutableStateOf(TextFieldValue(initial?.title ?: "")) }
    var startMonth by remember { mutableStateOf(initial?.startMonth ?: currentMonth) }

    // Duration UI shows EMPTY string by default
    var durationText by remember { mutableStateOf(TextFieldValue(initial?.monthsCount?.toString() ?: "")) }

    var everyDay by remember { mutableStateOf(initial?.everyDay ?: true) }
    var pickedDays by remember { mutableStateOf(initial?.daysOfWeek ?: DayOfWeek.entries.toSet()) }

    var hasTime by remember { mutableStateOf(initial?.time != null) }
    var time by remember { mutableStateOf(initial?.time ?: LocalTime.of(9, 0)) }
    val timeState = rememberTimePickerState(time.hour, time.minute, true)

    val parsedDuration by remember(durationText.text) {
        mutableStateOf(durationText.text.filter(Char::isDigit).take(3).toIntOrNull()?.coerceAtLeast(1) ?: 1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) sNew else sEdit) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(sTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(sStartMonth, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonthSpinner(month = startMonth, onChange = { m -> startMonth = m })
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { input ->
                        val cleaned = input.text.filter(Char::isDigit).take(3)
                        durationText = TextFieldValue(cleaned)
                    },
                    label = { Text(sDuration) },
                    singleLine = true,
                    modifier = Modifier.width(220.dp)
                )

                Text(
                    "$sPreview " + rotateMonths(startMonth, parsedDuration).joinToString(", ") {
                        Month.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = everyDay, onCheckedChange = { checked -> everyDay = checked })
                    Text(sEveryDay)
                }

                if (!everyDay) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.entries.forEach { d ->
                            FilterChipSmall(
                                label = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                selected = pickedDays.contains(d),
                                onClick = {
                                    pickedDays = if (pickedDays.contains(d)) pickedDays - d else pickedDays + d
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasTime, onCheckedChange = { checked -> hasTime = checked })
                    Text(sSetTime)
                }

                if (hasTime) {
                    TimePicker(state = timeState)
                    LaunchedEffect(timeState.hour, timeState.minute) {
                        time = LocalTime.of(timeState.hour, timeState.minute)
                    }
                } else {
                    Text(sAnyTime, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanTitle = title.text.trim().ifEmpty { sMyGoal }
                val days = if (everyDay) DayOfWeek.entries.toSet() else pickedDays.ifEmpty { DayOfWeek.entries.toSet() }
                onSave(
                    UiGoal(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        title = cleanTitle,
                        startMonth = startMonth,
                        monthsCount = parsedDuration,
                        everyDay = everyDay,
                        daysOfWeek = days,
                        time = if (hasTime) time else null
                    )
                )
            }) { Text(sSave) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(sCancel) } }
    )
}

@Composable
private fun MonthSpinner(month: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())

    Box {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.goals_start_month)) },
            modifier = Modifier
                .width(160.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..12).forEach { m ->
                DropdownMenuItem(
                    text = { Text(Month.of(m).getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                    onClick = { onChange(m); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FilterChipSmall(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    )
}

// ---------------- Hooks you can call from Today / Weekly ----------------

@Composable
fun GoalsTodayCard(currentDate: LocalDate, modifier: Modifier = Modifier) {
    val goalsToday by PlannerData.observeGoalsForDate(currentDate).collectAsState(initial = emptyList())

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.today_section_goals_today), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (goalsToday.isEmpty()) {
                // requires: <string name="goals_today_empty">No goals for today.</string>
                Text(stringResource(R.string.goals_today_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goalsToday, key = { it.id }) { g ->
                        Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(g.title, style = MaterialTheme.typography.titleSmall)
                                    Text(g.time?.toString() ?: stringResource(R.string.goals_any_time), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberTimedGoalsForDay(
    day: DayOfWeek,
    referenceDate: LocalDate = LocalDate.now()
): State<List<Pair<Int, String>>> {
    return PlannerData.observeTimedGoalsForDayOfWeek(day, referenceDate)
        .collectAsState(initial = emptyList())
}
