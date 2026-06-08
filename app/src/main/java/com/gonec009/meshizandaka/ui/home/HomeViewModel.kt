package com.gonec009.meshizandaka.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.AppSettings
import com.gonec009.meshizandaka.domain.model.DashboardSummary
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val recentRecords: List<MealRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val templates: List<com.gonec009.meshizandaka.domain.model.MealTemplate> = emptyList(),
    val message: String? = null,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.settingsRepository.settingsFlow,
                container.mealTemplateRepository.observeActiveTemplates(),
                container.observeDashboardUseCase(),
            ) { settings, templates, dashboard -> Triple(settings, templates, dashboard) }
                .collect { (settings, templates, dashboard) ->
                    val (summary, recentRecords) = dashboard
                    _uiState.update {
                        it.copy(
                            summary = summary,
                            recentRecords = recentRecords,
                            settings = settings,
                            templates = templates,
                        )
                    }
                }
        }
    }

    fun recordBreakfast() {
        recordTemplate(resolveTemplateId(TemplateShortcutRole.BREAKFAST), "朝セットを記録しました")
    }

    fun recordLunch() {
        recordTemplate(resolveTemplateId(TemplateShortcutRole.LUNCH), "昼セットを記録しました")
    }

    fun recordDinner() {
        recordTemplate(resolveTemplateId(TemplateShortcutRole.DINNER), "夕セットを記録しました")
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun recordTemplate(templateId: Long?, successMessage: String) {
        if (templateId == null) {
            _uiState.update { it.copy(message = "設定でテンプレートを選んでください") }
            return
        }
        viewModelScope.launch {
            container.createQuickRecordUseCase(templateId, emptyList())
            _uiState.update {
                it.copy(message = successMessage)
            }
        }
    }

    private fun resolveTemplateId(role: TemplateShortcutRole): Long? {
        return _uiState.value.templates.firstOrNull { it.shortcutRole == role }?.id
            ?: when (role) {
                TemplateShortcutRole.BREAKFAST -> _uiState.value.settings.defaultBreakfastTemplateId
                TemplateShortcutRole.LUNCH -> _uiState.value.settings.defaultLunchTemplateId
                TemplateShortcutRole.DINNER -> _uiState.value.settings.defaultDinnerTemplateId
                TemplateShortcutRole.NONE -> null
            }
    }
}
