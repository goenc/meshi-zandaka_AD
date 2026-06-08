package com.gonec009.meshizandaka.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.DashboardSummary
import com.gonec009.meshizandaka.domain.model.MealRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val recentRecords: List<MealRecord> = emptyList(),
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.observeDashboardUseCase().collect { (summary, recentRecords) ->
                _uiState.update { it.copy(summary = summary, recentRecords = recentRecords) }
            }
        }
    }
}
