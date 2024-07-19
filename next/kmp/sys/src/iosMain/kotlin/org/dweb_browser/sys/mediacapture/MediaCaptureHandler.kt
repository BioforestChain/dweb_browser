package org.dweb_browser.sys.mediacapture

import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.platform.NSDataHelper.toByteArray
import platform.CoreServices.kUTTypeMovie
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerImageURL
import platform.UIKit.UIImagePickerControllerMediaURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue


class MediaCaptureHandler {

  enum class MediaType {
    CAMERA, PHOTO, VIDEO
  }

  val rootController: UIViewController?
    get() {
      return UIApplication.sharedApplication.keyWindow?.rootViewController
    }

  private var mediaType: MediaType? = null
  private var imageBase64Callback: ((result: ByteArray) -> Unit)? = null
  private var filePathCallback: ((result: String) -> Unit)? = null

  fun launchCameraString(callback: (result: ByteArray) -> Unit) {
    imageBase64Callback = callback
    mediaType = MediaType.CAMERA
    launchImagePickerController()
  }

  fun launchPhotoString(callback: (result: String) -> Unit) {
    filePathCallback = callback
    mediaType = MediaType.PHOTO
    launchImagePickerController()
  }

  fun launchVideoPath(callback: (result: String) -> Unit) {
    filePathCallback = callback
    mediaType = MediaType.VIDEO
    launchImagePickerController()
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun launchImagePickerController() {
    val picker = UIImagePickerController()
    picker.delegate = delegate
    picker.allowsEditing = true
    when (mediaType) {
      MediaType.CAMERA -> picker.sourceType =
        UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera

      MediaType.PHOTO -> picker.sourceType =
        UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary

      else -> {
        picker.sourceType =
          UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.mediaTypes = listOf(kUTTypeMovie)
      }
    }
    dispatch_async(dispatch_get_main_queue()) {
      rootController?.presentViewController(picker, true, null)
    }
  }

  inner class UIDelegate : NSObject(), UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
      picker: UIImagePickerController,
      didFinishPickingImage: UIImage,
      editingInfo: Map<Any?, *>?
    ) {
      picker.dismissViewControllerAnimated(true, null)
      val image = editingInfo?.get(UIImagePickerControllerOriginalImage) as? UIImage
      val imagePath = editingInfo?.get(UIImagePickerControllerImageURL) as? NSURL
      val videoPath = editingInfo?.get(UIImagePickerControllerMediaURL) as? NSURL

      when (mediaType) {
        MediaType.CAMERA -> {
          val data = image?.let { UIImagePNGRepresentation(it) }
          imageBase64Callback?.let { it(data?.toByteArray() ?: ByteArray(0)) }
        }

        MediaType.PHOTO -> filePathCallback?.let { it(imagePath?.absoluteString ?: "") }
        else -> filePathCallback?.let { it(videoPath?.absoluteString ?: "") }
      }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
      picker.dismissViewControllerAnimated(true, null)
      imageBase64Callback?.let { it(ByteArray(0)) }
      filePathCallback?.let { it("") }
    }
  }

  private val delegate = UIDelegate()
}