package com.anote.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

class ImageStore(context: Context) {
    private val filesDir = context.applicationContext.filesDir
    private val cacheDir = context.applicationContext.cacheDir
    private val resolver = context.applicationContext.contentResolver

    fun clearPending() {
        File(cacheDir, PENDING_DIR).deleteRecursively()
    }

    fun fileFor(relativePath: String): File = File(filesDir, relativePath)

    fun deleteRelative(relativePath: String) {
        File(filesDir, relativePath).delete()
    }

    suspend fun importToPending(uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            try {
                compressUriToPending(uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                null
            }
        }
    }

    suspend fun persist(pending: File, entryId: Long): String = withContext(Dispatchers.IO) {
        val dir = File(filesDir, "images/$entryId").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        pending.copyTo(dest, overwrite = true)
        pending.delete()
        dest.relativeTo(filesDir).invariantSeparatorsPath
    }

    private fun pendingDir(): File = File(cacheDir, PENDING_DIR).apply { mkdirs() }

    private fun compressUriToPending(uri: Uri): File {
        val raw = File(pendingDir(), "${UUID.randomUUID()}.src")
        resolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("无法打开图片")
            raw.outputStream().use { output -> input.copyTo(output) }
        }
        try {
            val bitmap = toOpaque(scaleToMaxEdge(decodeOriented(raw), MAX_EDGE))
            val dest = File(pendingDir(), "${UUID.randomUUID()}.jpg")
            dest.outputStream().use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    throw IOException("无法压缩图片")
                }
            }
            bitmap.recycle()
            return dest
        } finally {
            raw.delete()
        }
    }

    private fun decodeOriented(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("无法读取图片")
        }
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
            },
        ) ?: throw IOException("无法读取图片")
        return applyExif(file, decoded)
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxEdge || h / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExif(file: File, bitmap: Bitmap): Bitmap {
        val orientation = ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun toOpaque(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(opaque)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return opaque
    }

    private companion object {
        const val PENDING_DIR = "pending"
        const val MAX_EDGE = 1920
        const val JPEG_QUALITY = 85
    }
}
