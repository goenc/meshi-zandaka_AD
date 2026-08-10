package com.gonec009.meshizandaka

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import androidx.lifecycle.lifecycleScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.data.drive.DriveAccessManager
import com.gonec009.meshizandaka.data.drive.DriveConnectionPhase
import com.gonec009.meshizandaka.navigation.MeshiZandakaAppRoot
import com.gonec009.meshizandaka.ui.startup.StartupLoadingScreen
import com.gonec009.meshizandaka.ui.theme.MeshiZandakaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val startupLoading = mutableStateOf(true)
    private var homeScreenReady = false
    private var calorieSummaryReady = false
    private var authorizationStartedForStartup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MeshiZandakaApp
        appContainer = app.container
        authorizationLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                appContainer.driveAccessManager.markAuthorizationFailed()
                markCalorieSummaryReady()
                return@registerForActivityResult
            }
            val authorizationResult = runCatching {
                Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data)
            }.getOrNull()
            if (authorizationResult == null) {
                appContainer.driveAccessManager.markAuthorizationFailed()
                markCalorieSummaryReady()
            } else {
                handleAuthorizationResult(authorizationResult)
            }
        }
        enableEdgeToEdge()
        setContent {
            MeshiZandakaTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MeshiZandakaAppRoot(
                        container = app.container,
                        onDriveConnect = ::requestDriveAccess,
                        onHomeReady = ::markHomeScreenReady,
                    )
                    if (startupLoading.value) {
                        StartupLoadingScreen()
                    }
                }
            }
        }
        lifecycleScope.launch {
            delay(STARTUP_LOADING_TIMEOUT_MS)
            finishStartupLoading()
        }
        lifecycleScope.launch {
            app.container.ensureSeedDataUseCase()
            app.container.driveAccessManager.restoreCachedPlans()
            requestStartupDriveAccess()
        }
    }

    private fun requestDriveAccess() {
        beginDriveAccess(forStartup = false)
    }

    private fun requestStartupDriveAccess() {
        beginDriveAccess(forStartup = true)
    }

    private fun beginDriveAccess(forStartup: Boolean) {
        if (appContainer.driveAccessManager.state.value.phase == DriveConnectionPhase.CONNECTING) return
        authorizationStartedForStartup = forStartup
        appContainer.driveAccessManager.markAuthorizationStarted()
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(DriveAccessManager.authorizationScopes)
            .build()
        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener(::handleAuthorizationResult)
            .addOnFailureListener {
                appContainer.driveAccessManager.markAuthorizationFailed()
                markCalorieSummaryReady()
            }
    }

    private fun handleAuthorizationResult(result: AuthorizationResult) {
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
            if (pendingIntent == null) {
                appContainer.driveAccessManager.markAuthorizationFailed()
                markCalorieSummaryReady()
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
            markCalorieSummaryReady()
            return
        }
        val isStartupRequest = authorizationStartedForStartup
        lifecycleScope.launch {
            val calorieSummary = appContainer.driveAccessManager.fetchCalorieSummary(token)
            if (!isStartupRequest || startupLoading.value) {
                appContainer.driveAccessManager.publishCalorieSummary(calorieSummary)
            }
            if (isStartupRequest) markCalorieSummaryReady()
            appContainer.driveAccessManager.connect(token)
        }
    }

    private fun markHomeScreenReady() {
        homeScreenReady = true
        finishStartupLoadingIfReady()
    }

    private fun markCalorieSummaryReady() {
        if (calorieSummaryReady) return
        lifecycleScope.launch {
            delay(HOME_STABILIZATION_DELAY_MS)
            calorieSummaryReady = true
            finishStartupLoadingIfReady()
        }
    }

    private fun finishStartupLoadingIfReady() {
        if (homeScreenReady && calorieSummaryReady) {
            finishStartupLoading()
        }
    }

    private fun finishStartupLoading() {
        startupLoading.value = false
    }

    companion object {
        private const val STARTUP_LOADING_TIMEOUT_MS = 10_000L
        private const val HOME_STABILIZATION_DELAY_MS = 100L
    }
}
