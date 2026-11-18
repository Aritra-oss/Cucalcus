package com.example.platform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class PinShortcutOutcome {
    REQUESTED,
    UNSUPPORTED_LAUNCHER,
    MISSING_IMAGE,
    FAILED
}

class CustomIconManager(private val context: Context) {

    companion object {
        private const val ICON_FILE_PREFIX = "custom_app_icon_"
        private const val ICON_SIZE_PX = 512
        private const val SHORTCUT_ID = "cucalcus_custom_gallery_icon"
    }

    suspend fun importIconFromGalleryUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val decoded = decodeSampledBitmapFromUri(uri, 1024)
                ?: return@withContext Result.failure(IllegalStateException("Unable to decode selected icon image"))

            val rotated = applyExifOrientation(uri, decoded)
            val square = centerCropToSquare(rotated, ICON_SIZE_PX)
            if (rotated !== decoded) decoded.recycle()
            if (square !== rotated) rotated.recycle()

            deleteAllCustomIcons()
            val outFile = File(context.filesDir, "${ICON_FILE_PREFIX}${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                square.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            square.recycle()

            Result.success(outFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCustomIcon() = withContext(Dispatchers.IO) {
        deleteAllCustomIcons()
    }

    suspend fun loadPreviewBitmap(path: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pinGalleryShortcutToHome(path: String?): PinShortcutOutcome = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext PinShortcutOutcome.MISSING_IMAGE
        val file = File(path)
        if (!file.exists()) return@withContext PinShortcutOutcome.MISSING_IMAGE

        try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return@withContext PinShortcutOutcome.UNSUPPORTED_LAUNCHER
            }

            val raw = BitmapFactory.decodeFile(file.absolutePath)
                ?: return@withContext PinShortcutOutcome.FAILED

            // Frame inside safe zone for Android adaptive icon masks (66/108 ratio)
            val adaptiveSize = 432
            val innerSize = (adaptiveSize * 0.72f).roundToInt()
            val offset = (adaptiveSize - innerSize) / 2
            val framed = Bitmap.createBitmap(adaptiveSize, adaptiveSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(framed)
            val edgeColor = sampleDominantEdgeColor(raw)
            canvas.drawColor(edgeColor)

            val scaledInner = Bitmap.createScaledBitmap(raw, innerSize, innerSize, true)
            val roundedInner = roundCorners(scaledInner, innerSize * 0.22f)
            canvas.drawBitmap(roundedInner, offset.toFloat(), offset.toFloat(), null)

            roundedInner.recycle()
            if (scaledInner !== raw) scaledInner.recycle()
            raw.recycle()

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                .setShortLabel("Cucalcus")
                .setLongLabel("Cucalcus")
                .setIcon(IconCompat.createWithAdaptiveBitmap(framed))
                .setIntent(launchIntent)
                .build()

            val ok = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            framed.recycle()
            if (ok) PinShortcutOutcome.REQUESTED else PinShortcutOutcome.FAILED
        } catch (_: Throwable) {
            PinShortcutOutcome.FAILED
        }
    }

    private fun deleteAllCustomIcons() {
        context.filesDir.listFiles { file ->
            file.name.startsWith(ICON_FILE_PREFIX)
        }?.forEach { it.delete() }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var inSampleSize = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (max(w, h) > maxDim * 2) {
            w /= 2
            h /= 2
            inSampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
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

    private fun centerCropToSquare(src: Bitmap, targetSize: Int): Bitmap {
        val side = min(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        val cropped = Bitmap.createBitmap(src, x, y, side, side)
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (cropped !== src && cropped !== scaled) {
            cropped.recycle()
        }
        return scaled
    }

    private fun sampleDominantEdgeColor(bitmap: Bitmap): Int {
        val x = (bitmap.width / 2).coerceIn(0, bitmap.width - 1)
        val y = (bitmap.height / 12).coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(x, y)
    }

    private fun roundCorners(src: Bitmap, radiusPx: Float): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, src.width, src.height)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        return output
    }
}
