package com.gonec009.meshizandaka

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.data.drive.DriveAccessManager
import com.gonec009.meshizandaka.navigation.MeshiZandakaAppRoot
import com.gonec009.meshizandaka.ui.theme.MeshiZandakaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MeshiZandakaApp
        appContainer = app.container
        authorizationLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                appContainer.driveAccessManager.markAuthorizationFailed()
                return@registerForActivityResult
            }
            val authorizationResult = runCatching {
                Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data)
            }.getOrNull()
            if (authorizationResult == null) {
                appContainer.driveAccessManager.markAuthorizationFailed()
            } else {
                handleAuthorizationResult(authorizationResult)
            }
        }
        lifecycleScope.launch {
            app.container.ensureSeedDataUseCase()
        }
        enableEdgeToEdge()
        setContent {
            MeshiZandakaTheme {
                LaunchedEffect(Unit) {
                    app.container.ensureSeedDataUseCase()
                }
                MeshiZandakaAppRoot(
                    container = app.container,
                    onDriveConnect = ::requestDriveAccess,
                )
            }
        }
    }

    private fun requestDriveAccess() {
        appContainer.driveAccessManager.markAuthorizationStarted()
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(DriveAccessManager.authorizationScopes)
            .build()
        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener(::handleAuthorizationResult)
            .addOnFailureListener {
                appContainer.driveAccessManager.markAuthorizationFailed()
            }
    }

    private fun handleAuthorizationResult(result: AuthorizationResult) {
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
            if (pendingIntent == null) {
                appContainer.driveAccessManager.markAuthorizationFailed()
            } else {
                authorizationLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                )
            }
            return
        }
        val token = result.accessToken
        if (token.isNullOrBlank()) {
            appContainer.driveAccessManager.markAuthorizationFailed()
            return
        }
        lifecycleScope.launch {
            appContainer.driveAccessManager.connect(token)
        }
    }
}
