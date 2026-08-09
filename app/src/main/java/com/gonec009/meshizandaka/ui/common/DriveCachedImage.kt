package com.gonec009.meshizandaka.ui.common

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal object DriveImageMemoryCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun get(path: String): Bitmap? = cache.get(path)

    suspend fun preload(paths: Collection<String>) {
        val uniquePaths = paths.filter { it.isNotBlank() }.distinct()
        coroutineScope {
            uniquePaths.map { path ->
                async(Dispatchers.IO) {
                    load(path)
                }
            }.awaitAll()
        }
    }

    suspend fun load(path: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(path) ?: runCatching { BitmapFactory.decodeFile(path) }
            .getOrNull()
            ?.also { bitmap -> cache.put(path, bitmap) }
    }
}

@Composable
fun DriveCachedImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember {
        mutableStateOf(DriveImageMemoryCache.get(path)?.asImageBitmap())
    }
    LaunchedEffect(path) {
        DriveImageMemoryCache.load(path)
            ?.asImageBitmap()
            ?.let { loadedBitmap -> bitmap = loadedBitmap }
    }
    bitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
