package com.gonec009.meshizandaka.ui.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import com.gonec009.meshizandaka.ui.mealTypeLabel

@Composable
fun TemplateManagementRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
) {
    val viewModel: TemplateManagementViewModel = viewModel(factory = AppViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()
    TemplateManagementScreen(
        innerPadding = innerPadding,
        state = state,
        onAddClick = viewModel::openNewDialog,
        onEditClick = viewModel::openEditDialog,
        onCloseDialog = viewModel::closeDialog,
        onUpdateEditor = viewModel::updateEditor,
        onSave = viewModel::saveTemplate,
    )
}

@Composable
private fun TemplateManagementScreen(
    innerPadding: PaddingValues,
    state: TemplateManagementUiState,
    onAddClick: () -> Unit,
    onEditClick: (MealTemplate) -> Unit,
    onCloseDialog: () -> Unit,
    onUpdateEditor: ((TemplateEditorState) -> TemplateEditorState) -> Unit,
    onSave: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_template))
            }
        }
        items(state.templates, key = { it.id }) { template ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditClick(template) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = template.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.kcal_format, template.baseCalories))
                    Text(text = if (template.isSpecial) stringResource(R.string.special_meal) else stringResource(R.string.standard_meal))
                }
            }
        }
    }

    if (state.isDialogOpen) {
        TemplateEditorDialog(
            state = state,
            onCloseDialog = onCloseDialog,
            onUpdateEditor = onUpdateEditor,
            onSave = onSave,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorDialog(
    state: TemplateManagementUiState,
    onCloseDialog: () -> Unit,
    onUpdateEditor: ((TemplateEditorState) -> TemplateEditorState) -> Unit,
    onSave: () -> Unit,
) {
    val editor = state.editorState
    var mealTypeExpanded by remember { mutableStateOf(false) }
    var comparisonExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCloseDialog,
        title = { Text(stringResource(R.string.template_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { onUpdateEditor { current -> current.copy(name = it) } },
                    label = { Text(stringResource(R.string.template_name)) },
                )
                ExposedDropdownMenuBox(
                    expanded = mealTypeExpanded,
                    onExpandedChange = { mealTypeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = mealTypeLabel(editor.mealType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.meal_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) },
                        modifier = Modifier.menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = mealTypeExpanded,
                        onDismissRequest = { mealTypeExpanded = false },
                    ) {
                        MealType.entries.forEach { mealType ->
                            DropdownMenuItem(
                                text = { Text(mealTypeLabel(mealType)) },
                                onClick = {
                                    mealTypeExpanded = false
                                    onUpdateEditor { current -> current.copy(mealType = mealType) }
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = editor.baseCalories,
                    onValueChange = { onUpdateEditor { current -> current.copy(baseCalories = it) } },
                    label = { Text(stringResource(R.string.base_calories)) },
                )
                OutlinedTextField(
                    value = editor.proteinG,
                    onValueChange = { onUpdateEditor { current -> current.copy(proteinG = it) } },
                    label = { Text(stringResource(R.string.protein)) },
                )
                OutlinedTextField(
                    value = editor.fatG,
                    onValueChange = { onUpdateEditor { current -> current.copy(fatG = it) } },
                    label = { Text(stringResource(R.string.fat)) },
                )
                OutlinedTextField(
                    value = editor.carbG,
                    onValueChange = { onUpdateEditor { current -> current.copy(carbG = it) } },
                    label = { Text(stringResource(R.string.carb)) },
                )
                ExposedDropdownMenuBox(
                    expanded = comparisonExpanded,
                    onExpandedChange = { comparisonExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.normalTemplates.firstOrNull { it.id == editor.comparisonTemplateId }?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.comparison_template)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = comparisonExpanded) },
                        modifier = Modifier.menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = comparisonExpanded,
                        onDismissRequest = { comparisonExpanded = false },
                    ) {
                        state.normalTemplates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    comparisonExpanded = false
                                    onUpdateEditor { current -> current.copy(comparisonTemplateId = template.id) }
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = editor.weeklyLimitCount,
                    onValueChange = { onUpdateEditor { current -> current.copy(weeklyLimitCount = it) } },
                    label = { Text(stringResource(R.string.weekly_limit)) },
                )
                OutlinedTextField(
                    value = editor.monthlyLimitCount,
                    onValueChange = { onUpdateEditor { current -> current.copy(monthlyLimitCount = it) } },
                    label = { Text(stringResource(R.string.monthly_limit)) },
                )
                OutlinedTextField(
                    value = editor.memo,
                    onValueChange = { onUpdateEditor { current -> current.copy(memo = it) } },
                    label = { Text(stringResource(R.string.memo)) },
                )
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = editor.isSpecial,
                        onCheckedChange = { checked ->
                            onUpdateEditor { current -> current.copy(isSpecial = checked) }
                        },
                    )
                    Text(stringResource(R.string.special_meal))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCloseDialog) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
