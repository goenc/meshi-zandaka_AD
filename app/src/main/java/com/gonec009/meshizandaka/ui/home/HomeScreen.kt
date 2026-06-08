package com.gonec009.meshizandaka.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.SnackbarHostState
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onQuickRecordClick: () -> Unit,
    onTemplateManagementClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
        onTemplateManagementClick = onTemplateManagementClick,
        onSettingsClick = onSettingsClick,
        onBreakfastClick = viewModel::recordBreakfast,
        onLunchClick = viewModel::recordLunch,
        onDinnerClick = viewModel::recordDinner,
        onRecordClick = onRecordClick,
    )
}

@Composable
private fun HomeScreen(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onQuickRecordClick: () -> Unit,
    onTemplateManagementClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBreakfastClick: () -> Unit,
    onLunchClick: () -> Unit,
    onDinnerClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBreakfastClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.breakfast_set))
                }
                Button(onClick = onLunchClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lunch_set))
                }
                Button(onClick = onDinnerClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dinner_set))
                }
                Button(onClick = onQuickRecordClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.go_to_record))
                }
                Button(onClick = onTemplateManagementClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.go_to_template_management))
                }
                Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }
        }
        item {
            SummaryCard(
                title = stringResource(R.string.today_consumed),
                value = stringResource(R.string.kcal_format, state.summary.todayConsumedCalories),
            )
        }
        item {
            SummaryCard(
                title = stringResource(R.string.today_balance),
                value = stringResource(R.string.kcal_format_signed, state.summary.todayBalanceCalories),
            )
        }
        item {
            SummaryCard(
                title = stringResource(R.string.week_balance),
                value = stringResource(R.string.kcal_format_signed, state.summary.weekBalanceCalories),
            )
        }
        item {
            SummaryCard(
                title = stringResource(R.string.month_balance),
                value = stringResource(R.string.kcal_format_signed, state.summary.monthBalanceCalories),
            )
        }
        item {
            SummaryCard(
                title = stringResource(R.string.month_special_count),
                value = stringResource(R.string.count_format, state.summary.monthSpecialCount),
            )
        }
        item {
            SummaryCard(
                title = stringResource(R.string.month_special_delta),
                value = stringResource(R.string.kcal_format_signed, state.summary.monthSpecialDeltaCalories),
            )
        }
        item {
            Text(
                text = stringResource(R.string.recent_records),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (state.recentRecords.isEmpty()) {
            item {
                Text(text = stringResource(R.string.no_records))
            }
        } else {
            items(state.recentRecords, key = MealRecord::id) { record ->
                RecordRow(record = record, onClick = { onRecordClick(record.id) })
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
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
