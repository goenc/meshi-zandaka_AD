package com.gonec009.meshizandaka.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.AppSettings
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.WeekStartDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val targetCaloriesPerDay: String = "",
    val maintenanceCaloriesPerDay: String = "",
    val weekStartsOn: WeekStartDay = WeekStartDay.MONDAY,
    val defaultLunchTemplateId: Long? = null,
    val defaultDinnerTemplateId: Long? = null,
    val normalTemplates: List<MealTemplate> = emptyList(),
    val message: String? = null,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.settingsRepository.settingsFlow,
                container.mealTemplateRepository.observeNormalTemplates(),
            ) { settings, templates -> settings to templates }
                .collect { (settings, templates) ->
                    _uiState.update {
                        it.copy(
                            targetCaloriesPerDay = settings.targetCaloriesPerDay.toString(),
                            maintenanceCaloriesPerDay = settings.maintenanceCaloriesPerDay.toString(),
                            weekStartsOn = settings.weekStartsOn,
                            defaultLunchTemplateId = settings.defaultLunchTemplateId,
                            defaultDinnerTemplateId = settings.defaultDinnerTemplateId,
                            normalTemplates = templates,
                        )
                    }
                }
        }
    }

    fun updateTarget(value: String) {
        _uiState.update { it.copy(targetCaloriesPerDay = value) }
    }

    fun updateMaintenance(value: String) {
        _uiState.update { it.copy(maintenanceCaloriesPerDay = value) }
    }

    fun updateWeekStart(day: WeekStartDay) {
        _uiState.update { it.copy(weekStartsOn = day) }
    }

    fun updateLunchTemplate(id: Long?) {
        _uiState.update { it.copy(defaultLunchTemplateId = id) }
    }

    fun updateDinnerTemplate(id: Long?) {
        _uiState.update { it.copy(defaultDinnerTemplateId = id) }
    }

    fun save() {
        viewModelScope.launch {
            container.settingsRepository.updateSettings(
                AppSettings(
                    targetCaloriesPerDay = _uiState.value.targetCaloriesPerDay.toIntOrNull() ?: 1800,
                    maintenanceCaloriesPerDay = _uiState.value.maintenanceCaloriesPerDay.toIntOrNull() ?: 2000,
                    weekStartsOn = _uiState.value.weekStartsOn,
                    defaultLunchTemplateId = _uiState.value.defaultLunchTemplateId,
                    defaultDinnerTemplateId = _uiState.value.defaultDinnerTemplateId,
                ),
            )
            _uiState.update { it.copy(message = "設定を保存しました") }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
