package com.gonec009.meshizandaka.ui.template

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
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
            val isLockedTemplate = template.shortcutRole != TemplateShortcutRole.NONE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditClick(template) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = template.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.kcal_format, template.baseCalories))
                    if (isLockedTemplate) {
                        Text(text = stringResource(R.string.template_fixed_menu))
                    }
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
    val isLockedTemplate = editor.shortcutRole != TemplateShortcutRole.NONE
    var mealTypeExpanded by remember { mutableStateOf(false) }
    var weeklyLimitExpanded by remember { mutableStateOf(false) }
    var monthlyLimitExpanded by remember { mutableStateOf(false) }
    val weeklyLimitOptions = remember { listOf(null) + (1..7).toList() }
    val monthlyLimitOptions = remember { listOf(null) + (1..7).toList() }

    AlertDialog(
        onDismissRequest = onCloseDialog,
        title = { Text(stringResource(R.string.template_editor_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompactOutlinedField(
                    value = editor.name,
                    onValueChange = { onUpdateEditor { current -> current.copy(name = it) } },
                    label = { Text(stringResource(R.string.template_name)) },
                    enabled = !isLockedTemplate,
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    singleLine = true,
                )
                ExposedDropdownMenuBox(
                    expanded = mealTypeExpanded,
                    onExpandedChange = { if (!isLockedTemplate) mealTypeExpanded = it },
                ) {
                    CompactOutlinedField(
                        value = mealTypeLabel(editor.mealType),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isLockedTemplate,
                        label = { Text(stringResource(R.string.meal_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) },
                        modifier = compactFieldModifier().menuAnchor(),
                        textStyle = compactFieldTextStyle(),
                        singleLine = true,
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
                CompactOutlinedField(
                    value = editor.baseCalories,
                    onValueChange = { onUpdateEditor { current -> current.copy(baseCalories = it) } },
                    label = { Text(stringResource(R.string.base_calories)) },
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    singleLine = true,
                )
                CompactOutlinedField(
                    value = editor.proteinG,
                    onValueChange = { onUpdateEditor { current -> current.copy(proteinG = it) } },
                    label = { Text(stringResource(R.string.protein)) },
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    singleLine = true,
                )
                CompactOutlinedField(
                    value = editor.fatG,
                    onValueChange = { onUpdateEditor { current -> current.copy(fatG = it) } },
                    label = { Text(stringResource(R.string.fat)) },
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    singleLine = true,
                )
                CompactOutlinedField(
                    value = editor.carbG,
                    onValueChange = { onUpdateEditor { current -> current.copy(carbG = it) } },
                    label = { Text(stringResource(R.string.carb)) },
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    singleLine = true,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = weeklyLimitExpanded,
                            onExpandedChange = { weeklyLimitExpanded = it },
                        ) {
                            CompactOutlinedField(
                                value = limitCountLabel(editor.weeklyLimitCount),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.weekly_limit)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weeklyLimitExpanded) },
                                modifier = compactFieldModifier().menuAnchor(),
                                textStyle = compactFieldTextStyle(),
                                singleLine = true,
                            )
                            DropdownMenu(
                                expanded = weeklyLimitExpanded,
                                onDismissRequest = { weeklyLimitExpanded = false },
                            ) {
                                weeklyLimitOptions.forEach { count ->
                                    DropdownMenuItem(
                                        text = { Text(limitCountLabel(count?.toString().orEmpty())) },
                                        onClick = {
                                            weeklyLimitExpanded = false
                                            onUpdateEditor { current ->
                                                current.copy(weeklyLimitCount = count?.toString().orEmpty())
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        ExposedDropdownMenuBox(
                            expanded = monthlyLimitExpanded,
                            onExpandedChange = { monthlyLimitExpanded = it },
                        ) {
                            CompactOutlinedField(
                                value = limitCountLabel(editor.monthlyLimitCount),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.monthly_limit)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthlyLimitExpanded) },
                                modifier = compactFieldModifier().menuAnchor(),
                                textStyle = compactFieldTextStyle(),
                                singleLine = true,
                            )
                            DropdownMenu(
                                expanded = monthlyLimitExpanded,
                                onDismissRequest = { monthlyLimitExpanded = false },
                            ) {
                                monthlyLimitOptions.forEach { count ->
                                    DropdownMenuItem(
                                        text = { Text(limitCountLabel(count?.toString().orEmpty())) },
                                        onClick = {
                                            monthlyLimitExpanded = false
                                            onUpdateEditor { current ->
                                                current.copy(monthlyLimitCount = count?.toString().orEmpty())
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                CompactOutlinedField(
                    value = editor.memo,
                    onValueChange = { onUpdateEditor { current -> current.copy(memo = it) } },
                    label = { Text(stringResource(R.string.memo)) },
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    minLines = 2,
                    maxLines = 4,
                )
                if (isLockedTemplate) {
                    Text(
                        text = stringResource(R.string.template_fixed_menu_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

@Composable
private fun limitCountLabel(value: String): String {
    return value.ifBlank { stringResource(R.string.no_limit) }
}

private fun compactFieldModifier(): Modifier {
    return Modifier
        .fillMaxWidth()
        .heightIn(min = 38.dp)
}

private fun compactFieldTextStyle(): TextStyle {
    return TextStyle(fontSize = 14.sp)
}

@Composable
private fun CompactOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    textStyle: TextStyle = compactFieldTextStyle(),
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .padding(top = 8.dp),
        ) {
            label?.invoke()
        }
        Surface(
            modifier = modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (readOnly) {
                        Text(
                            text = value,
                            style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            fontWeight = FontWeight.Normal,
                        )
                    } else {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            readOnly = false,
                            singleLine = singleLine,
                            minLines = minLines,
                            maxLines = maxLines,
                            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            interactionSource = interactionSource,
                        )
                    }
                }
                trailingIcon?.invoke()
            }
        }
    }
}
