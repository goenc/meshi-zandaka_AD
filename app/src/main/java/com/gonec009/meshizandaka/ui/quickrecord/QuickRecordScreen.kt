package com.gonec009.meshizandaka.ui.quickrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.ui.AppViewModelFactory

@Composable
fun QuickRecordRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel: QuickRecordViewModel = viewModel(factory = AppViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    QuickRecordScreen(
        innerPadding = innerPadding,
        state = state,
        onTemplateSelect = viewModel::selectTemplate,
        onOptionSelect = viewModel::selectOption,
        onSaveClick = viewModel::saveRecord,
    )
}

@Composable
private fun QuickRecordScreen(
    innerPadding: PaddingValues,
    state: QuickRecordUiState,
    onTemplateSelect: (MealTemplate) -> Unit,
    onOptionSelect: (Long, Long) -> Unit,
    onSaveClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TemplateSection(
                title = stringResource(R.string.standard_meal),
                templates = state.standardTemplates,
                selectedTemplateId = state.selectedTemplate?.id,
                onTemplateSelect = onTemplateSelect,
            )
        }
        item {
            TemplateSection(
                title = stringResource(R.string.special_meal),
                templates = state.specialTemplates,
                selectedTemplateId = state.selectedTemplate?.id,
                onTemplateSelect = onTemplateSelect,
            )
        }
        state.selectedTemplate?.let { template ->
            items(template.optionGroups, key = { it.id }) { group ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(group.options, key = { it.id }) { option ->
                            FilterChip(
                                selected = state.selectedOptionIds[group.id] == option.id,
                                onClick = { onOptionSelect(group.id, option.id) },
                                label = { Text(option.name) },
                            )
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = stringResource(R.string.record_preview), style = MaterialTheme.typography.titleMedium)
                    Text(text = state.selectedTemplate?.name ?: stringResource(R.string.not_selected))
                    Text(text = stringResource(R.string.kcal_format, state.estimatedCalories))
                    Button(
                        onClick = onSaveClick,
                        enabled = state.selectedTemplate != null && !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.record_now))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateSection(
    title: String,
    templates: List<MealTemplate>,
    selectedTemplateId: Long?,
    onTemplateSelect: (MealTemplate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates, key = { it.id }) { template ->
                FilterChip(
                    selected = selectedTemplateId == template.id,
                    onClick = { onTemplateSelect(template) },
                    label = { Text(template.name) },
                )
            }
        }
    }
}
