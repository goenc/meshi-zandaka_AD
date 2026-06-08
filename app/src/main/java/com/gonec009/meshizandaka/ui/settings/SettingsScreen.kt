package com.gonec009.meshizandaka.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.WeekStartDay
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import com.gonec009.meshizandaka.ui.weekStartDayLabel

@Composable
fun SettingsRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    SettingsScreen(
        innerPadding = innerPadding,
        state = state,
        onTargetChange = viewModel::updateTarget,
        onMaintenanceChange = viewModel::updateMaintenance,
        onWeekStartChange = viewModel::updateWeekStart,
        onLunchTemplateChange = viewModel::updateLunchTemplate,
        onDinnerTemplateChange = viewModel::updateDinnerTemplate,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    innerPadding: PaddingValues,
    state: SettingsUiState,
    onTargetChange: (String) -> Unit,
    onMaintenanceChange: (String) -> Unit,
    onWeekStartChange: (WeekStartDay) -> Unit,
    onLunchTemplateChange: (Long?) -> Unit,
    onDinnerTemplateChange: (Long?) -> Unit,
    onSave: () -> Unit,
) {
    var weekStartExpanded by remember { mutableStateOf(false) }
    var lunchExpanded by remember { mutableStateOf(false) }
    var dinnerExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.targetCaloriesPerDay,
            onValueChange = onTargetChange,
            label = { Text(stringResource(R.string.target_calories)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.maintenanceCaloriesPerDay,
            onValueChange = onMaintenanceChange,
            label = { Text(stringResource(R.string.maintenance_calories)) },
            modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(
            expanded = weekStartExpanded,
            onExpandedChange = { weekStartExpanded = it },
        ) {
            OutlinedTextField(
                value = weekStartDayLabel(state.weekStartsOn),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.week_start_day)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekStartExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            DropdownMenu(
                expanded = weekStartExpanded,
                onDismissRequest = { weekStartExpanded = false },
            ) {
                WeekStartDay.entries.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(weekStartDayLabel(day)) },
                        onClick = {
                            weekStartExpanded = false
                            onWeekStartChange(day)
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = lunchExpanded,
            onExpandedChange = { lunchExpanded = it },
        ) {
            OutlinedTextField(
                value = state.normalTemplates.firstOrNull { it.id == state.defaultLunchTemplateId }?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.default_lunch_template)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lunchExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            DropdownMenu(
                expanded = lunchExpanded,
                onDismissRequest = { lunchExpanded = false },
            ) {
                state.normalTemplates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.name) },
                        onClick = {
                            lunchExpanded = false
                            onLunchTemplateChange(template.id)
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = dinnerExpanded,
            onExpandedChange = { dinnerExpanded = it },
        ) {
            OutlinedTextField(
                value = state.normalTemplates.firstOrNull { it.id == state.defaultDinnerTemplateId }?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.default_dinner_template)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dinnerExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            DropdownMenu(
                expanded = dinnerExpanded,
                onDismissRequest = { dinnerExpanded = false },
            ) {
                state.normalTemplates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.name) },
                        onClick = {
                            dinnerExpanded = false
                            onDinnerTemplateChange(template.id)
                        },
                    )
                }
            }
        }
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save))
        }
    }
}
