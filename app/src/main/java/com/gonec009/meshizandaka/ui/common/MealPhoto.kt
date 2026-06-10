package com.gonec009.meshizandaka.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MealPhoto(
    uriString: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxSizePx: Int = 720,
) {
    val context = LocalContext.current
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, uriString, maxSizePx) {
        value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(context, uriString, maxSizePx)?.asImageBitmap()
        }
    }
    imageBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun MealPhotoWithDeleteAction(
    uriString: String?,
    contentDescription: String?,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxSizePx: Int = 720,
) {
    Box(modifier = modifier) {
        MealPhoto(
            uriString = uriString,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            maxSizePx = maxSizePx,
        )
        if (!uriString.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(onClick = onDeleteClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "×",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun decodeSampledBitmap(
    context: Context,
    uriString: String?,
    maxSizePx: Int,
): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxSizePx)
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

private fun calculateSampleSize(
    width: Int,
    height: Int,
    maxSizePx: Int,
): Int {
    var sampleSize = 1
    var sampledWidth = width
    var sampledHeight = height
    while (sampledWidth / 2 >= maxSizePx || sampledHeight / 2 >= maxSizePx) {
        sampleSize *= 2
        sampledWidth /= 2
        sampledHeight /= 2
    }
    return sampleSize
}
