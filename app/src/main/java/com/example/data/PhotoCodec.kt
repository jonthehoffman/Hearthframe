package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EncodedPhoto(
  val imageBase64: String,
  val aspectRatio: Double,
  val takenAtMillis: Long,
  val dayLabel: String,
)

object PhotoCodec {
  private const val MAX_DIMENSION_PX = 1080
  private const val MAX_JPEG_BYTES = 450_000
  private const val MAX_BASE64_CHARS = 800_000

  private val bitmapCache =
    object : LruCache<String, ImageBitmap>(48) {}

  suspend fun decodeForDisplay(photoId: String, imageBase64: String): ImageBitmap? {
    if (photoId.isNotBlank()) {
      bitmapCache.get(photoId)?.let { return it }
    }
    if (imageBase64.isBlank()) return null
    return withContext(Dispatchers.Default) {
      try {
        val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        val imageBitmap = bitmap.asImageBitmap()
        if (photoId.isNotBlank()) {
          bitmapCache.put(photoId, imageBitmap)
        }
        imageBitmap
      } catch (_: Exception) {
        null
      }
    }
  }

  suspend fun compressAndEncode(context: Context, uri: Uri): Result<EncodedPhoto> =
    withContext(Dispatchers.IO) {
      try {
        val resolver = context.applicationContext.contentResolver

        // 1. Read EXIF orientation & capture timestamp if present
        var orientation = ExifInterface.ORIENTATION_NORMAL
        var takenAtMillis = System.currentTimeMillis()
        resolver.openInputStream(uri)?.use { exifStream ->
          try {
            val exif = ExifInterface(exifStream)
            orientation =
              exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
              )
            val dateStr =
              exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (!dateStr.isNullOrBlank()) {
              val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
              parser.parse(dateStr)?.time?.let { parsedTime ->
                if (parsedTime > 0L) takenAtMillis = parsedTime
              }
            }
          } catch (_: Exception) {
            // Non-EXIF image format (e.g. PNG/WebP); use current timestamp & normal orientation
          }
        }

        // 2. Decode bounds first to avoid OOM on high-MP phone photos
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        if (rawW <= 0 || rawH <= 0) {
          return@withContext Result.failure(IllegalArgumentException("Unable to read selected image."))
        }

        val sampleSize = calculateInSampleSize(rawW, rawH, MAX_DIMENSION_PX)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampledBitmap =
          resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
          }
            ?: return@withContext Result.failure(
              IllegalArgumentException("Unable to decode selected image.")
            )

        val orientedBitmap = applyExifOrientation(sampledBitmap, orientation)
        if (orientedBitmap != sampledBitmap) {
          sampledBitmap.recycle()
        }

        val scaledBitmap = scaleToMaxDimension(orientedBitmap, MAX_DIMENSION_PX)
        if (scaledBitmap != orientedBitmap) {
          orientedBitmap.recycle()
        }

        val aspectRatio =
          (scaledBitmap.width.toDouble() / max(1, scaledBitmap.height).toDouble())
            .coerceIn(0.25, 4.5)

        // 3. Compress JPEG iteratively to fit Firestore 1 MiB document ceiling cleanly
        val baos = ByteArrayOutputStream()
        var quality = 82
        var jpegBytes: ByteArray
        do {
          baos.reset()
          scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
          jpegBytes = baos.toByteArray()
          quality -= 10
        } while (jpegBytes.size > MAX_JPEG_BYTES && quality >= 35)

        scaledBitmap.recycle()

        val encoded = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        if (encoded.length > MAX_BASE64_CHARS) {
          return@withContext Result.failure(
            IllegalArgumentException("Image is still too large after compression.")
          )
        }

        val dayLabel = formatDayLabel(takenAtMillis)
        Result.success(
          EncodedPhoto(
            imageBase64 = encoded,
            aspectRatio = aspectRatio,
            takenAtMillis = takenAtMillis,
            dayLabel = dayLabel,
          )
        )
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  fun formatDayLabel(millis: Long): String {
    val formatter = SimpleDateFormat("EEE · MMM d", Locale.getDefault())
    return formatter.format(Date(millis)).take(40)
  }

  private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var inSampleSize = 1
    val longest = max(width, height)
    while (longest / inSampleSize > maxDim * 2) {
      inSampleSize *= 2
    }
    return inSampleSize
  }

  private fun scaleToMaxDimension(bitmap: Bitmap, maxDim: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxDim) return bitmap
    val scale = maxDim.toFloat() / longest.toFloat()
    val targetW = max(1, (bitmap.width * scale).roundToInt())
    val targetH = max(1, (bitmap.height * scale).roundToInt())
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
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
}
