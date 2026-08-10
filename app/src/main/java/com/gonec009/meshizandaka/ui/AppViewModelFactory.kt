package com.gonec009.meshizandaka.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.ui.home.HomeViewModel
import com.gonec009.meshizandaka.ui.quickrecord.QuickRecordViewModel
import com.gonec009.meshizandaka.ui.recordedit.RecordEditViewModel
import com.gonec009.meshizandaka.ui.settings.SettingsViewModel

class AppViewModelFactory(
    private val container: AppContainer,
    private val recordId: Long? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container) as T
            modelClass.isAssignableFrom(QuickRecordViewModel::class.java) -> QuickRecordViewModel(container) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container) as T
            modelClass.isAssignableFrom(RecordEditViewModel::class.java) -> RecordEditViewModel(container, requireNotNull(recordId)) as T
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
