package com.gonec009.meshizandaka.navigation

import com.gonec009.meshizandaka.R

sealed class AppDestination(val route: String, val titleResId: Int) {
    data object Home : AppDestination("home", R.string.home_title)
    data object QuickRecord : AppDestination("quick_record", R.string.quick_record_title)
    data object Templates : AppDestination("templates", R.string.template_management_title)
    data object Settings : AppDestination("settings", R.string.settings_title)
    data object RecordEdit : AppDestination("record_edit/{recordId}", R.string.record_edit_title) {
        fun route(recordId: Long): String = "record_edit/$recordId"
    }
}
