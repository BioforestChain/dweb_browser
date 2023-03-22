package info.bagen.rust.plaoc.microService.webview

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResult
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.sys.plugin.permission.PermissionManager
import java.util.*

fun isMediaCaptureSupported(): Boolean {
    val permissions = arrayOf(Manifest.permission.CAMERA)
    return PermissionManager.hasPermissions(App.appContext, permissions) ||
            !PermissionManager.hasDefinedPermission(
                App.appContext,
                Manifest.permission.CAMERA
            )
}

//         fun showMediaCaptureOrFilePicker(
//             filePathCallback: ValueCallback<Array<Uri>>,
//             fileChooserParams: WebChromeClient.FileChooserParams,
//             isVideo: Boolean
//        ) {
//            // TODO: add support for video capture on Android M and older
//            // On Android M and lower the VIDEO_CAPTURE_INTENT (e.g.: intent.getData())
//            // returns a file:// URI instead of the expected content:// URI.
//            // So we disable it for now because it requires a bit more work
//            var shown = false
//            if (isVideo) {
//                shown = showVideoCapturePicker(filePathCallback)
//            } else {
//                shown = showImageCapturePicker(filePathCallback)
//            }
//            if (!shown) {
//                showFilePicker(filePathCallback, fileChooserParams)
//            }
//        }

//private fun showFilePicker(
//    filePathCallback: ValueCallback<Array<Uri?>?>,
//    fileChooserParams: WebChromeClient.FileChooserParams
//) {
//    val intent = fileChooserParams.createIntent()
//    if (fileChooserParams.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//    }
//    if (fileChooserParams.acceptTypes.size > 1 || intent.type!!.startsWith(".")) {
//        val validTypes: Array<String> = getValidTypes(fileChooserParams.acceptTypes)
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, validTypes)
//        if (intent.type!!.startsWith(".")) {
//            intent.type = validTypes[0]
//        }
//    }
//    try {
//       val activityLauncher = { activityResult ->
//            val result: Array<Uri?>?
//            val resultIntent: Intent = activityResult.getData()
//            if (activityResult.getResultCode() == Activity.RESULT_OK && resultIntent.clipData != null && resultIntent.clipData!!.itemCount > 1) {
//                val numFiles = resultIntent.clipData!!.itemCount
//                result = arrayOfNulls(numFiles)
//                for (i in 0 until numFiles) {
//                    result[i] = resultIntent.clipData!!.getItemAt(i).uri
//                }
//            } else {
//                result = WebChromeClient.FileChooserParams.parseResult(
//                    activityResult.getResultCode(),
//                    resultIntent
//                )
//            }
//            filePathCallback.onReceiveValue(result)
//        }
//        Activity.launch(intent)
//    } catch (e: ActivityNotFoundException) {
//        filePathCallback.onReceiveValue(null)
//    }
//}


fun getValidTypes(currentTypes: Array<String>): Array<String> {
    val validTypes: MutableList<String> = ArrayList()
    val mtm: MimeTypeMap = MimeTypeMap.getSingleton()
    for (mime in currentTypes) {
        if (mime.startsWith(".")) {
            val extension = mime.substring(1)
            val extensionMime = mtm.getMimeTypeFromExtension(extension)
            if (extensionMime != null && !validTypes.contains(extensionMime)) {
                validTypes.add(extensionMime)
            }
        } else if (!validTypes.contains(mime)) {
            validTypes.add(mime)
        }
    }
    val validObj: Array<Any> = validTypes.toTypedArray()
    return Arrays.copyOf(validObj, validObj.size, Array<String>::class.java)
}
