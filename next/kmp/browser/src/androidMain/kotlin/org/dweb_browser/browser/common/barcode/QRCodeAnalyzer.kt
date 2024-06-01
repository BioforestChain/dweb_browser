package org.dweb_browser.browser.common.barcode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.Image.Plane
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal class QRCodeAnalyzer(
  private val onFailure: (Exception) -> Unit,
  private val onBarcodeDetected: (Bitmap?, List<Barcode>) -> Unit,
) : ImageAnalysis.Analyzer {

  @ExperimentalGetImage
  override fun analyze(imageProxy: ImageProxy) {
    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val barcodeScanner = BarcodeScanning.getClient(options)

    imageProxy.image?.let { image ->
      val rotationDegrees = imageProxy.imageInfo.rotationDegrees
      val bitmap = BitmapUtils.getBitmap(image, rotationDegrees) // 产生图片
      val imageProcess = InputImage.fromMediaImage(image, rotationDegrees)
      barcodeScanner.process(imageProcess).addOnSuccessListener { barcodeList ->
        if (barcodeList.isNotEmpty()) {
          onBarcodeDetected(bitmap, barcodeList)
        } else {
          onFailure(Exception("Not found Barcode"))
        }
      }.addOnFailureListener { exception ->
        onFailure(exception)
      }.addOnCompleteListener {
        image.close()
        imageProxy.close()
      }
    }
  }
}

object BitmapUtils {
  private fun getBitmap(data: ByteBuffer, width: Int, height: Int, rotationDegrees: Int): Bitmap? {
    data.rewind()
    val imageInBuffer = ByteArray(data.limit())
    data[imageInBuffer, 0, imageInBuffer.size]
    try {
      val image = YuvImage(
        imageInBuffer, ImageFormat.NV21, width, height, null
      )
      val stream = ByteArrayOutputStream()
      image.compressToJpeg(Rect(0, 0, width, height), 80, stream)
      val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
      stream.close()
      return rotateBitmap(bmp, rotationDegrees, false, false)
    } catch (e: java.lang.Exception) {
      Log.e("getBitmap", "Error: " + e.message)
    }
    return null
  }

  @ExperimentalGetImage
  fun getBitmap(image: Image, rotationDegrees: Int): Bitmap? {
    val nv21Buffer = yuv420ThreePlanesToNV21(image.planes, image.width, image.height)
    return getBitmap(nv21Buffer, image.width, image.height, rotationDegrees)
  }

  private fun rotateBitmap(
    bitmap: Bitmap, rotationDegrees: Int, flipX: Boolean, flipY: Boolean,
  ): Bitmap? {
    val matrix = Matrix()

    // Rotate the image back to straight.
    matrix.postRotate(rotationDegrees.toFloat())

    // Mirror the image along the X or Y axis.
    matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    // Recycle the old bitmap if it has changed.
    if (rotatedBitmap != bitmap) {
      bitmap.recycle()
    }
    return rotatedBitmap
  }

  private fun yuv420ThreePlanesToNV21(
    yuv420888planes: Array<Plane>, width: Int, height: Int,
  ): ByteBuffer {
    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))
    if (areUVPlanesNV21(yuv420888planes, width, height)) {
      // Copy the Y values.
      yuv420888planes[0].buffer[out, 0, imageSize]
      val uBuffer = yuv420888planes[1].buffer
      val vBuffer = yuv420888planes[2].buffer
      // Get the first V value from the V buffer, since the U buffer does not contain it.
      vBuffer[out, imageSize, 1]
      // Copy the first U value and the remaining VU values from the U buffer.
      uBuffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
    } else {
      // Fallback to copying the UV values one by one, which is slower but also works.
      // Unpack Y.
      unpackPlane(yuv420888planes[0], width, height, out, 0, 1)
      // Unpack U.
      unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2)
      // Unpack V.
      unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2)
    }
    return ByteBuffer.wrap(out)
  }

  private fun areUVPlanesNV21(planes: Array<Plane>, width: Int, height: Int): Boolean {
    val imageSize = width * height
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    // Backup buffer properties.
    val vBufferPosition = vBuffer.position()
    val uBufferLimit = uBuffer.limit()

    // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
    vBuffer.position(vBufferPosition + 1)
    // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
    uBuffer.limit(uBufferLimit - 1)

    // Check that the buffers are equal and have the expected number of elements.
    val areNV21 = vBuffer.remaining() == 2 * imageSize / 4 - 2 && vBuffer.compareTo(uBuffer) == 0

    // Restore buffers to their initial state.
    vBuffer.position(vBufferPosition)
    uBuffer.limit(uBufferLimit)
    return areNV21
  }

  private fun unpackPlane(
    plane: Plane, width: Int, height: Int, out: ByteArray, offset: Int, pixelStride: Int,
  ) {
    val buffer = plane.buffer
    buffer.rewind()

    // Compute the size of the current plane.
    // We assume that it has the aspect ratio as the original image.
    val numRow = (buffer.limit() + plane.rowStride - 1) / plane.rowStride
    if (numRow == 0) {
      return
    }
    val scaleFactor = height / numRow
    val numCol = width / scaleFactor

    // Extract the data in the output buffer.
    var outputPos = offset
    var rowStart = 0
    for (row in 0 until numRow) {
      var inputPos = rowStart
      for (col in 0 until numCol) {
        out[outputPos] = buffer[inputPos]
        outputPos += pixelStride
        inputPos += plane.pixelStride
      }
      rowStart += plane.rowStride
    }
  }
}

internal object PointUtils {
  /**
   * 转换坐标：将原始 point 的坐标点从原始：srcWidth，srcHeight 进行换算后，转换成目标：destWidth，destHeight 后的坐标点
   * @param isFit 是否自适应，如果为 true 表示：宽或高自适应铺满，如果为 false 表示：填充铺满（可能会出现裁剪）
   */
  @JvmOverloads
  fun transform(
    point: Point,
    srcWidth: Int,
    srcHeight: Int,
    destWidth: Int,
    destHeight: Int,
    isFit: Boolean = false,
  ): Point {
    return transform(point.x, point.y, srcWidth, srcHeight, destWidth, destHeight, isFit)
  }

  /**
   * 转换坐标：将原始 x，y 的坐标点从原始：srcWidth，srcHeight 进行换算后，转换成目标：destWidth，destHeight 后的坐标点
   * @param isFit 是否自适应，如果为 true 表示：宽或高自适应铺满，如果为 false 表示：填充铺满（可能会出现裁剪）
   */
  @JvmOverloads
  fun transform(
    x: Int,
    y: Int,
    srcWidth: Int,
    srcHeight: Int,
    destWidth: Int,
    destHeight: Int,
    isFit: Boolean = false,
  ): Point {
    val widthRatio = destWidth * 1.0f / srcWidth
    val heightRatio = destHeight * 1.0f / srcHeight
    val point = Point()
    if (isFit) { //宽或高自适应铺满
      val ratio = Math.min(widthRatio, heightRatio)
      val left = Math.abs(srcWidth * ratio - destWidth) / 2
      val top = Math.abs(srcHeight * ratio - destHeight) / 2
      point.x = (x * ratio + left).toInt()
      point.y = (y * ratio + top).toInt()
    } else { //填充铺满（可能会出现裁剪）
      val ratio = Math.max(widthRatio, heightRatio)
      val left = Math.abs(srcWidth * ratio - destWidth) / 2
      val top = Math.abs(srcHeight * ratio - destHeight) / 2
      point.x = (x * ratio - left).toInt()
      point.y = (y * ratio - top).toInt()
    }
    return point
  }
}