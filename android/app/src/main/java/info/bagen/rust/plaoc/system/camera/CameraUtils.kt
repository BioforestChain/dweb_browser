package info.bagen.rust.plaoc.system.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import info.bagen.rust.plaoc.App
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


object CameraUtils {
    fun createImageFileUri(appId: String): Uri? {
        val photoFile = createImageFile()
        return FileProvider.getUriForFile(App.appContext, "$appId.fileprovider", photoFile)
    }

    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir: File? = App.appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }


    /**
     * Resize an image to the given max width and max height. Constraint can be put
     * on one dimension, or both. Resize will always preserve aspect ratio.
     * @param bitmap
     * @param desiredMaxWidth
     * @param desiredMaxHeight
     * @return a new, scaled Bitmap
     */
    fun resize(bitmap: Bitmap, desiredMaxWidth: Int, desiredMaxHeight: Int): Bitmap {
        return resizePreservingAspectRatio(bitmap, desiredMaxWidth, desiredMaxHeight)
    }

    /**
     * Resize an image to the given max width and max height. Constraint can be put
     * on one dimension, or both. Resize will always preserve aspect ratio.
     * @param bitmap
     * @param desiredMaxWidth
     * @param desiredMaxHeight
     * @return a new, scaled Bitmap
     */
    private fun resizePreservingAspectRatio(
        bitmap: Bitmap, desiredMaxWidth: Int, desiredMaxHeight: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 0 is treated as 'no restriction'
        val maxHeight = if (desiredMaxHeight == 0) height else desiredMaxHeight
        val maxWidth = if (desiredMaxWidth == 0) width else desiredMaxWidth

        // resize with preserved aspect ratio
        var newWidth = width.coerceAtMost(maxWidth).toFloat()
        var newHeight = height * newWidth / width
        if (newHeight > maxHeight) {
            newWidth = (width * maxHeight / height).toFloat()
            newHeight = maxHeight.toFloat()
        }
        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth.roundToInt(),
            newHeight.roundToInt(),
            false
        )
    }

    /**
     * Transform an image with the given matrix
     * @param bitmap
     * @param matrix
     * @return
     */
    private fun transform(bitmap: Bitmap, matrix: Matrix): Bitmap {
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Correct the orientation of an image by reading its exif information and rotating
     * the appropriate amount for portrait mode
     * @param bitmap
     * @param imageUri
     * @param exif
     * @return
     */
    fun correctOrientation(bitmap: Bitmap, imageUri: Uri, exif: ExifWrapper): Bitmap {
        val orientation = getOrientation(App.appContext, imageUri)
        return if (orientation != 0) {
            val matrix = Matrix()
            matrix.postRotate(orientation.toFloat())
            exif.resetOrientation()
            transform(bitmap, matrix)
        } else {
            bitmap
        }
    }

    private fun getOrientation(c: Context, imageUri: Uri): Int {
        var result = 0
        c.contentResolver.openInputStream(imageUri)?.use { iStream ->
            val exifInterface = ExifInterface(iStream)
            val orientation: Int = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            result = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
        return result
    }

    fun getExifData(bitmap: Bitmap, imageUri: Uri): ExifWrapper {
        var stream: InputStream? = null
        try {
            stream = App.appContext.contentResolver.openInputStream(imageUri)
            stream?.let {
                val exifInterface = ExifInterface(it)
                return ExifWrapper(exifInterface)
            }
        } catch (ex: IOException) {
            Log.e("CameraUtils", "Error loading exif data from image", ex)
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (ignored: IOException) {
                }
            }
        }
        return ExifWrapper(null)
    }
}
