package com.gonec009.meshizandaka.data.drive

import android.content.Context
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveImageCache(
    context: Context,
) {
    private val imageDirectory = File(context.applicationContext.filesDir, "drive_images")

    suspend fun existingPath(contentHash: String): String? = withContext(Dispatchers.IO) {
        fileFor(contentHash).takeIf { it.isFile && it.length() > 0L }?.absolutePath
    }

    suspend fun store(contentHash: String, content: ByteArray): String = withContext(Dispatchers.IO) {
        require(contentHash.isNotBlank()) { "画像ハッシュが空です。" }
        check(content.isNotEmpty()) { "画像データが空です。" }
        if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
            error("Drive画像キャッシュの保存先を作成できません。")
        }

        val destination = fileFor(contentHash)
        val temporary = File(imageDirectory, ".${destination.name}.tmp")
        temporary.outputStream().use { output -> output.write(content) }
        if (destination.exists() && !destination.delete()) {
            temporary.delete()
            error("既存のDrive画像キャッシュを更新できません。")
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            error("Drive画像キャッシュを確定できません。")
        }
        destination.absolutePath
    }

    private fun fileFor(contentHash: String): File {
        val safeName = contentHash
            .lowercase(Locale.ROOT)
            .replace(UNSAFE_FILE_NAME, "_")
        return File(imageDirectory, safeName)
    }

    private companion object {
        val UNSAFE_FILE_NAME = Regex("[^a-z0-9._-]")
    }
}
