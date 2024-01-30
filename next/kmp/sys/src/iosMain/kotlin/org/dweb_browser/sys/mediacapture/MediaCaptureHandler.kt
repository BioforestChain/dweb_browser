package org.dweb_browser.sys.mediacapture

import org.dweb_browser.sys.scan.toByteArray
import platform.Foundation.NSURL
import platform.Foundation.base64Encoding
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

class MediaCaptureHandler {

    val rootController: UIViewController?
        get() {
            return UIApplication.sharedApplication.keyWindow?.rootViewController
        }

    private var imageBase64Callback: (result: ByteArray) -> Unit = {}

    fun launchCameraString(callback: (result: ByteArray) -> Unit) {
        imageBase64Callback = callback
        launchImagePickerController()
    }

    private fun launchImagePickerController() {
        val picker = UIImagePickerController()
        picker.delegate = delegate
        picker.allowsEditing = true
        picker.sourceType =
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        dispatch_async(dispatch_get_main_queue()) {
            rootController?.presentViewController(picker,true,null)
        }
    }

    private val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol,
        UINavigationControllerDelegateProtocol {
        override fun imagePickerController(
            picker: UIImagePickerController,
            didFinishPickingImage: UIImage,
            editingInfo: Map<Any?, *>?
        ) {
            picker.dismissViewControllerAnimated(true,null)
            val image = editingInfo?.get(UIImagePickerControllerOriginalImage) as? UIImage ?: return
            val data = UIImagePNGRepresentation(image)
            imageBase64Callback(data?.toByteArray() ?: ByteArray(0))
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true,null)
            imageBase64Callback(ByteArray(0))
        }
    }
}