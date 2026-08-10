package com.gonec009.meshizandaka.ui.quickrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import com.gonec009.meshizandaka.ui.common.InAppCameraCapture
import com.gonec009.meshizandaka.ui.common.MealPhotoWithDeleteAction
import com.gonec009.meshizandaka.ui.common.discardCapturedPhoto
import com.gonec009.meshizandaka.ui.common.isManagedPhotoInFolder
import com.gonec009.meshizandaka.ui.mealTypeLabel
import kotlinx.coroutines.launch

private const val MANUAL_TAB_INDEX = 0
private const val EATING_OUT_TAB_INDEX = 1
private const val FOOD_TAB_INDEX = 2

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
        onManualTabSelected = viewModel::selectManualTab,
        onFoodTabSelected = viewModel::selectFoodTab,
        onTemplateSelect = viewModel::selectTemplate,
        onDriveEatingOutCardSelect = viewModel::selectDriveEatingOutCard,
        onFoodRegister = viewModel::registerFood,
        onTemplateNameChange = viewModel::setTemplateName,
        onMealTypeSelect = viewModel::selectMealType,
        onTotalCaloriesChange = viewModel::setTotalCalories,
        onProteinChange = viewModel::setProtein,
        onFatChange = viewModel::setFat,
        onCarbChange = viewModel::setCarb,
        onPhotoCaptured = viewModel::setPhotoUri,
        onPhotoRemoved = viewModel::clearPhoto,
        onSaveClick = viewModel::saveRecord,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickRecordScreen(
    innerPadding: PaddingValues,
    state: QuickRecordUiState,
    onManualTabSelected: () -> Unit,
    onFoodTabSelected: () -> Unit,
    onTemplateSelect: (MealTemplate) -> Unit,
    onDriveEatingOutCardSelect: (QuickRecordDriveCard) -> Unit,
    onFoodRegister: (QuickRecordFood) -> Unit,
    onTemplateNameChange: (String) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onTotalCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbChange: (String) -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    onPhotoRemoved: () -> Unit,
    onSaveClick: () -> Unit,
) {
    var showCamera by remember { mutableStateOf(false) }
    var mealTypeExpanded by remember { mutableStateOf(false) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(MANUAL_TAB_INDEX) }
    var selectedFoodCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                listOf(
                    stringResource(R.string.quick_record_tab_manual),
                    stringResource(R.string.quick_record_tab_eating_out),
                    stringResource(R.string.quick_record_tab_food),
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            when (index) {
                                MANUAL_TAB_INDEX -> onManualTabSelected()
                                FOOD_TAB_INDEX -> onFoodTabSelected()
                            }
                        },
                        text = { Text(title) },
                    )
                }
            }
            val isManualTab = selectedTabIndex == MANUAL_TAB_INDEX
            val isEatingOutTab = selectedTabIndex == EATING_OUT_TAB_INDEX
            val isFoodTab = selectedTabIndex == FOOD_TAB_INDEX
            if (isFoodTab) {
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
                        onExpandedChange = { mealTypeExpanded = it },
                        onMealTypeSelect = { mealType ->
                            mealTypeExpanded = false
                            onMealTypeSelect(mealType)
                        },
                    )
                    QuickRecordFoodCategoryTabRow(
                        selectedIndex = selectedFoodCategoryIndex.coerceIn(
                            0,
                            quickRecordFoodCategoryTabs.lastIndex,
                        ),
                        onSelected = { selectedFoodCategoryIndex = it },
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                if (isEatingOutTab) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = mealTypeExpanded,
                                    onExpandedChange = { mealTypeExpanded = it },
                                ) {
                                    LabeledMealSettingField(
                                        mealTypeLabel = stringResource(R.string.meal_type),
                                        selectedMealType = mealTypeLabel(state.selectedMealType),
                                        expanded = mealTypeExpanded,
                                        mealFieldModifier = Modifier.menuAnchor(),
                                    )
                                    DropdownMenu(
                                        expanded = mealTypeExpanded,
                                        onDismissRequest = { mealTypeExpanded = false },
                                    ) {
                                        MealType.entries
                                            .filterNot {
                                                it == MealType.EATING_OUT || it == MealType.SNACK
                                            }
                                            .forEach { mealType ->
                                                DropdownMenuItem(
                                                    text = { Text(mealTypeLabel(mealType)) },
                                                    onClick = {
                                                        mealTypeExpanded = false
                                                        onMealTypeSelect(mealType)
                                                    },
                                                )
                                            }
                                    }
                                }
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
                                DriveEatingOutItemSection(
                                    card = card,
                                )
                            }
                        }
                }
                if (isManualTab) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
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
                                ExposedDropdownMenuBox(
                                    expanded = mealTypeExpanded,
                                    onExpandedChange = { mealTypeExpanded = it },
                                ) {
                                    LabeledMealSettingField(
                                        mealTypeLabel = stringResource(R.string.meal_type),
                                        selectedMealType = mealTypeLabel(state.selectedMealType),
                                        expanded = mealTypeExpanded,
                                        mealFieldModifier = Modifier.menuAnchor(),
                                    )
                                    DropdownMenu(
                                        expanded = mealTypeExpanded,
                                        onDismissRequest = { mealTypeExpanded = false },
                                    ) {
                                        MealType.entries
                                            .filterNot {
                                                it == MealType.EATING_OUT || it == MealType.SNACK
                                            }
                                            .forEach { mealType ->
                                                DropdownMenuItem(
                                                    text = { Text(mealTypeLabel(mealType)) },
                                                    onClick = {
                                                        mealTypeExpanded = false
                                                        onMealTypeSelect(mealType)
                                                    },
                                                )
                                            }
                                    }
                                }
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
                                        onClick = { showCamera = true },
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
                                        onDeleteClick = { showDeletePhotoDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }

        if (showCamera) {
            InAppCameraCapture(
                folderName = "quick_records",
                filePrefix = "quick_record",
                onCaptured = onPhotoCaptured,
                onDismiss = { showCamera = false },
            )
        }
        if (showDeletePhotoDialog && !state.photoUri.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = { showDeletePhotoDialog = false },
                text = { Text(stringResource(R.string.delete_photo_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val currentPhotoUri = state.photoUri
                            if (currentPhotoUri != state.selectedTemplate?.photoUri &&
                                isManagedPhotoInFolder(currentPhotoUri, "quick_records")
                            ) {
                                scope.launch {
                                    discardCapturedPhoto(context, currentPhotoUri)
                                }
                            }
                            onPhotoRemoved()
                            showDeletePhotoDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePhotoDialog = false }) {
                        Text(stringResource(R.string.no))
                    }
                },
            )
        }
    }
}
