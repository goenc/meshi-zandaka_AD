package com.gonec009.meshizandaka.ui.recordedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import com.gonec009.meshizandaka.ui.common.MealPhotoWithDeleteAction
import com.gonec009.meshizandaka.ui.registeredMealLabel
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun RecordEditRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
    recordId: Long,
    snackbarHostState: SnackbarHostState,
    onDeleteComplete: () -> Unit,
) {
    val viewModel: RecordEditViewModel = viewModel(factory = AppViewModelFactory(container, recordId))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    RecordEditScreen(
        innerPadding = innerPadding,
        state = state,
        onCaloriesChange = viewModel::updateCalories,
        onSpecialChange = viewModel::updateSpecial,
        onMemoChange = viewModel::updateMemo,
        onPhotoDelete = viewModel::clearPhoto,
        onSave = viewModel::save,
        onDelete = { viewModel.delete(onDeleteComplete) },
    )
}

@Composable
private fun RecordEditScreen(
    innerPadding: PaddingValues,
    state: RecordEditUiState,
    onCaloriesChange: (String) -> Unit,
    onSpecialChange: (Boolean) -> Unit,
    onMemoChange: (String) -> Unit,
    onPhotoDelete: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.record?.let { record ->
            Text(text = registeredMealLabel(record.mealType))
        } ?: Text(stringResource(R.string.no_records))
        OutlinedTextField(
            value = state.calories,
            onValueChange = onCaloriesChange,
            label = { Text(stringResource(R.string.total_calories)) },
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = state.isSpecial, onCheckedChange = onSpecialChange)
            Text(stringResource(R.string.special_meal))
        }
        OutlinedTextField(
            value = state.memo,
            onValueChange = onMemoChange,
            label = { Text(stringResource(R.string.memo)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.photoUri.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save))
        }
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete))
        }
    }
    if (showDeletePhotoDialog && !state.photoUri.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showDeletePhotoDialog = false },
            text = { Text(stringResource(R.string.delete_photo_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPhotoDelete()
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
