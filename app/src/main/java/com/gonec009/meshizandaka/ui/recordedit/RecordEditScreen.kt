package com.gonec009.meshizandaka.ui.recordedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import com.gonec009.meshizandaka.ui.AppViewModelFactory

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
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = state.record?.templateNameSnapshot ?: stringResource(R.string.no_records))
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
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save))
        }
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete))
        }
    }
}
