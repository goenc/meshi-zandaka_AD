package com.gonec009.meshizandaka.data.repository

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
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
    private val defaultBreakfastTemplateIdKey = longPreferencesKey("default_breakfast_template_id")
    private val defaultLunchTemplateIdKey = longPreferencesKey("default_lunch_template_id")
    private val defaultDinnerTemplateIdKey = longPreferencesKey("default_dinner_template_id")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
            AppSettings(
                targetCaloriesPerDay = preferences[targetCaloriesKey] ?: 1800,
                maintenanceCaloriesPerDay = preferences[maintenanceCaloriesKey] ?: 2000,
                weekStartsOn = preferences[weekStartsOnKey]?.let(WeekStartDay::valueOf) ?: WeekStartDay.MONDAY,
                defaultBreakfastTemplateId = preferences[defaultBreakfastTemplateIdKey],
                defaultLunchTemplateId = preferences[defaultLunchTemplateIdKey],
                defaultDinnerTemplateId = preferences[defaultDinnerTemplateIdKey],
            )
        }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[targetCaloriesKey] = settings.targetCaloriesPerDay
            preferences[maintenanceCaloriesKey] = settings.maintenanceCaloriesPerDay
            preferences[weekStartsOnKey] = settings.weekStartsOn.name
            if (settings.defaultBreakfastTemplateId == null) {
                preferences.remove(defaultBreakfastTemplateIdKey)
            } else {
                preferences[defaultBreakfastTemplateIdKey] = settings.defaultBreakfastTemplateId
            }
            if (settings.defaultLunchTemplateId == null) {
                preferences.remove(defaultLunchTemplateIdKey)
            } else {
                preferences[defaultLunchTemplateIdKey] = settings.defaultLunchTemplateId
            }
            if (settings.defaultDinnerTemplateId == null) {
                preferences.remove(defaultDinnerTemplateIdKey)
            } else {
                preferences[defaultDinnerTemplateIdKey] = settings.defaultDinnerTemplateId
            }
        }
    }

    suspend fun setDefaultBreakfastTemplateId(templateId: Long?) {
        context.dataStore.edit { preferences ->
            if (templateId == null) {
                preferences.remove(defaultBreakfastTemplateIdKey)
            } else {
                preferences[defaultBreakfastTemplateIdKey] = templateId
            }
        }
    }

    suspend fun setDefaultTemplates(lunchTemplateId: Long?, dinnerTemplateId: Long?) {
        context.dataStore.edit { preferences ->
            if (lunchTemplateId == null) {
                preferences.remove(defaultLunchTemplateIdKey)
            } else {
                preferences[defaultLunchTemplateIdKey] = lunchTemplateId
            }
            if (dinnerTemplateId == null) {
                preferences.remove(defaultDinnerTemplateIdKey)
            } else {
                preferences[defaultDinnerTemplateIdKey] = dinnerTemplateId
            }
        }
    }
}
