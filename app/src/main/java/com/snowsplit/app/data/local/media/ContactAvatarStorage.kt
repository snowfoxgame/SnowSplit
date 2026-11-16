package com.snowsplit.app.data.local.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.snowsplit.app.core.common.DispatcherProvider
import com.snowsplit.app.core.common.calculateInSampleSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class ContactAvatarLoadException(message: String) : IllegalStateException(message)

/**
 * Copies selected gallery photos for contact/"Me" bubbles into internal `filesDir/avatars/`
 * so Photo Picker URI grants never expire across app restarts.
 */
@Singleton
class ContactAvatarStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    private val avatarsDir: File
        get() = File(context.filesDir, "avatars").apply { mkdirs() }

    suspend fun importFromUri(
        sourceUri: Uri,
        previousLocalUri: String? = null
    ): Result<String> = withContext(dispatcherProvider.io) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: throw ContactAvatarLoadException("Unable to open selected avatar image")

            val sampleSize = calculateInSampleSize(
                width = bounds.outWidth.coerceAtLeast(1),
                height = bounds.outHeight.coerceAtLeast(1),
                reqWidth = MAX_DIMENSION_PX,
                reqHeight = MAX_DIMENSION_PX
            )
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: throw ContactAvatarLoadException("Unable to decode selected avatar image")

            try {
                val targetFile = File(avatarsDir, "avatar_${UUID.randomUUID()}.jpg")
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                deleteLocalAvatarIfManaged(previousLocalUri)
                Uri.fromFile(targetFile).toString()
            } finally {
                bitmap.recycle()
            }
        }
    }

    suspend fun deleteLocalAvatarIfManaged(uriString: String?) = withContext(dispatcherProvider.io) {
        if (uriString.isNullOrBlank()) return@withContext
        runCatching {
            val parsed = Uri.parse(uriString)
            if (parsed.scheme == "file") {
                val file = File(parsed.path ?: return@runCatching)
                if (file.parentFile?.absolutePath == avatarsDir.absolutePath && file.exists()) {
                    file.delete()
                }
            }
        }
    }

    companion object {
        private const val MAX_DIMENSION_PX = 512
    }
}
