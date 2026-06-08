package com.gonec009.meshizandaka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.gonec009.meshizandaka.navigation.MeshiZandakaAppRoot
import com.gonec009.meshizandaka.ui.theme.MeshiZandakaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MeshiZandakaApp
        lifecycleScope.launch {
            app.container.ensureSeedDataUseCase()
        }
        enableEdgeToEdge()
        setContent {
            MeshiZandakaTheme {
                LaunchedEffect(Unit) {
                    app.container.ensureSeedDataUseCase()
                }
                MeshiZandakaAppRoot(app.container)
            }
        }
    }
}
