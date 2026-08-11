package com.gonec009.meshizandaka.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.data.drive.DriveCalorieSummary
import com.gonec009.meshizandaka.data.drive.DriveExternalCard
import com.gonec009.meshizandaka.data.drive.DriveFood
import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import java.time.LocalDate

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
            HomeRecordActions(
                selectedDate = state.selectedRecordDate,
                onPreviousDateClick = { onMoveSelectedDate(-1L) },
                onNextDateClick = { onMoveSelectedDate(1L) },
                onOpenDatePicker = { isDatePickerVisible = true },
                onBreakfastClick = onBreakfastClick,
                onMorningSnackClick = onMorningSnackClick,
                onLunchClick = onLunchClick,
                onDinnerClick = onDinnerClick,
                onDaytimeSnackClick = onDaytimeSnackClick,
                onFreeSnackClick = onFreeSnackClick,
                onQuickRecordClick = onQuickRecordClick,
            )
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
        HomeDatePickerDialog(
            selectedDate = state.selectedRecordDate,
            onDismissRequest = { isDatePickerVisible = false },
            onDateSelected = onDateSelected,
        )
    }
    HomeDeleteDialogs(
        pendingDeleteRecord = pendingDeleteRecord,
        pendingDeleteRecordPhoto = pendingDeleteRecordPhoto,
        pendingDeleteDrivePlanItem = pendingDeleteDrivePlanItem,
        pendingDeleteRecordOption = pendingDeleteRecordOption,
        onDismissRecord = { pendingDeleteRecord = null },
        onDismissRecordPhoto = { pendingDeleteRecordPhoto = null },
        onDismissDrivePlanItem = { pendingDeleteDrivePlanItem = null },
        onDismissRecordOption = { pendingDeleteRecordOption = null },
        onDeleteRecord = onDeleteRecord,
        onDeleteRecordPhoto = onDeleteRecordPhoto,
        onDeleteDrivePlanItem = onDeleteDrivePlanItem,
        onDeleteRecordOption = onDeleteRecordOption,
    )
}
