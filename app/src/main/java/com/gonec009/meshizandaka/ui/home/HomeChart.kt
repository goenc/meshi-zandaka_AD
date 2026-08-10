package com.gonec009.meshizandaka.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.drive.DriveExternalCard
import com.gonec009.meshizandaka.data.drive.DriveFood
import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.domain.model.DailyMealStack
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.ui.common.DriveImageMemoryCache
import com.gonec009.meshizandaka.ui.common.MealPhotoMemoryCache
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ChartMealSection {
    BREAKFAST,
    MORNING_SNACK,
    LUNCH,
    DINNER,
    DAYTIME_SNACK,
    FREE_SNACK,
}

internal data class ChartMealDialogState(
    val date: LocalDate,
    val stack: DailyMealStack,
)

private val ChartBarWidth = 39.dp
private val ChartBarHeight = 96.dp
private const val ChartSectionCount = 6

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
internal fun WeeklyChartCard(
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
            drawBarBackground()
            drawStackSegment(stack.breakfastCalories, BreakfastChartColor)
            drawStackSegment(
                stack.morningSnackCalories,
                MorningSnackChartColor,
                chartBlockCount(stack.breakfastCalories),
            )
            drawStackSegment(
                stack.lunchCalories,
                LunchChartColor,
                chartBlockCount(stack.breakfastCalories) + chartBlockCount(stack.morningSnackCalories),
            )
            drawStackSegment(
                stack.dinnerCalories,
                DinnerChartColor,
                chartBlockCount(stack.breakfastCalories) +
                    chartBlockCount(stack.morningSnackCalories) +
                    chartBlockCount(stack.lunchCalories),
            )
            drawStackSegment(
                stack.daytimeSnackCalories,
                DaytimeSnackChartColor,
                chartBlockCount(stack.breakfastCalories) +
                    chartBlockCount(stack.morningSnackCalories) +
                    chartBlockCount(stack.lunchCalories) +
                    chartBlockCount(stack.dinnerCalories),
            )
            drawStackSegment(
                stack.freeSnackCalories,
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

private fun DrawScope.drawBarBackground() {
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
