package com.gonec009.meshizandaka.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.DailyMealStack
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private data class SummaryItem(
    val title: String,
    val value: String,
)

private enum class ChartMealSection {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

private data class ChartMealDialogState(
    val date: LocalDate,
    val section: ChartMealSection,
    val records: List<MealRecord>,
)

private data class PendingDeleteRecord(
    val record: MealRecord,
)

private val ChartBarWidth = 39.dp

private fun formatChartCalories(calories: Int): String {
    return "${calories}K"
}

private fun ChartMealSection.labelResId(): Int = when (this) {
    ChartMealSection.BREAKFAST -> R.string.meal_type_breakfast
    ChartMealSection.LUNCH -> R.string.meal_type_lunch
    ChartMealSection.DINNER -> R.string.meal_type_dinner
    ChartMealSection.SNACK -> R.string.chart_snack
}

private fun DailyMealStack.recordsForSection(section: ChartMealSection): List<MealRecord> = when (section) {
    ChartMealSection.BREAKFAST -> breakfastRecords
    ChartMealSection.LUNCH -> lunchRecords
    ChartMealSection.DINNER -> dinnerRecords
    ChartMealSection.SNACK -> snackRecords
}

internal fun formatChartDateLabel(
    date: LocalDate,
    previousDate: LocalDate?,
): String {
    return if (date.dayOfMonth == 1 && previousDate?.month != date.month) {
        "${date.monthValue}/${date.dayOfMonth}"
    } else {
        date.dayOfMonth.toString()
    }
}

@Composable
fun HomeRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onQuickRecordClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    HomeScreen(
        innerPadding = innerPadding,
        state = state,
        onQuickRecordClick = onQuickRecordClick,
        onMoveSelectedDate = viewModel::moveSelectedRecordDate,
        onDateSelected = viewModel::updateSelectedRecordDate,
    onBreakfastClick = viewModel::recordBreakfast,
    onLunchClick = viewModel::recordLunch,
    onDinnerClick = viewModel::recordDinner,
    onToggleRecentRecords = viewModel::toggleRecentRecords,
    onRecordClick = onRecordClick,
    onDeleteRecord = viewModel::deleteRecord,
    )
}

@Composable
private fun HomeScreen(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onQuickRecordClick: () -> Unit,
    onMoveSelectedDate: (Long) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onBreakfastClick: () -> Unit,
    onLunchClick: () -> Unit,
    onDinnerClick: () -> Unit,
    onToggleRecentRecords: () -> Unit,
    onRecordClick: (Long) -> Unit,
    onDeleteRecord: (Long, () -> Unit) -> Unit,
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    var pendingDeleteRecord by remember { mutableStateOf<PendingDeleteRecord?>(null) }
    val summaryItems = listOf(
        SummaryItem(
            title = stringResource(R.string.today_consumed),
            value = stringResource(R.string.kcal_format, state.summary.todayConsumedCalories),
        ),
        SummaryItem(
            title = stringResource(R.string.today_balance),
            value = stringResource(R.string.kcal_format_signed, state.summary.todayBalanceCalories),
        ),
        SummaryItem(
            title = stringResource(R.string.week_balance),
            value = stringResource(R.string.kcal_format_signed, state.summary.weekBalanceCalories),
        ),
        SummaryItem(
            title = stringResource(R.string.month_balance),
            value = stringResource(R.string.kcal_format_signed, state.summary.monthBalanceCalories),
        ),
        SummaryItem(
            title = stringResource(R.string.month_special_count),
            value = stringResource(R.string.count_format, state.summary.monthSpecialCount),
        ),
        SummaryItem(
            title = stringResource(R.string.month_special_delta),
            value = stringResource(R.string.kcal_format_signed, state.summary.monthSpecialDeltaCalories),
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordDateSelector(
                    selectedDate = state.selectedRecordDate,
                    onPreviousDateClick = { onMoveSelectedDate(-1L) },
                    onNextDateClick = { onMoveSelectedDate(1L) },
                    onOpenDatePicker = { isDatePickerVisible = true },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onBreakfastClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.breakfast_set))
                    }
                    Button(onClick = onLunchClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.lunch_set))
                    }
                    Button(onClick = onDinnerClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dinner_set))
                    }
                }
                Button(onClick = onQuickRecordClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.go_to_record))
                }
            }
        }
        item {
            WeeklyChartCard(
                stacks = state.weeklyChart.days,
                maxCalories = state.weeklyChart.maxTotalCalories,
                modifier = Modifier.testTag("weekly_chart_card"),
                onDeleteRecordRequest = { _, record ->
                    pendingDeleteRecord = PendingDeleteRecord(
                        record = record,
                    )
                },
            )
        }
        item {
            CompactSummaryPanel(items = summaryItems)
        }
        item {
            RecentRecordsSection(
                records = state.recentRecords,
                expanded = state.isRecentRecordsExpanded,
                onToggle = onToggleRecentRecords,
                onRecordClick = onRecordClick,
            )
        }
    }

    if (isDatePickerVisible) {
        val zoneId = ZoneId.systemDefault()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedRecordDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate())
                        }
                        isDatePickerVisible = false
                    },
                ) {
                    Text(stringResource(R.string.confirm_date))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
    pendingDeleteRecord?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_record_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecord(pending.record.id) {
                            pendingDeleteRecord = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecord = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }
}

