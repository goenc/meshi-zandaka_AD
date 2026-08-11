package com.gonec009.meshizandaka.ui.startup

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gonec009.meshizandaka.R
import kotlin.math.sin

private const val LOADING_DOT_CYCLE_RADIANS = 6.2831855f
private const val LOADING_DOT_PHASE_OFFSET_RADIANS = LOADING_DOT_CYCLE_RADIANS / 3f
private const val LOADING_DOT_DURATION_MILLIS = 1_200
private const val LOADING_DOT_LIFT_DP = 4f

@Composable
fun StartupLoadingScreen() {
    val loadingLabel = stringResource(R.string.startup_loading)
    val loadingAnimation = rememberInfiniteTransition(label = "startupLoadingDots")
    val loadingPhase by loadingAnimation.animateFloat(
        initialValue = 0f,
        targetValue = LOADING_DOT_CYCLE_RADIANS,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LOADING_DOT_DURATION_MILLIS,
                easing = LinearEasing,
            ),
        ),
        label = "startupLoadingDotPhase",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.loading_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 112.dp)
                .clearAndSetSemantics {
                    contentDescription = loadingLabel
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = loadingLabel,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            repeat(3) { index ->
                val dotLift = maxOf(
                    0f,
                    sin(loadingPhase - index * LOADING_DOT_PHASE_OFFSET_RADIANS),
                ) * LOADING_DOT_LIFT_DP
                Text(
                    text = ".",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-dotLift).dp),
                )
            }
        }
    }
}
