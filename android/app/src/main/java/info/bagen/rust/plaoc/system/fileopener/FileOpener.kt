package info.bagen.rust.plaoc.system.fileopener

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import info.bagen.rust.plaoc.App
import java.io.File


object FileOpener {

    fun open(
        filePath: String,
        contentType: String? = null,
        openWithDefault: Boolean,
        onErrorCallback: (String) -> Unit
    ) {
        val fileName = try {
            Uri.parse(filePath).path
        } catch (e: Exception) {
            filePath
        }

        fileName?.let { fileName ->
            val file = File(fileName)
            if (file.exists()) {
                try {
                    val type = if (contentType == null || contentType.trim().isEmpty()) {
                        getMimeType(fileName)
                    } else {
                        contentType
                    }

                    val intent = Intent(Intent.ACTION_VIEW)
                    /*val path: Uri = FileProvider.getUriForFile(
                      App.appContext, App.appContext.packageName + ".file.opener.provider", file
                    )*/
                    val path = Uri.parse("content://${file.absolutePath}")
                    intent.setDataAndType(path, type)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    App.dwebViewActivity?.let { activity ->
                        if (openWithDefault) {
                            activity.startActivity(intent)
                        } else {
                            activity.startActivity(Intent.createChooser(intent, "Open File in..."))
                        }
                    }
                } catch (exception: ActivityNotFoundException) {
                    Log.e("FileOpener", "FIleOpener::open ActivityNotFoundException->$exception")
                    onErrorCallback("Activity not found: ${exception.message} --> 8 $exception")
                } catch (exception: Exception) {
                    Log.e("FileOpener", "FIleOpener::open Exception->$exception")
                    onErrorCallback("${exception.localizedMessage} --> 1 $exception")
                }

            } else {
                onErrorCallback("File not found --> 9")
            }
        }
    }

    private fun getMimeType(url: String): String {
        var mimeType = "*/*"
        val extensionIndex = url.lastIndexOf('.')
        if (extensionIndex > 0) {
            val extMimeType =
                MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(url.substring(extensionIndex + 1))
            if (extMimeType != null) {
                mimeType = extMimeType
            }
        }
        return mimeType
    }
}

data class FileOpenerOption(
    var filePath: String,
    var contentType: String? = null,
    var openWithDefault: Boolean = true,
)

