package com.snowsplit.app.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppBitmapCache {
    private const val MIN_CACHE_BYTES = 8L * 1024L * 1024L
    private const val MAX_CACHE_BYTES = 24L * 1024L * 1024L

    private val maxBytes: Int = (Runtime.getRuntime().maxMemory() / 16L)
        .coerceIn(MIN_CACHE_BYTES, MAX_CACHE_BYTES)
        .toInt()

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun cacheKey(uriString: String, maxWidthPx: Int, maxHeightPx: Int): String =
        "$uriString@$maxWidthPx" + "x$maxHeightPx"

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            cache.put(key, bitmap)
        }
    }

    fun trimToHalf() {
        cache.trimToSize(maxBytes / 2)
    }

    fun evictAll() {
        cache.evictAll()
    }
}

fun calculateInSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int,
    reqHeight: Int
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

suspend fun loadSampledBitmapFromUri(
    context: Context,
    uriString: String,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? = withContext(Dispatchers.IO) {
    val key = AppBitmapCache.cacheKey(uriString, reqWidth, reqHeight)
    AppBitmapCache.get(key)?.takeIf { !it.isRecycled }?.let { return@withContext it }

    runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val sampleSize = calculateInSampleSize(
            width = bounds.outWidth.coerceAtLeast(1),
            height = bounds.outHeight.coerceAtLeast(1),
            reqWidth = reqWidth,
            reqHeight = reqHeight
        )
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }?.also { decoded ->
            AppBitmapCache.put(key, decoded)
        }
    }.getOrNull()
}