@Composable
private fun RecordDateSelector(
    selectedDate: LocalDate,
    onPreviousDateClick: () -> Unit,
    onNextDateClick: () -> Unit,
    onOpenDatePicker: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy/M/d(E)", Locale.JAPAN) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPreviousDateClick) {
                Text(stringResource(R.string.previous_day))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onOpenDatePicker) {
                    Text(
                        text = dateFormatter.format(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            OutlinedButton(onClick = onNextDateClick) {
                Text(stringResource(R.string.next_day))
            }
        }
    }
}

@Composable
private fun WeeklyChartCard(
    stacks: List<DailyMealStack>,
    maxCalories: Int,
    modifier: Modifier = Modifier,
    onDeleteRecordRequest: (ChartMealDialogState, MealRecord) -> Unit,
) {
    val scrollState = rememberScrollState()
    val resolvedMaxCalories = maxCalories.coerceAtLeast(1)
    var dialogState by remember { mutableStateOf<ChartMealDialogState?>(null) }
    LaunchedEffect(stacks.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LegendRow()
            if (maxCalories == 0) {
                Text(
                    text = stringResource(R.string.weekly_chart_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .testTag("weekly_chart_scroll"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stacks.forEachIndexed { index, stack ->
                    DayStackBar(
                        stack = stack,
                        maxCalories = resolvedMaxCalories,
                        dateLabel = formatChartDateLabel(
                            date = stack.date,
                            previousDate = stacks.getOrNull(index - 1)?.date,
                        ),
                        isSunday = stack.date.dayOfWeek == DayOfWeek.SUNDAY,
                        onSectionClick = { section ->
                            val records = stack.recordsForSection(section)
                            if (records.isNotEmpty()) {
                                dialogState = ChartMealDialogState(
                                    date = stack.date,
                                    section = section,
                                    records = records,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
    dialogState?.let { detail ->
        ChartMealDetailDialog(
            state = detail,
            onDismiss = { dialogState = null },
            onDeleteClick = { record ->
                onDeleteRecordRequest(detail, record)
                dialogState = null
            },
        )
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendItem(label = stringResource(R.string.chart_breakfast), color = BreakfastChartColor)
        LegendItem(label = stringResource(R.string.chart_lunch), color = LunchChartColor)
        LegendItem(label = stringResource(R.string.chart_dinner), color = DinnerChartColor)
        LegendItem(label = stringResource(R.string.chart_snack), color = SnackChartColor)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DayStackBar(
    stack: DailyMealStack,
    maxCalories: Int,
    dateLabel: String,
    isSunday: Boolean,
    onSectionClick: (ChartMealSection) -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("MM/dd", Locale.JAPAN)
    Column(
        modifier = Modifier.width(ChartBarWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = formatChartCalories(stack.totalCalories),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Canvas(
            modifier = Modifier
                .width(24.dp)
                .height(184.dp)
                .pointerInput(stack, maxCalories) {
                    detectTapGestures { offset ->
                        detectTappedMealSection(
                            offsetY = offset.y,
                            height = size.height.toFloat(),
                            stack = stack,
                            maxCalories = maxCalories,
                        )?.let(onSectionClick)
                    }
                }
                .testTag("weekly_chart_bar_${formatter.format(stack.date)}"),
        ) {
            drawBarBackground()
            drawStackSegment(stack.breakfastCalories, maxCalories, BreakfastChartColor)
            drawStackSegment(stack.lunchCalories, maxCalories, LunchChartColor, stack.breakfastCalories)
            drawStackSegment(
                stack.dinnerCalories,
                maxCalories,
                DinnerChartColor,
                stack.breakfastCalories + stack.lunchCalories,
            )
            drawStackSegment(
                stack.snackCalories,
                maxCalories,
                SnackChartColor,
                stack.breakfastCalories + stack.lunchCalories + stack.dinnerCalories,
            )
        }
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (isSunday) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun detectTappedMealSection(
    offsetY: Float,
    height: Float,
    stack: DailyMealStack,
    maxCalories: Int,
): ChartMealSection? {
    if (maxCalories <= 0) return null
    val totalHeight = height
    val breakfastTop = totalHeight - totalHeight * (stack.breakfastCalories.toFloat() / maxCalories.toFloat())
    val lunchTop = breakfastTop - totalHeight * (stack.lunchCalories.toFloat() / maxCalories.toFloat())
    val dinnerTop = lunchTop - totalHeight * (stack.dinnerCalories.toFloat() / maxCalories.toFloat())
    val snackTop = dinnerTop - totalHeight * (stack.snackCalories.toFloat() / maxCalories.toFloat())

    return when {
        stack.snackCalories > 0 && offsetY in snackTop..dinnerTop -> ChartMealSection.SNACK
        stack.dinnerCalories > 0 && offsetY in dinnerTop..lunchTop -> ChartMealSection.DINNER
        stack.lunchCalories > 0 && offsetY in lunchTop..breakfastTop -> ChartMealSection.LUNCH
        stack.breakfastCalories > 0 && offsetY in breakfastTop..totalHeight -> ChartMealSection.BREAKFAST
        else -> null
    }
}

@Composable
private fun ChartMealDetailDialog(
    state: ChartMealDialogState,
    onDismiss: () -> Unit,
    onDeleteClick: (MealRecord) -> Unit,
) {
    val titleDateFormatter = remember { DateTimeFormatter.ofPattern("M/d", Locale.JAPAN) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.JAPAN) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = {
            Text("${titleDateFormatter.format(state.date)} ${stringResource(state.section.labelResId())}")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.records.forEach { record ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = record.templateNameSnapshot,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${timeFormatter.format(Date(record.eatenAt))}  ${stringResource(R.string.kcal_format, record.totalCalories)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (record.selectedOptions.isNotEmpty()) {
                                Text(
                                    text = record.selectedOptions.joinToString(" / ") { option -> option.optionNameSnapshot },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (record.memo.isNotBlank()) {
                                Text(
                                    text = record.memo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteClick(record) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}

private fun DrawScope.drawBarBackground() {
    val chartLeft = size.width * 0.15f
    val chartWidth = size.width * 0.7f
    drawRoundRect(
        color = Color(0x14000000),
        topLeft = Offset(x = chartLeft, y = 0f),
        size = Size(width = chartWidth, height = size.height),
        cornerRadius = CornerRadius(x = 18f, y = 18f),
    )
    repeat(4) { index ->
        val y = size.height - (size.height * ((index + 1) / 4f))
        drawLine(
            color = Color(0x2A000000),
            start = Offset(x = chartLeft, y = y),
            end = Offset(x = chartLeft + chartWidth, y = y),
            strokeWidth = 1.5f,
        )
    }
    drawRoundRect(
        color = Color(0x22000000),
        topLeft = Offset(x = chartLeft, y = 0f),
        size = Size(width = chartWidth, height = size.height),
        cornerRadius = CornerRadius(x = 18f, y = 18f),
        style = Stroke(width = 2f),
    )
}

private fun DrawScope.drawStackSegment(
    calories: Int,
    maxCalories: Int,
    color: Color,
    lowerCalories: Int = 0,
) {
    if (calories <= 0) return
    val chartWidth = size.width * 0.7f
    val chartLeft = size.width * 0.15f
    val segmentHeight = size.height * (calories.toFloat() / maxCalories.toFloat())
    val lowerHeight = size.height * (lowerCalories.toFloat() / maxCalories.toFloat())
    drawRoundRect(
        color = color,
        topLeft = Offset(x = chartLeft, y = size.height - lowerHeight - segmentHeight),
        size = Size(width = chartWidth, height = segmentHeight),
        cornerRadius = CornerRadius(x = 18f, y = 18f),
    )
}

@Composable
private fun RecentRecordsSection(
    records: List<MealRecord>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRecordClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recent_records),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onToggle) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.hide_recent_records else R.string.show_recent_records,
                    ),
                )
            }
        }
        if (expanded) {
            if (records.isEmpty()) {
                Text(text = stringResource(R.string.no_records))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    records.forEach { record ->
                        RecordRow(record = record, onClick = { onRecordClick(record.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSummaryPanel(items: List<SummaryItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            items.chunked(2).forEachIndexed { rowIndex, rowItems ->
                if (rowIndex > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { item ->
                        CompactSummaryItem(
                            title = item.title,
                            value = item.value,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSummaryItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RecordRow(record: MealRecord, onClick: () -> Unit) {
    val formatter = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = record.templateNameSnapshot, style = MaterialTheme.typography.titleMedium)
            Text(text = formatter.format(Date(record.eatenAt)))
            Text(text = stringResource(R.string.kcal_format, record.totalCalories))
        }
    }
}
