package com.gonec009.meshizandaka.ui.recordedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordEditUiState(
    val record: MealRecord? = null,
    val calories: String = "",
    val isSpecial: Boolean = false,
    val memo: String = "",
    val photoUri: String? = null,
    val message: String? = null,
)

class RecordEditViewModel(
    private val container: AppContainer,
    private val recordId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordEditUiState())
    val uiState: StateFlow<RecordEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.mealRecordRepository.observeRecord(recordId).collect { record ->
                _uiState.update {
                    it.copy(
                        record = record,
                        calories = record?.totalCalories?.toString().orEmpty(),
                        isSpecial = record?.isSpecial ?: false,
                        memo = record?.memo.orEmpty(),
                        photoUri = record?.photoUri,
                    )
                }
            }
        }
    }

    fun updateCalories(value: String) {
        _uiState.update { it.copy(calories = value) }
    }

    fun updateSpecial(value: Boolean) {
        _uiState.update { it.copy(isSpecial = value) }
    }

    fun updateMemo(value: String) {
        _uiState.update { it.copy(memo = value) }
    }

    fun clearPhoto() {
        _uiState.update {
            it.copy(
                photoUri = null,
                message = "写真を削除しました",
            )
        }
    }

    fun save() {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            container.mealRecordRepository.updateRecord(
                record.copy(
                    totalCalories = _uiState.value.calories.toIntOrNull() ?: record.totalCalories,
                    isSpecial = _uiState.value.isSpecial,
                    specialDeltaCalories = if (_uiState.value.isSpecial) record.specialDeltaCalories else 0,
                    memo = _uiState.value.memo,
                    photoUri = _uiState.value.photoUri,
                    sourceType = SourceType.MANUAL_EDIT,
                ),
            )
            _uiState.update { it.copy(message = "記録を更新しました") }
        }
    }

    fun delete(onComplete: () -> Unit) {
        viewModelScope.launch {
            container.mealRecordRepository.deleteRecord(recordId)
            onComplete()
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
