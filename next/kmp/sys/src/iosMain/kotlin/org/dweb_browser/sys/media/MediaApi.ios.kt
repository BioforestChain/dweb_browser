package org.dweb_browser.sys.media


import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultiPartFileEncode
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.toUtf8ByteArray
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

actual suspend fun savePictures(saveLocation: String?, files: List<MultiPartFile>) {
  files.forEach { multiPartFile ->
    savePicture(multiPartFile)
  }
}

private fun savePicture(files: MultiPartFile) {
  val data = when (files.encode) {
    MultiPartFileEncode.UTF8 -> files.data.toUtf8ByteArray()
    MultiPartFileEncode.BASE64 -> files.data.toBase64ByteArray()
  }
  val uiImage = byteArrayToUIImage(data)
  //图片对象、目标对象、一个完成时调用的选择器（selector）以及一个上下文信息对象
  saveImageToPhotosAlbum(uiImage)
}

@OptIn(ExperimentalForeignApi::class)
fun saveImageToPhotosAlbum(image: UIImage) {
  val callbackPointer =
    staticCFunction { _: COpaquePointer? ->
      println("Hello from thread!")
      null
    }
  debugMedia("ios ","saveImageToPhotosAlbum ${image.images?.size}")
  UIImageWriteToSavedPhotosAlbum(
    image,
    null,
    callbackPointer,
    null
  )
}


@OptIn(ExperimentalForeignApi::class)
fun byteArrayToUIImage(byteArray: ByteArray): UIImage {
  val data = byteArray.usePinned { pinned ->
    NSData.dataWithBytes(pinned.addressOf(0), byteArray.size.toULong())
  }
  return UIImage(data = data)
}