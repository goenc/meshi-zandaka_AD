package com.gonec009.meshizandaka.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gonec009.meshizandaka.domain.model.AppSettings
import com.gonec009.meshizandaka.domain.model.WeekStartDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "meshi_zandaka_settings")

class SettingsRepository(private val context: Context) {
    private val targetCaloriesKey = intPreferencesKey("target_calories_per_day")
    private val maintenanceCaloriesKey = intPreferencesKey("maintenance_calories_per_day")
    private val weekStartsOnKey = stringPreferencesKey("week_starts_on")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
            AppSettings(
                targetCaloriesPerDay = preferences[targetCaloriesKey] ?: 1800,
                maintenanceCaloriesPerDay = preferences[maintenanceCaloriesKey] ?: 2000,
                weekStartsOn = preferences[weekStartsOnKey]?.let(WeekStartDay::valueOf) ?: WeekStartDay.MONDAY,
            )
        }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[targetCaloriesKey] = settings.targetCaloriesPerDay
            preferences[maintenanceCaloriesKey] = settings.maintenanceCaloriesPerDay
            preferences[weekStartsOnKey] = settings.weekStartsOn.name
        }
    }
}
