package com.gonec009.meshizandaka.ui.quickrecord

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import com.gonec009.meshizandaka.ui.common.InAppCameraCapture
import com.gonec009.meshizandaka.ui.common.discardCapturedPhoto
import com.gonec009.meshizandaka.ui.common.isManagedPhotoInFolder
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
                QuickRecordFoodTabContent(
                    state = state,
                    selectedFoodCategoryIndex = selectedFoodCategoryIndex,
                    mealTypeExpanded = mealTypeExpanded,
                    onFoodCategorySelected = { selectedFoodCategoryIndex = it },
                    onMealTypeExpandedChange = { mealTypeExpanded = it },
                    onMealTypeSelect = onMealTypeSelect,
                    onFoodRegister = onFoodRegister,
                )
            } else {
                QuickRecordEntryTabContent(
                    state = state,
                    isManualTab = isManualTab,
                    isEatingOutTab = isEatingOutTab,
                    mealTypeExpanded = mealTypeExpanded,
                    onTemplateSelect = onTemplateSelect,
                    onDriveEatingOutCardSelect = onDriveEatingOutCardSelect,
                    onTemplateNameChange = onTemplateNameChange,
                    onMealTypeExpandedChange = { mealTypeExpanded = it },
                    onMealTypeSelect = onMealTypeSelect,
                    onTotalCaloriesChange = onTotalCaloriesChange,
                    onProteinChange = onProteinChange,
                    onFatChange = onFatChange,
                    onCarbChange = onCarbChange,
                    onSaveClick = onSaveClick,
                    onCameraClick = { showCamera = true },
                    onDeletePhotoClick = { showDeletePhotoDialog = true },
                )
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
