package com.gonec009.meshizandaka

import android.app.Application
import com.gonec009.meshizandaka.data.AppContainer

class MeshiZandakaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
