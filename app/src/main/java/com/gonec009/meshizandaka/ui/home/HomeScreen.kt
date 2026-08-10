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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.gonec009.meshizandaka.data.drive.DriveCalorieSummary
import com.gonec009.meshizandaka.data.drive.DriveExternalCard
import com.gonec009.meshizandaka.data.drive.DriveFood
import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal
import com.gonec009.meshizandaka.data.drive.recordKey
import com.gonec009.meshizandaka.domain.model.DailyMealStack
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import com.gonec009.meshizandaka.ui.common.DriveCachedImage
import com.gonec009.meshizandaka.ui.common.DriveImageMemoryCache
import com.gonec009.meshizandaka.ui.common.MealPhoto
import com.gonec009.meshizandaka.ui.common.MealPhotoMemoryCache
import com.gonec009.meshizandaka.ui.registeredMealLabel
import com.gonec009.meshizandaka.util.formatOneDecimal
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
    MORNING_SNACK,
    LUNCH,
    DINNER,
    DAYTIME_SNACK,
    FREE_SNACK,
}

private data class ChartMealDialogState(
    val date: LocalDate,
    val stack: DailyMealStack,
)

private data class PendingDeleteRecord(
    val record: MealRecord,
)

private data class PendingDeleteRecordPhoto(
    val record: MealRecord,
)

private data class PendingDeleteDrivePlanItem(
    val record: MealRecord,
    val item: DrivePlanItem,
    val itemKey: String,
)

private data class PendingDeleteRecordOption(
    val record: MealRecord,
    val option: MealRecordOption,
)

private data class NutritionTotals(
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
)

private val ChartBarWidth = 39.dp
private val ChartBarHeight = 96.dp
private const val ChartSectionCount = 6
private const val FoodOptionGroupName = "食品"

private fun formatChartCalories(calories: Int): String {
    return "${calories}K"
}

internal fun chartBlockCount(calories: Int): Int = if (calories > 0) 1 else 0

private fun DailyMealStack.recordsForSection(section: ChartMealSection): List<MealRecord> = when (section) {
    ChartMealSection.BREAKFAST -> breakfastRecords
    ChartMealSection.MORNING_SNACK -> morningSnackRecords
    ChartMealSection.LUNCH -> lunchRecords
    ChartMealSection.DINNER -> dinnerRecords
    ChartMealSection.DAYTIME_SNACK -> daytimeSnackRecords
    ChartMealSection.FREE_SNACK -> freeSnackRecords
}

internal fun DailyMealStack.allRecords(): List<MealRecord> {
    return breakfastRecords +
        lunchRecords +
        dinnerRecords +
        morningSnackRecords +
        daytimeSnackRecords +
        freeSnackRecords
}

private fun List<MealRecord>.pfcTotals(): NutritionTotals {
    return NutritionTotals(
        proteinG = sumOf { it.proteinG },
        fatG = sumOf { it.fatG },
        carbG = sumOf { it.carbG },
    )
}

private fun formatPfcSummary(totals: NutritionTotals): String {
    return "P ${formatOneDecimal(totals.proteinG)}g / F ${formatOneDecimal(totals.fatG)}g / C ${formatOneDecimal(totals.carbG)}g"
}

