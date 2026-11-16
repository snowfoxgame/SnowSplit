package com.snowsplit.app.data.local.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.snowsplit.app.core.common.AppBitmapCache
import com.snowsplit.app.core.common.DispatcherProvider
import com.snowsplit.app.core.common.calculateInSampleSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

class BackgroundImageLoadException(message: String) : IllegalStateException(message)

@Singleton
class BackgroundImageStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    private val backgroundsDir: File
        get() = File(context.filesDir, "backgrounds").apply { mkdirs() }

    suspend fun importFromUri(sourceUri: Uri): Result<String> =
        importFromUriWithHash(sourceUri).map { it.first }

    suspend fun importFromUriWithHash(sourceUri: Uri): Result<Pair<String, String>> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                } ?: throw BackgroundImageLoadException("Unable to open selected image")

                val sampleSize = calculateInSampleSize(
                    width = bounds.outWidth.coerceAtLeast(1),
                    height = bounds.outHeight.coerceAtLeast(1),
                    reqWidth = MAX_DIMENSION_PX,
                    reqHeight = MAX_DIMENSION_PX
                )
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: throw BackgroundImageLoadException("Unable to decode selected image")

                try {
                    clearStoredImages()
                    val targetFile = File(backgroundsDir, "bg_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(targetFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    }
                    val hash = computeFileSha256(targetFile)
                        ?: throw BackgroundImageLoadException("Unable to hash background image")
                    Uri.fromFile(targetFile).toString() to hash
                } finally {
                    bitmap.recycle()
                }
            }
        }

    suspend fun saveSyncedBackground(inputStream: InputStream): Result<Pair<String, String>> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val tempFile = File(backgroundsDir, "bg_sync_tmp_${System.currentTimeMillis()}")
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    FileOutputStream(tempFile).use { out ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = inputStream.read(buffer)
                            if (read <= 0) break
                            digest.update(buffer, 0, read)
                            out.write(buffer, 0, read)
                        }
                        out.fd.sync()
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(tempFile.absolutePath, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        throw BackgroundImageLoadException("Synced background is not a valid image")
                    }

                    clearStoredImages(exceptFile = tempFile)
                    val targetFile = File(backgroundsDir, "bg_sync_${System.currentTimeMillis()}.img")
                    if (!tempFile.renameTo(targetFile)) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }
                    AppBitmapCache.evictAll()
                    val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                    Uri.fromFile(targetFile).toString() to actualHash
                } catch (throwable: Throwable) {
                    tempFile.delete()
                    throw throwable
                }
            }
        }

    fun resolveManagedBackgroundFile(uriString: String?): File? {
        if (uriString.isNullOrBlank()) return null
        val parsed = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        if (parsed.scheme != "file") return null
        val path = parsed.path ?: return null
        val candidate = File(path)
        val dirCanonical = runCatching { backgroundsDir.canonicalPath }.getOrNull() ?: return null
        val fileCanonical = runCatching { candidate.canonicalPath }.getOrNull() ?: return null
        if (!fileCanonical.startsWith("$dirCanonical${File.separator}")) return null
        return candidate.takeIf { it.exists() && it.isFile }
    }

    fun computeFileSha256(file: File): String? {
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }

    suspend fun clearStoredImages() = withContext(dispatcherProvider.io) {
        clearStoredImages(exceptFile = null)
    }

    private fun clearStoredImages(exceptFile: File?) {
        backgroundsDir.listFiles()?.forEach { file ->
            if (exceptFile == null || file.absolutePath != exceptFile.absolutePath) {
                file.delete()
            }
        }
        AppBitmapCache.evictAll()
    }

    companion object {
        private const val MAX_DIMENSION_PX = 2048
    }
}
