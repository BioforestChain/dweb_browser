package org.dweb_browser.sys.mediacapture

import org.dweb_browser.sys.scan.toByteArray
import platform.Foundation.NSURL
import platform.Foundation.base64Encoding
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
import platform.darwin.NSObject

class MediaCaptureHandler {

    val rootController: UIViewController?
        get() {
            return UIApplication.sharedApplication.keyWindow?.rootViewController
        }

    private var imageBase64Callback: (result: String) -> Unit = {}

    fun launchCameraString(callback: (result: String) -> Unit) {
        imageBase64Callback = callback
        launchImagePickerController()
    }

    private fun launchImagePickerController() {
        val picker = UIImagePickerController()
        picker.delegate = delegate
        picker.allowsEditing = true
        picker.sourceType =
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        rootController?.presentViewController(picker,true,null)
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
//            data?.toByteArray()
            val base64 = data?.base64Encoding() ?: ""
            imageBase64Callback(base64)
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true,null)
        }
    }
}