@Composable
private fun MealRecordOptionRows(options: List<MealRecordOption>) {
    options.forEachIndexed { index, option ->
        key(option.id.takeIf { it > 0L } ?: "${option.optionGroupNameSnapshot}:${option.optionNameSnapshot}:$index") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option.optionNameSnapshot,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.kcal_format, option.calorieDelta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
    onReady: () -> Unit,
    onQuickRecordClick: () -> Unit,
    onTemplateManagementClick: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()
    val drivePlanState by container.driveAccessManager.planState.collectAsState()
    val calorieSummary by container.driveAccessManager.calorieSummary.collectAsState()
    LaunchedEffect(state.isInitialized) {
        if (state.isInitialized) onReady()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    HomeScreen(
        innerPadding = innerPadding,
        state = state,
        selectedDrivePlan = drivePlanState.selectedPlan,
        externalCards = drivePlanState.externalCards,
        foods = drivePlanState.foods,
        calorieSummary = calorieSummary,
        onQuickRecordClick = onQuickRecordClick,
        onTemplateManagementClick = onTemplateManagementClick,
        onMoveSelectedDate = viewModel::moveSelectedRecordDate,
        onDateSelected = viewModel::updateSelectedRecordDate,
        onBreakfastClick = viewModel::recordBreakfast,
        onMorningSnackClick = viewModel::recordMorningSnack,
        onLunchClick = viewModel::recordLunch,
        onDinnerClick = viewModel::recordDinner,
        onDaytimeSnackClick = viewModel::recordDaytimeSnack,
        onFreeSnackClick = viewModel::recordFreeSnack,
        onDeleteRecord = viewModel::deleteRecord,
        onDeleteRecordPhoto = viewModel::deleteRecordPhoto,
        onDeleteDrivePlanItem = viewModel::deleteDrivePlanItem,
        onDeleteRecordOption = viewModel::deleteRecordOption,
        onSelectDrivePlanMainDish = viewModel::selectDrivePlanMainDish,
    )
}

@Composable
private fun HomeScreen(
    innerPadding: PaddingValues,
    state: HomeUiState,
    selectedDrivePlan: DrivePlan?,
    externalCards: List<DriveExternalCard>,
    foods: List<DriveFood>,
    calorieSummary: DriveCalorieSummary?,
    onQuickRecordClick: () -> Unit,
    onTemplateManagementClick: () -> Unit,
    onMoveSelectedDate: (Long) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onBreakfastClick: () -> Unit,
    onMorningSnackClick: () -> Unit,
    onLunchClick: () -> Unit,
    onDinnerClick: () -> Unit,
    onDaytimeSnackClick: () -> Unit,
    onFreeSnackClick: () -> Unit,
    onDeleteRecord: (Long, () -> Unit) -> Unit,
    onDeleteRecordPhoto: (Long, () -> Unit) -> Unit,
    onDeleteDrivePlanItem: (Long, String, DrivePlanItem, () -> Unit) -> Unit,
    onDeleteRecordOption: (Long, MealRecordOption, () -> Unit) -> Unit,
    onSelectDrivePlanMainDish: (Long, DrivePlanItem?, DrivePlanItem, String, () -> Unit) -> Unit,
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    var pendingDeleteRecord by remember { mutableStateOf<PendingDeleteRecord?>(null) }
    var pendingDeleteRecordPhoto by remember { mutableStateOf<PendingDeleteRecordPhoto?>(null) }
    var pendingDeleteDrivePlanItem by remember { mutableStateOf<PendingDeleteDrivePlanItem?>(null) }
    var pendingDeleteRecordOption by remember { mutableStateOf<PendingDeleteRecordOption?>(null) }
    val burnedCaloriesTitle = when {
        calorieSummary?.todayKcal == null -> stringResource(R.string.today_burned_calories)
        calorieSummary.calorieDate == LocalDate.now() -> stringResource(R.string.today_burned_calories)
        else -> stringResource(R.string.latest_burned_calories)
    }
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
            title = burnedCaloriesTitle,
            value = calorieSummary?.todayKcal?.let { kcal ->
                stringResource(R.string.kcal_format, kcal)
            } ?: stringResource(R.string.kcal_unavailable),
        ),
        SummaryItem(
            title = stringResource(R.string.average_burned_calories),
            value = calorieSummary?.averageKcal?.let { kcal ->
                stringResource(R.string.kcal_format, kcal)
            } ?: stringResource(R.string.kcal_unavailable),
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onMorningSnackClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.morning_snack_set))
                    }
                    Button(onClick = onDaytimeSnackClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.daytime_snack_set))
                    }
                    Button(onClick = onFreeSnackClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.free_snack_set))
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
                selectedDrivePlan = selectedDrivePlan,
                externalCards = externalCards,
                foods = foods,
                modifier = Modifier.testTag("weekly_chart_card"),
                onDeleteRecordRequest = { _, record ->
                    pendingDeleteRecord = PendingDeleteRecord(
                        record = record,
                    )
                },
                onDeleteDrivePlanItemRequest = { _, record, item, itemKey ->
                    pendingDeleteDrivePlanItem = PendingDeleteDrivePlanItem(
                        record = record,
                        item = item,
                        itemKey = itemKey,
                    )
                },
                onDeleteRecordOptionRequest = { _, record, option ->
                    pendingDeleteRecordOption = PendingDeleteRecordOption(
                        record = record,
                        option = option,
                    )
                },
                onDeleteRecordPhotoRequest = { _, record ->
                    pendingDeleteRecordPhoto = PendingDeleteRecordPhoto(record)
                },
                onSelectDrivePlanMainDish = onSelectDrivePlanMainDish,
            )
        }
        item {
            CompactSummaryPanel(items = summaryItems)
        }
        item {
            Button(onClick = onTemplateManagementClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    selectedDrivePlan?.name?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.go_to_template_management),
                )
            }
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
    pendingDeleteRecordPhoto?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRecordPhoto = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_photo_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecordPhoto(pending.record.id) {
                            pendingDeleteRecordPhoto = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecordPhoto = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }
    pendingDeleteDrivePlanItem?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDeleteDrivePlanItem = null },
            title = { Text(stringResource(R.string.delete)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_meal_item_confirm_message,
                        pending.item.name,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDrivePlanItem(
                            pending.record.id,
                            pending.itemKey,
                            pending.item,
                        ) {
                            pendingDeleteDrivePlanItem = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteDrivePlanItem = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }
    pendingDeleteRecordOption?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRecordOption = null },
            title = { Text(stringResource(R.string.delete)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_meal_item_confirm_message,
                        pending.option.optionNameSnapshot,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecordOption(
                            pending.record.id,
                            pending.option,
                        ) {
                            pendingDeleteRecordOption = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecordOption = null }) {
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
    selectedDrivePlan: DrivePlan?,
    externalCards: List<DriveExternalCard>,
    foods: List<DriveFood>,
    modifier: Modifier = Modifier,
    onDeleteRecordRequest: (ChartMealDialogState, MealRecord) -> Unit,
    onDeleteDrivePlanItemRequest: (ChartMealDialogState, MealRecord, DrivePlanItem, String) -> Unit,
    onDeleteRecordOptionRequest: (ChartMealDialogState, MealRecord, MealRecordOption) -> Unit,
    onDeleteRecordPhotoRequest: (ChartMealDialogState, MealRecord) -> Unit,
    onSelectDrivePlanMainDish: (Long, DrivePlanItem?, DrivePlanItem, String, () -> Unit) -> Unit,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LegendRow()
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
                            if (stack.recordsForSection(section).isNotEmpty()) {
                                dialogState = ChartMealDialogState(
                                    date = stack.date,
                                    stack = stack,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
    val currentDetail = dialogState?.let { detail ->
        stacks.firstOrNull { it.date == detail.date }?.let { stack ->
            detail.copy(stack = stack)
        } ?: detail
    }
    currentDetail?.let { detail ->
        ChartMealDetailDialog(
            state = detail,
            selectedDrivePlan = selectedDrivePlan,
            externalCards = externalCards,
            foods = foods,
            onDismiss = { dialogState = null },
            onDeleteClick = { record ->
                onDeleteRecordRequest(detail, record)
            },
            onDeleteDrivePlanItemClick = { record, item, itemKey ->
                onDeleteDrivePlanItemRequest(detail, record, item, itemKey)
            },
            onDeleteRecordOptionClick = { record, option ->
                onDeleteRecordOptionRequest(detail, record, option)
            },
            onDeleteRecordPhotoClick = { record ->
                onDeleteRecordPhotoRequest(detail, record)
            },
            onSelectDrivePlanMainDish = onSelectDrivePlanMainDish,
        )
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(label = stringResource(R.string.chart_breakfast), color = BreakfastChartColor)
        LegendItem(label = stringResource(R.string.chart_morning_snack), color = MorningSnackChartColor)
        LegendItem(label = stringResource(R.string.chart_lunch), color = LunchChartColor)
        LegendItem(label = stringResource(R.string.chart_daytime_snack), color = DaytimeSnackChartColor)
        LegendItem(label = stringResource(R.string.chart_dinner), color = DinnerChartColor)
        LegendItem(label = stringResource(R.string.chart_free_snack), color = FreeSnackChartColor)
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
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                .height(ChartBarHeight)
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
            drawBarBackground(maxCalories)
            drawStackSegment(stack.breakfastCalories, maxCalories, BreakfastChartColor)
            drawStackSegment(
                stack.morningSnackCalories,
                maxCalories,
                MorningSnackChartColor,
                chartBlockCount(stack.breakfastCalories),
            )
            drawStackSegment(
                stack.lunchCalories,
                maxCalories,
                LunchChartColor,
                chartBlockCount(stack.breakfastCalories) + chartBlockCount(stack.morningSnackCalories),
            )
            drawStackSegment(
                stack.dinnerCalories,
                maxCalories,
                DinnerChartColor,
                chartBlockCount(stack.breakfastCalories) +
                    chartBlockCount(stack.morningSnackCalories) +
                    chartBlockCount(stack.lunchCalories),
            )
            drawStackSegment(
                stack.daytimeSnackCalories,
                maxCalories,
                DaytimeSnackChartColor,
                chartBlockCount(stack.breakfastCalories) +
                    chartBlockCount(stack.morningSnackCalories) +
                    chartBlockCount(stack.lunchCalories) +
                    chartBlockCount(stack.dinnerCalories),
            )
            drawStackSegment(
                stack.freeSnackCalories,
                maxCalories,
                FreeSnackChartColor,
                chartBlockCount(stack.breakfastCalories) +
                    chartBlockCount(stack.morningSnackCalories) +
                    chartBlockCount(stack.lunchCalories) +
                    chartBlockCount(stack.dinnerCalories) +
                    chartBlockCount(stack.daytimeSnackCalories),
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
    val maxBlocks = ChartSectionCount
    val blockGap = 2f
    val blockHeight = ((height - (blockGap * (maxBlocks - 1))) / maxBlocks).coerceAtLeast(1f)
    val blockStride = blockHeight + blockGap
    val distanceFromBottom = height - offsetY
    val positionInStride = distanceFromBottom % blockStride
    if (positionInStride > blockHeight) return null
    val blockIndexFromBottom = (distanceFromBottom / blockStride).toInt()
    if (blockIndexFromBottom < 0 || blockIndexFromBottom >= maxBlocks) return null

    val breakfastBlocks = chartBlockCount(stack.breakfastCalories)
    val morningSnackBlocks = chartBlockCount(stack.morningSnackCalories)
    val lunchBlocks = chartBlockCount(stack.lunchCalories)
    val dinnerBlocks = chartBlockCount(stack.dinnerCalories)
    val daytimeSnackBlocks = chartBlockCount(stack.daytimeSnackCalories)
    val freeSnackBlocks = chartBlockCount(stack.freeSnackCalories)

    return when {
        blockIndexFromBottom < breakfastBlocks -> ChartMealSection.BREAKFAST
        blockIndexFromBottom < breakfastBlocks + morningSnackBlocks -> ChartMealSection.MORNING_SNACK
        blockIndexFromBottom < breakfastBlocks + morningSnackBlocks + lunchBlocks -> ChartMealSection.LUNCH
        blockIndexFromBottom < breakfastBlocks + morningSnackBlocks + lunchBlocks + dinnerBlocks -> ChartMealSection.DINNER
        blockIndexFromBottom < breakfastBlocks + morningSnackBlocks + lunchBlocks + dinnerBlocks + daytimeSnackBlocks -> ChartMealSection.DAYTIME_SNACK
        blockIndexFromBottom < breakfastBlocks + morningSnackBlocks + lunchBlocks + dinnerBlocks + daytimeSnackBlocks + freeSnackBlocks -> ChartMealSection.FREE_SNACK
        else -> null
    }
}

@Composable
private fun ChartMealDetailDialog(
    state: ChartMealDialogState,
    selectedDrivePlan: DrivePlan?,
    externalCards: List<DriveExternalCard>,
    foods: List<DriveFood>,
    onDismiss: () -> Unit,
    onDeleteClick: (MealRecord) -> Unit,
    onDeleteDrivePlanItemClick: (MealRecord, DrivePlanItem, String) -> Unit,
    onDeleteRecordOptionClick: (MealRecord, MealRecordOption) -> Unit,
    onDeleteRecordPhotoClick: (MealRecord) -> Unit,
    onSelectDrivePlanMainDish: (Long, DrivePlanItem?, DrivePlanItem, String, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val titleDateFormatter = remember { DateTimeFormatter.ofPattern("M/d", Locale.JAPAN) }
    val records = remember(state.stack) { state.stack.allRecords() }
    val detailPhotoUris = remember(records) {
        records.mapNotNull { record ->
            record.photoUri?.takeIf { it.isNotBlank() }
        }.distinct()
    }
    val detailImagePaths = remember(records, selectedDrivePlan, externalCards, foods) {
        records.flatMap { record ->
            selectedDrivePlan?.mealForRecord(record)?.imagePaths().orEmpty() +
                record.externalCard(externalCards)?.items.orEmpty().mapNotNull { item -> item.imagePath } +
                record.selectedOptions.mapNotNull { option ->
                    foodForRecordOption(option, foods)?.imagePath
                }
        }.distinct()
    }
    var detailsReady by remember(detailImagePaths, detailPhotoUris) {
        mutableStateOf(detailImagePaths.isEmpty() && detailPhotoUris.isEmpty())
    }
    LaunchedEffect(detailImagePaths, detailPhotoUris) {
        detailsReady = false
        MealPhotoMemoryCache.preload(
            context = context,
            uriStrings = detailPhotoUris,
            maxSizePx = 160,
        )
        DriveImageMemoryCache.preload(detailImagePaths)
        detailsReady = true
    }
    var selectedRecord by remember { mutableStateOf<MealRecord?>(null) }
    val currentSelectedRecord = selectedRecord?.let { selected ->
        records.firstOrNull { record -> record.id == selected.id }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = titleDateFormatter.format(state.date),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.daily_total_calories,
                        state.stack.totalCalories,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MealRecordSection(
                    records = records,
                    total = records.pfcTotals(),
                    detailsReady = detailsReady,
                    onRecordClick = { selectedRecord = it },
                    onDeleteClick = onDeleteClick,
                )
                if (!detailsReady) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.meal_detail_preloading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
    currentSelectedRecord?.let { record ->
        MealRecordContentDialog(
            record = record,
            selectedDrivePlan = selectedDrivePlan,
            externalCards = externalCards,
            foods = foods,
            onDismiss = { selectedRecord = null },
            onDeleteDrivePlanItemClick = onDeleteDrivePlanItemClick,
            onDeleteRecordOptionClick = onDeleteRecordOptionClick,
            onDeleteRecordPhotoClick = onDeleteRecordPhotoClick,
            onSelectMainDishClick = { currentItem, selectedItem, selectedItemKey ->
                onSelectDrivePlanMainDish(
                    record.id,
                    currentItem,
                    selectedItem,
                    selectedItemKey,
                ) {}
            },
        )
    }
}

@Composable
private fun MealRecordSection(
    records: List<MealRecord>,
    total: NutritionTotals,
    detailsReady: Boolean,
    onRecordClick: (MealRecord) -> Unit,
    onDeleteClick: (MealRecord) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPfcSummary(total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (records.isEmpty()) {
            Text(
                text = stringResource(R.string.no_records),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            records.forEachIndexed { index, record ->
                key(record.id.takeIf { it > 0L } ?: "${record.eatenAt}:$index") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = detailsReady) { onRecordClick(record) }
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = registeredMealLabel(record.mealType),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.kcal_format, record.totalCalories),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "${stringResource(R.string.protein_short)} ${formatOneDecimal(record.proteinG)}g / ${stringResource(R.string.fat_short)} ${formatOneDecimal(record.fatG)}g / ${stringResource(R.string.carb_short)} ${formatOneDecimal(record.carbG)}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
        }
    }
}

@Composable
private fun MealRecordContentDialog(
    record: MealRecord,
    selectedDrivePlan: DrivePlan?,
    externalCards: List<DriveExternalCard>,
    foods: List<DriveFood>,
    onDismiss: () -> Unit,
    onDeleteDrivePlanItemClick: (MealRecord, DrivePlanItem, String) -> Unit,
    onDeleteRecordOptionClick: (MealRecord, MealRecordOption) -> Unit,
    onDeleteRecordPhotoClick: (MealRecord) -> Unit,
    onSelectMainDishClick: (DrivePlanItem?, DrivePlanItem, String) -> Unit,
) {
    val timeFormatter = remember { SimpleDateFormat("yyyy/M/d HH:mm", Locale.JAPAN) }
    val planMeal = selectedDrivePlan?.mealForRecord(record)
    val displayOptions = record.displayOptions(externalCards)
    val externalCard = record.externalCard(externalCards)
    val externalOptions = externalCard?.let { card ->
        displayOptions.filter { option -> option.optionGroupNameSnapshot == card.name }
    }.orEmpty()
    val otherOptions = if (externalCard == null) {
        displayOptions
    } else {
        displayOptions.filterNot { option -> option.optionGroupNameSnapshot == externalCard.name }
    }
    val foodOptions = otherOptions.filter { option ->
        option.optionGroupNameSnapshot == FoodOptionGroupName
    }
    val nonFoodOptions = otherOptions.filterNot { option ->
        option.optionGroupNameSnapshot == FoodOptionGroupName
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = {
            Text(registeredMealLabel(record.mealType))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = timeFormatter.format(Date(record.eatenAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.kcal_format, record.totalCalories),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${stringResource(R.string.protein_short)} ${formatOneDecimal(record.proteinG)}g / " +
                        "${stringResource(R.string.fat_short)} ${formatOneDecimal(record.fatG)}g / " +
                        "${stringResource(R.string.carb_short)} ${formatOneDecimal(record.carbG)}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val recordPhotoUri = record.photoUri?.takeIf { it.isNotBlank() }
                val recordPhotoCalories = quickRecordCalories(record, planMeal)
                if (planMeal != null) {
                    DrivePlanMealContent(
                        meal = planMeal,
                        excludedItemKeys = record.excludedDrivePlanItemKeys,
                        record = record,
                        recordPhotoUri = recordPhotoUri,
                        recordPhotoCalories = recordPhotoCalories,
                        onDeleteRecordPhotoClick = { onDeleteRecordPhotoClick(record) },
                        onDeleteItemClick = { item, itemKey ->
                            onDeleteDrivePlanItemClick(record, item, itemKey)
                        },
                        onSelectMainDishClick = onSelectMainDishClick,
                    )
                    if (externalCard != null && externalOptions.isNotEmpty()) {
                        DriveExternalCardContent(
                            card = externalCard,
                            options = externalOptions,
                            onDeleteOptionClick = { option ->
                                onDeleteRecordOptionClick(record, option)
                            },
                        )
                    }
                } else if (externalCard != null && externalOptions.isNotEmpty()) {
                    DriveExternalCardContent(
                        card = externalCard,
                        options = externalOptions,
                        onDeleteOptionClick = { option ->
                            onDeleteRecordOptionClick(record, option)
                        },
                    )
                } else if (recordPhotoUri != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.meal_detail_contents),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        MealRecordPhotoRow(
                            uriString = recordPhotoUri,
                            label = recordPhotoLabel(record.templateNameSnapshot),
                            calories = recordPhotoCalories,
                            onDeleteClick = { onDeleteRecordPhotoClick(record) },
                        )
                    }
                } else if (foodOptions.isEmpty() && nonFoodOptions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.meal_detail_drive_content_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (foodOptions.isNotEmpty()) {
                    MealRecordFoodContent(
                        foods = foods,
                        options = foodOptions,
                        showTitle = planMeal == null && externalOptions.isEmpty() && recordPhotoUri == null,
                        onDeleteOptionClick = { option ->
                            onDeleteRecordOptionClick(record, option)
                        },
                    )
                }
                if (nonFoodOptions.isNotEmpty()) {
                    MealRecordOptionRows(nonFoodOptions)
                }
            }
        },
    )
}

@Composable
private fun DriveExternalCardContent(
    card: DriveExternalCard,
    options: List<MealRecordOption>,
    onDeleteOptionClick: (MealRecordOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.meal_detail_contents),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        options.forEachIndexed { index, option ->
            key(option.id.takeIf { it > 0L } ?: "${option.optionNameSnapshot}:$index") {
                val driveItem = card.items.firstOrNull { item -> item.name == option.optionNameSnapshot }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    driveItem?.imagePath?.let { imagePath ->
                        DriveCachedImage(
                            path = imagePath,
                            contentDescription = option.optionNameSnapshot,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.optionNameSnapshot, fontWeight = FontWeight.SemiBold)
                        driveItem?.amountLabel?.takeIf { it.isNotBlank() }?.let { amountLabel ->
                            Text(
                                text = amountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(R.string.kcal_format, option.calorieDelta),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (option.id > 0L) {
                        IconButton(onClick = { onDeleteOptionClick(option) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_meal_item),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRecordFoodContent(
    foods: List<DriveFood>,
    options: List<MealRecordOption>,
    showTitle: Boolean,
    onDeleteOptionClick: (MealRecordOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.meal_detail_contents),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        options.forEachIndexed { index, option ->
            key(option.id.takeIf { it > 0L } ?: "${option.optionNameSnapshot}:$index") {
                val food = foodForRecordOption(option, foods)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    food?.imagePath?.let { imagePath ->
                        DriveCachedImage(
                            path = imagePath,
                            contentDescription = option.optionNameSnapshot,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.optionNameSnapshot, fontWeight = FontWeight.SemiBold)
                        food?.amountLabel?.takeIf { it.isNotBlank() }?.let { amountLabel ->
                            Text(
                                text = amountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(R.string.kcal_format, option.calorieDelta),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (option.id > 0L) {
                        IconButton(onClick = { onDeleteOptionClick(option) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_meal_item),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrivePlanMealContent(
    meal: DrivePlanMeal,
    excludedItemKeys: Set<String>,
    record: MealRecord,
    recordPhotoUri: String?,
    recordPhotoCalories: Int?,
    onDeleteRecordPhotoClick: () -> Unit,
    onDeleteItemClick: (DrivePlanItem, String) -> Unit,
    onSelectMainDishClick: (DrivePlanItem?, DrivePlanItem, String) -> Unit,
) {
    val itemEntries = meal.items.mapIndexed { index, item -> item to item.recordKey(index) }
    val selectedMainDishKey = record.selectedDrivePlanMainDishItemKey
        ?: itemEntries.firstOrNull { (item, _) -> item.isMainDish }?.second
    val selectedMainDishItem = itemEntries.firstOrNull { (item, itemKey) ->
        itemKey == selectedMainDishKey && itemKey !in excludedItemKeys
    }?.first
    val canSelectMainDish = record.templateId?.let { it < 0L } == true ||
        record.selectedDrivePlanMainDishItemKey != null
    val visibleItems = itemEntries.filterNot { (_, itemKey) -> itemKey in excludedItemKeys }
    val orderedItems = orderDrivePlanMealItems(visibleItems, selectedMainDishKey)
    val hasMultipleMainDishCandidates = visibleItems.count { (item, _) ->
        item.isMainDishCandidate
    } > 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.meal_detail_contents),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (hasMultipleMainDishCandidates) {
                Text(
                    text = stringResource(R.string.meal_detail_multiple_main_dishes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        meal.imagePath?.let { path ->
            DriveCachedImage(
                path = path,
                contentDescription = meal.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
        recordPhotoUri?.let { uriString ->
            MealRecordPhotoRow(
                uriString = uriString,
                label = recordPhotoLabel(record.templateNameSnapshot),
                calories = recordPhotoCalories,
                onDeleteClick = onDeleteRecordPhotoClick,
            )
        }
        if (visibleItems.isEmpty()) {
            Text(
                text = stringResource(R.string.meal_detail_no_items),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        orderedItems.forEach { (item, itemKey) ->
            key(itemKey) {
                val isUnselectedMainDish = item.isMainDishCandidate && itemKey != selectedMainDishKey
                val itemColor = if (isUnselectedMainDish) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isUnselectedMainDish) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.imagePath?.let { path ->
                        DriveCachedImage(
                            path = path,
                            contentDescription = item.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .alpha(if (isUnselectedMainDish) 0.55f else 1f),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, color = itemColor, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = stringResource(R.string.kcal_format, item.calories),
                            style = MaterialTheme.typography.bodySmall,
                            color = itemColor,
                        )
                    }
                    if (isUnselectedMainDish && canSelectMainDish) {
                        TextButton(
                            onClick = {
                                onSelectMainDishClick(selectedMainDishItem, item, itemKey)
                            },
                        ) {
                            Text(stringResource(R.string.select_main_dish))
                        }
                    } else {
                        IconButton(onClick = { onDeleteItemClick(item, itemKey) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_meal_item),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRecordPhotoRow(
    uriString: String,
    label: String?,
    calories: Int?,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MealPhoto(
            uriString = uriString,
            contentDescription = stringResource(R.string.meal_detail_record_photo),
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            maxSizePx = 160,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = label ?: stringResource(R.string.meal_detail_record_photo),
                fontWeight = FontWeight.SemiBold,
            )
            calories?.let { value ->
                Text(
                    text = stringResource(R.string.kcal_format, value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal fun recordPhotoLabel(templateNameSnapshot: String): String? {
    return templateNameSnapshot
        .substringAfterLast(" / ")
        .trim()
        .takeIf { it.isNotBlank() }
}

internal fun MealRecord.externalCard(externalCards: List<DriveExternalCard>): DriveExternalCard? {
    val recordedNames = templateNameSnapshot
        .split(" / ")
        .map(String::trim)
        .filter(String::isNotBlank)
    return externalCards.firstOrNull { card -> card.name in recordedNames }
}

internal fun foodForRecordOption(
    option: MealRecordOption,
    foods: List<DriveFood>,
): DriveFood? {
    if (option.optionGroupNameSnapshot != FoodOptionGroupName) return null
    return foods.firstOrNull { food -> food.name == option.optionNameSnapshot }
}

private fun MealRecord.displayOptions(externalCards: List<DriveExternalCard>): List<MealRecordOption> {
    if (recordPhotoLabel(templateNameSnapshot) == null) return selectedOptions
    val externalCard = externalCard(externalCards)
        ?.takeIf { card -> card.items.isNotEmpty() }
        ?: return selectedOptions
    if (selectedOptions.any { option -> option.optionGroupNameSnapshot == externalCard.name }) {
        return selectedOptions
    }
    return selectedOptions + externalCard.items.map { item ->
        MealRecordOption(
            optionGroupNameSnapshot = externalCard.name,
            optionNameSnapshot = item.name,
            calorieDelta = item.calories,
            proteinDeltaG = item.proteinG,
            fatDeltaG = item.fatG,
            carbDeltaG = item.carbG,
        )
    }
}

internal fun quickRecordCalories(record: MealRecord, planMeal: DrivePlanMeal?): Int? {
    if (planMeal == null || !record.templateNameSnapshot.contains(" / ")) {
        return record.totalCalories
    }
    val itemEntries = planMeal.items.mapIndexed { index, item -> item to item.recordKey(index) }
    val selectedMainDishKey = record.selectedDrivePlanMainDishItemKey
        ?: itemEntries.firstOrNull { (item, _) -> item.isMainDish }?.second
    val mealCalories = itemEntries
        .filterNot { (_, itemKey) -> itemKey in record.excludedDrivePlanItemKeys }
        .filter { (item, itemKey) -> !item.isMainDishCandidate || itemKey == selectedMainDishKey }
        .sumOf { (item, _) -> item.calories }
    return (record.totalCalories - mealCalories).coerceAtLeast(0)
}

internal fun orderDrivePlanMealItems(
    itemEntries: List<Pair<DrivePlanItem, String>>,
    selectedMainDishKey: String?,
): List<Pair<DrivePlanItem, String>> {
    val (selectedItems, remainingItems) = itemEntries.partition { (_, itemKey) ->
        itemKey == selectedMainDishKey
    }
    val (regularItems, unselectedCandidateItems) = remainingItems.partition { (item, _) ->
        !item.isMainDishCandidate
    }
    return selectedItems + regularItems + unselectedCandidateItems
}

internal fun DrivePlan.mealForRecord(record: MealRecord): DrivePlanMeal? {
    if (record.templateId == null) return null
    val slot = when (record.mealType) {
        MealType.BREAKFAST -> 0
        MealType.MORNING_SNACK -> 1
        MealType.LUNCH -> 2
        MealType.DINNER -> 3
        MealType.DAYTIME_SNACK -> 4
        MealType.FREE_SNACK,
        MealType.SNACK,
        -> 5
        MealType.EATING_OUT -> null
    }
    return slot?.let { mealSlot -> meals.firstOrNull { it.slot == mealSlot } }
        ?: meals.firstOrNull { it.name == record.templateNameSnapshot }
}

private fun DrivePlanMeal.imagePaths(): List<String> = buildList {
    imagePath?.let(::add)
    items.mapNotNull { it.imagePath }.forEach(::add)
}

private fun DrawScope.drawBarBackground(maxCalories: Int) {
    val chartLeft = size.width * 0.15f
    val chartWidth = size.width * 0.7f
    drawRoundRect(
        color = Color(0x14000000),
        topLeft = Offset(x = chartLeft, y = 0f),
        size = Size(width = chartWidth, height = size.height),
        cornerRadius = CornerRadius(x = 18f, y = 18f),
    )
    val blockRows = ChartSectionCount
    repeat(blockRows) { index ->
        val y = size.height - (size.height * ((index + 1) / blockRows.toFloat()))
        drawLine(
            color = Color(0x2A000000),
            start = Offset(x = chartLeft, y = y),
            end = Offset(x = chartLeft + chartWidth, y = y),
            strokeWidth = 1f,
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
    lowerBlocks: Int = 0,
) {
    if (calories <= 0) return
    val maxBlocks = ChartSectionCount
    val segmentBlocks = chartBlockCount(calories)
    val chartWidth = size.width * 0.7f
    val chartLeft = size.width * 0.15f
    val blockGap = 2f
    val blockHeight = ((size.height - (blockGap * (maxBlocks - 1))) / maxBlocks).coerceAtLeast(1f)
    val chartTop = size.height - (blockHeight * maxBlocks) - (blockGap * (maxBlocks - 1))
    repeat(segmentBlocks) { index ->
        val blockBottomIndex = lowerBlocks + index
        val top = chartTop + ((maxBlocks - 1 - blockBottomIndex) * (blockHeight + blockGap))
        drawRoundRect(
            color = color,
            topLeft = Offset(x = chartLeft, y = top),
            size = Size(width = chartWidth, height = blockHeight),
            cornerRadius = CornerRadius(x = 4f, y = 4f),
        )
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
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            items.chunked(2).forEachIndexed { rowIndex, rowItems ->
                if (rowIndex > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 5.dp),
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
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
