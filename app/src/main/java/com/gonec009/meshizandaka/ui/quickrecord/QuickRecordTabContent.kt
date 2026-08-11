package com.gonec009.meshizandaka.ui.quickrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.common.MealPhotoWithDeleteAction
import com.gonec009.meshizandaka.ui.mealTypeLabel

@Composable
internal fun ColumnScope.QuickRecordFoodTabContent(
    state: QuickRecordUiState,
    selectedFoodCategoryIndex: Int,
    mealTypeExpanded: Boolean,
    onFoodCategorySelected: (Int) -> Unit,
    onMealTypeExpandedChange: (Boolean) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onFoodRegister: (QuickRecordFood) -> Unit,
) {
    val selectedFoodCategory = quickRecordFoodCategoryTabs[
        selectedFoodCategoryIndex.coerceIn(0, quickRecordFoodCategoryTabs.lastIndex)
    ]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FoodMealTypeSection(
            selectedMealType = mealTypeLabel(state.selectedMealType),
            expanded = mealTypeExpanded,
            onExpandedChange = onMealTypeExpandedChange,
            onMealTypeSelect = { mealType ->
                onMealTypeExpandedChange(false)
                onMealTypeSelect(mealType)
            },
        )
        QuickRecordFoodCategoryTabRow(
            selectedIndex = selectedFoodCategoryIndex.coerceIn(
                0,
                quickRecordFoodCategoryTabs.lastIndex,
            ),
            onSelected = onFoodCategorySelected,
        )
        val visibleFoods = state.foods.filter { food ->
            food.mealCategory == selectedFoodCategory.mealCategory
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (visibleFoods.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.quick_record_no_foods),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(visibleFoods, key = { food -> food.id }) { food ->
                    QuickRecordFoodRow(
                        food = food,
                        enabled = !state.isSaving,
                        onRegister = { onFoodRegister(food) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.QuickRecordEntryTabContent(
    state: QuickRecordUiState,
    isManualTab: Boolean,
    isEatingOutTab: Boolean,
    mealTypeExpanded: Boolean,
    onTemplateSelect: (MealTemplate) -> Unit,
    onDriveEatingOutCardSelect: (QuickRecordDriveCard) -> Unit,
    onTemplateNameChange: (String) -> Unit,
    onMealTypeExpandedChange: (Boolean) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onTotalCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDeletePhotoClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isEatingOutTab) {
            item {
                EatingOutRecordCard(
                    state = state,
                    mealTypeExpanded = mealTypeExpanded,
                    onMealTypeExpandedChange = onMealTypeExpandedChange,
                    onMealTypeSelect = onMealTypeSelect,
                    onSaveClick = onSaveClick,
                )
            }
        }
        if (isManualTab) {
            val normalTemplates = state.availableTemplates.filter { it.mealType != MealType.EATING_OUT }
            if (normalTemplates.isNotEmpty()) item {
                TemplateSection(
                    title = stringResource(R.string.quick_record_templates),
                    templates = normalTemplates,
                    selectedTemplateId = state.selectedTemplate?.id,
                    onTemplateSelect = onTemplateSelect,
                )
            }
        } else if (isEatingOutTab && state.driveEatingOutCards.isNotEmpty()) {
            item {
                DriveEatingOutCardSection(
                    cards = state.driveEatingOutCards,
                    selectedCardId = state.selectedDriveEatingOutCard?.id,
                    onCardSelect = onDriveEatingOutCardSelect,
                )
            }
        } else if (isEatingOutTab) {
            item {
                Text(
                    text = stringResource(R.string.quick_record_no_eating_out_cards),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isEatingOutTab) {
            state.selectedDriveEatingOutCard
                ?.takeIf { it.items.isNotEmpty() }
                ?.let { card ->
                    item {
                        DriveEatingOutItemSection(card = card)
                    }
                }
        }
        if (isManualTab) {
            item {
                ManualRecordCard(
                    state = state,
                    mealTypeExpanded = mealTypeExpanded,
                    onTemplateNameChange = onTemplateNameChange,
                    onMealTypeExpandedChange = onMealTypeExpandedChange,
                    onMealTypeSelect = onMealTypeSelect,
                    onTotalCaloriesChange = onTotalCaloriesChange,
                    onProteinChange = onProteinChange,
                    onFatChange = onFatChange,
                    onCarbChange = onCarbChange,
                    onSaveClick = onSaveClick,
                    onCameraClick = onCameraClick,
                    onDeletePhotoClick = onDeletePhotoClick,
                )
            }
        }
    }
}

@Composable
private fun EatingOutRecordCard(
    state: QuickRecordUiState,
    mealTypeExpanded: Boolean,
    onMealTypeExpandedChange: (Boolean) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onSaveClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MealTypeDropdown(
                selectedMealType = state.selectedMealType,
                expanded = mealTypeExpanded,
                onExpandedChange = onMealTypeExpandedChange,
                onMealTypeSelect = onMealTypeSelect,
            )
            Button(
                onClick = onSaveClick,
                enabled = state.canSave() && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.record_now))
            }
        }
    }
}

@Composable
private fun ManualRecordCard(
    state: QuickRecordUiState,
    mealTypeExpanded: Boolean,
    onTemplateNameChange: (String) -> Unit,
    onMealTypeExpandedChange: (Boolean) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onTotalCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDeletePhotoClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.selectedDriveEatingOutCard == null) {
                CompactOutlinedField(
                    value = state.templateName,
                    onValueChange = onTemplateNameChange,
                    label = { Text(stringResource(R.string.template_name)) },
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    singleLine = true,
                )
            }
            MealTypeDropdown(
                selectedMealType = state.selectedMealType,
                expanded = mealTypeExpanded,
                onExpandedChange = onMealTypeExpandedChange,
                onMealTypeSelect = onMealTypeSelect,
            )
            if (state.selectedDriveEatingOutCard == null) {
                CompactOutlinedField(
                    value = state.totalCalories,
                    onValueChange = onTotalCaloriesChange,
                    label = { Text(stringResource(R.string.base_calories)) },
                    modifier = compactFieldModifier(),
                    textStyle = compactFieldTextStyle(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactMacroField(
                        label = stringResource(R.string.protein_short),
                        value = state.proteinG,
                        onValueChange = onProteinChange,
                        modifier = Modifier.weight(1f),
                    )
                    CompactMacroField(
                        label = stringResource(R.string.fat_short),
                        value = state.fatG,
                        onValueChange = onFatChange,
                        modifier = Modifier.weight(1f),
                    )
                    CompactMacroField(
                        label = stringResource(R.string.carb_short),
                        value = state.carbG,
                        onValueChange = onCarbChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Button(
                onClick = onSaveClick,
                enabled = state.canSave() && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.record_now))
            }
            if (state.selectedDriveEatingOutCard == null) {
                Button(
                    onClick = onCameraClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.take_meal_photo))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    Text(
                        text = if (state.photoUri == null) {
                            stringResource(R.string.meal_photo_not_added)
                        } else {
                            stringResource(R.string.meal_photo_added)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MealPhotoWithDeleteAction(
                    uriString = state.photoUri,
                    contentDescription = stringResource(R.string.meal_photo_added),
                    onDeleteClick = onDeletePhotoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealTypeDropdown(
    selectedMealType: MealType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        LabeledMealSettingField(
            mealTypeLabel = stringResource(R.string.meal_type),
            selectedMealType = mealTypeLabel(selectedMealType),
            expanded = expanded,
            mealFieldModifier = Modifier.menuAnchor(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            MealType.entries
                .filterNot {
                    it == MealType.EATING_OUT || it == MealType.SNACK
                }
                .forEach { mealType ->
                    DropdownMenuItem(
                        text = { Text(mealTypeLabel(mealType)) },
                        onClick = {
                            onExpandedChange(false)
                            onMealTypeSelect(mealType)
                        },
                    )
                }
        }
    }
}
