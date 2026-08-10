package com.gonec009.meshizandaka.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TARGET_WIDTH = 1024
private const val TARGET_HEIGHT = 576
private const val JPEG_QUALITY = 75
private const val TARGET_MAX_DECODE = 2560

data class ManagedPhotoTarget(
    val file: File,
    val uri: Uri,
)

suspend fun optimizeCapturedPhoto(context: Context, photoUri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val oriented = decodeOrientedBitmap(context, photoUri) ?: return@runCatching photoUri.toString()
        val cropped = cropToLandscape(oriented)
        val scaled = cropped.scale(TARGET_WIDTH, TARGET_HEIGHT)
        context.contentResolver.openOutputStream(photoUri, "w")?.use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        }
        if (scaled !== cropped) scaled.recycle()
        if (cropped !== oriented) cropped.recycle()
        if (!oriented.isRecycled) oriented.recycle()
        photoUri.toString()
    }.getOrElse { photoUri.toString() }
}

suspend fun discardCapturedPhoto(context: Context, photoUri: Uri?) {
    withContext(Dispatchers.IO) {
        if (photoUri == null) return@withContext
        runCatching {
            context.contentResolver.delete(photoUri, null, null)
        }
    }
}

suspend fun discardCapturedPhoto(context: Context, photoUriString: String?) {
    val uri = photoUriString?.takeIf { it.isNotBlank() }?.let { runCatching { it.toUri() }.getOrNull() }
    discardCapturedPhoto(context, uri)
}

fun isManagedPhotoInFolder(photoUriString: String?, folderName: String): Boolean {
    if (photoUriString.isNullOrBlank()) return false
    val normalizedFolder = "/$folderName/"
    return runCatching { photoUriString.toUri() }.getOrNull()?.path?.contains(normalizedFolder) == true
}

fun createManagedPhotoTarget(
    context: Context,
    folderName: String,
    filePrefix: String,
): ManagedPhotoTarget {
    val photoDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), folderName)
    photoDir.mkdirs()
    val photoFile = File(photoDir, "${filePrefix}_${System.currentTimeMillis()}.jpg")
    return ManagedPhotoTarget(
        file = photoFile,
        uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile,
        ),
    )
}

private fun decodeOrientedBitmap(context: Context, photoUri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(photoUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
    }
    val source = context.contentResolver.openInputStream(photoUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    } ?: return null
    val orientation = context.contentResolver.openInputStream(photoUri)?.use { input ->
        ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) return source
    val matrix = Matrix().apply { postRotate(rotation) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    source.recycle()
    return rotated
}

private fun calculateSampleSize(width: Int, height: Int): Int {
    var sampleSize = 1
    var sampledWidth = width
    var sampledHeight = height
    while (sampledWidth / 2 >= TARGET_MAX_DECODE || sampledHeight / 2 >= TARGET_MAX_DECODE) {
        sampleSize *= 2
        sampledWidth /= 2
        sampledHeight /= 2
    }
    return sampleSize
}

private fun cropToLandscape(source: Bitmap): Bitmap {
    val targetAspect = TARGET_WIDTH.toFloat() / TARGET_HEIGHT.toFloat()
    val sourceAspect = source.width.toFloat() / source.height.toFloat()
    return if (sourceAspect > targetAspect) {
        val croppedWidth = (source.height * targetAspect).toInt()
        val offsetX = ((source.width - croppedWidth) / 2).coerceAtLeast(0)
        Bitmap.createBitmap(source, offsetX, 0, croppedWidth, source.height)
    } else {
        val croppedHeight = (source.width / targetAspect).toInt()
        val offsetY = ((source.height - croppedHeight) / 2).coerceAtLeast(0)
        Bitmap.createBitmap(source, 0, offsetY, source.width, croppedHeight)
    }
}
