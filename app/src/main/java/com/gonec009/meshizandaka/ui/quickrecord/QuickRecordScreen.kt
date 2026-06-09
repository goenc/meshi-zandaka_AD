package com.gonec009.meshizandaka.ui.quickrecord

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import java.io.File

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
        onMealTypeSelect = viewModel::selectMealType,
        onOptionSelect = viewModel::selectOption,
        onPhotoCaptured = viewModel::setPhotoUri,
        onSaveClick = viewModel::saveRecord,
    )
}

@Composable
private fun QuickRecordScreen(
    innerPadding: PaddingValues,
    state: QuickRecordUiState,
    onTemplateSelect: (MealTemplate) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onOptionSelect: (Long, Long) -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    onSaveClick: () -> Unit,
) {
    val context = LocalContext.current
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        onPhotoCaptured(if (captured) pendingPhotoUri?.toString() else null)
        pendingPhotoUri = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            MealTypeSection(
                mealTypes = state.mealTypes,
                selectedMealType = state.selectedMealType,
                onMealTypeSelect = onMealTypeSelect,
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
                    Text(text = mealTypeLabel(state.selectedMealType))
                    Text(text = stringResource(R.string.kcal_format, state.estimatedCalories))
                    OutlinedButton(
                        onClick = {
                            val photoUri = createQuickRecordPhotoUri(context)
                            pendingPhotoUri = photoUri
                            photoLauncher.launch(photoUri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.take_meal_photo))
                    }
                    Text(
                        text = if (state.photoUri == null) {
                            stringResource(R.string.meal_photo_not_added)
                        } else {
                            stringResource(R.string.meal_photo_added)
                        },
                    )
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

private fun createQuickRecordPhotoUri(context: Context): Uri {
    val photoDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "quick_records")
    photoDir.mkdirs()
    val photoFile = File(photoDir, "quick_record_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile,
    )
}

@Composable
private fun MealTypeSection(
    mealTypes: List<MealType>,
    selectedMealType: MealType,
    onMealTypeSelect: (MealType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.meal_type), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(mealTypes, key = { it.name }) { mealType ->
                FilterChip(
                    selected = selectedMealType == mealType,
                    onClick = { onMealTypeSelect(mealType) },
                    label = { Text(mealTypeLabel(mealType)) },
                )
            }
        }
    }
}

@Composable
private fun mealTypeLabel(mealType: MealType): String {
    return when (mealType) {
        MealType.BREAKFAST -> stringResource(R.string.meal_type_breakfast)
        MealType.LUNCH -> stringResource(R.string.meal_type_lunch)
        MealType.DINNER -> stringResource(R.string.meal_type_dinner)
        MealType.SNACK -> stringResource(R.string.meal_type_snack)
        MealType.EATING_OUT -> stringResource(R.string.meal_type_eating_out)
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
