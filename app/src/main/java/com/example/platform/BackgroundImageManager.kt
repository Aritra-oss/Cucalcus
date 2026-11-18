package com.example.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class BackgroundImageManager(private val context: Context) {

    companion object {
        private const val BG_FILE_PREFIX = "custom_app_bg_"
        private const val MAX_STORAGE_DIM = 1280
        private const val MAX_BLUR_SOURCE_DIM = 360
        private const val LIGHT_BLUR_RADIUS = 6
    }

    suspend fun importFromGalleryUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val decoded = decodeSampledBitmapFromUri(uri, MAX_STORAGE_DIM)
                ?: return@withContext Result.failure(IllegalStateException("Unable to decode selected image"))

            val rotated = applyExifOrientation(uri, decoded)
            val scaled = scaleDownIfNeeded(rotated, MAX_STORAGE_DIM)
            if (scaled !== rotated && rotated !== decoded) {
                rotated.recycle()
            }
            if (scaled !== decoded) {
                decoded.recycle()
            }

            deleteAllBackgroundFiles()
            val outFile = File(context.filesDir, "${BG_FILE_PREFIX}${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            scaled.recycle()

            Result.success(outFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearBackgroundImage() = withContext(Dispatchers.IO) {
        deleteAllBackgroundFiles()
    }

    suspend fun loadLightlyBlurredBitmap(path: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_BLUR_SOURCE_DIM)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@withContext null
            val small = scaleDownIfNeeded(decoded, MAX_BLUR_SOURCE_DIM)
            if (small !== decoded) decoded.recycle()

            val blurred = fastSeparableBlur(small, LIGHT_BLUR_RADIUS)
            if (blurred !== small) small.recycle()
            blurred
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteAllBackgroundFiles() {
        context.filesDir.listFiles { file ->
            file.name.startsWith(BG_FILE_PREFIX)
        }?.forEach { it.delete() }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val largest = max(w, h)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val newW = (w * ratio).roundToInt().coerceAtLeast(1)
        val newH = (h * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqMaxDim: Int): Int {
        var inSampleSize = 1
        var w = width
        var h = height
        while (max(w, h) > reqMaxDim * 2) {
            w /= 2
            h /= 2
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /**
     * Two-pass separable box blur producing a subtle frosted look on all API levels (24+).
     */
    private fun fastSeparableBlur(src: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return src
        val w = src.width
        val h = src.height
        if (w <= 1 || h <= 1) return src

        var pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        var temp = IntArray(w * h)

        repeat(2) {
            boxBlurHorizontal(pixels, temp, w, h, radius)
            boxBlurVertical(temp, pixels, w, h, radius)
        }

        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val window = r * 2 + 1
        for (y in 0 until h) {
            val rowOffset = y * w
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (dx in -r..r) {
                val px = src[rowOffset + dx.coerceIn(0, w - 1)]
                rSum += (px shr 16) and 0xFF
                gSum += (px shr 8) and 0xFF
                bSum += px and 0xFF
            }

            for (x in 0 until w) {
                dst[rowOffset + x] = (0xFF shl 24) or
                    ((rSum / window) shl 16) or
                    ((gSum / window) shl 8) or
                    (bSum / window)

                val removeIdx = (x - r).coerceIn(0, w - 1)
                val addIdx = (x + r + 1).coerceIn(0, w - 1)
                val pOut = src[rowOffset + removeIdx]
                val pIn = src[rowOffset + addIdx]

                rSum += ((pIn shr 16) and 0xFF) - ((pOut shr 16) and 0xFF)
                gSum += ((pIn shr 8) and 0xFF) - ((pOut shr 8) and 0xFF)
                bSum += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val window = r * 2 + 1
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (dy in -r..r) {
                val px = src[dy.coerceIn(0, h - 1) * w + x]
                rSum += (px shr 16) and 0xFF
                gSum += (px shr 8) and 0xFF
                bSum += px and 0xFF
            }

            for (y in 0 until h) {
                dst[y * w + x] = (0xFF shl 24) or
                    ((rSum / window) shl 16) or
                    ((gSum / window) shl 8) or
                    (bSum / window)

                val removeIdx = (y - r).coerceIn(0, h - 1)
                val addIdx = (y + r + 1).coerceIn(0, h - 1)
                val pOut = src[removeIdx * w + x]
                val pIn = src[addIdx * w + x]

                rSum += ((pIn shr 16) and 0xFF) - ((pOut shr 16) and 0xFF)
                gSum += ((pIn shr 8) and 0xFF) - ((pOut shr 8) and 0xFF)
                bSum += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    }
}
