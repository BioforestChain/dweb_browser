package info.bagen.rust.plaoc.webkit.inputFile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "InputFileActivityResultContract"

@RequiresApi(18)
open class InputFileActivityResultContract :
    ActivityResultContract<InputFileOptions, List<@JvmSuppressWildcards Uri>>() {
    var captureUri: Uri? = null
    lateinit var context: Context

    @CallSuper
    override fun createIntent(context: Context, input: InputFileOptions): Intent {
        this.context = context
        if (input.capture) {

            val tempDir =
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            Log.i(TAG, "tempDir: $tempDir")
            val tmpFileTitle = "capture-" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss_SSS")
            )

            when {
                input.accept[0] == "image/*" -> {
                    val tempFile: File = File.createTempFile(tmpFileTitle, ".jpg", tempDir)
                    Log.i(TAG, "fileprovider: ${context.packageName + ".fileprovider"}")
                    captureUri =
                        FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            tempFile
                        )
                    Log.i(TAG, "tempUri: $captureUri")
                    return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        .putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                }
                input.accept[0] == "video/*" -> {
                    val tempFile: File = File.createTempFile(tmpFileTitle, ".mp4", tempDir)
                    Log.i(TAG, "fileprovider: ${context.packageName + ".fileprovider"}")
                    captureUri =
                        FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            tempFile
                        )
                    Log.i(TAG, "tempUri: $captureUri")
                    return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        .putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                }
                input.accept[0] == "audio/*" -> {
//                    val tempFile: File = File.createTempFile(tmpFileTitle, ".aac", tempDir)
//                    Log.i(TAG, "fileprovider: ${context.packageName + ".fileprovider"}")
//                    captureUri =
//                        FileProvider.getUriForFile(
//                            context,
//                            context.packageName + ".fileprovider",
//                            tempFile
//                        );
//                    Log.i(TAG, "tempUri: $captureUri");
                    /**
                     * @todo RECORD_SOUND_ACTION 不支持 EXTRA_OUTPUT
                     * 这里 captureUri 是用在 intent 返回data之后，用来移动到该文件的
                     */
                    return Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
//                        .putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                }
            }
        }
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE).also { it ->
                if (input.multiple) {
                    it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                /**
                 * @todo Android EXTRA_MIME_TYPES 不支持 HTML-accpet标准中的文件名后缀:
                 *
                 * - A valid case-insensitive filename extension, starting with a period (".") character. For example: .jpg, .pdf, or .doc.
                 * - A valid MIME type string, with no extensions.
                 * - The string audio/\* meaning "any audio file".
                 * - The string video/\* meaning "any video file".
                 * - The string image/\* meaning "any image file".
                 */
                if (input.accept.size > 1) {
                    it.type = "*/*"
                    it.putExtra(Intent.EXTRA_MIME_TYPES, input.accept.toTypedArray())
                } else {
                    it.type = input.accept.getOrElse(0) { "*/*" }
                }
            }

    }

    final override fun getSynchronousResult(
        context: Context,
        input: InputFileOptions
    ): SynchronousResult<List<@JvmSuppressWildcards Uri>>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        val tmpCaptureUri = captureUri
        captureUri = null
        if (resultCode == Activity.RESULT_OK) {
            return intent?.let {
                // Use a LinkedHashSet to maintain any ordering that may be
                // present in the ClipData
                val resultSet = mutableSetOf<Uri>()
                it.data?.let { data ->
                    resultSet.add(data)
                }
                val clipData = it.clipData
                if (clipData == null && resultSet.isEmpty()) {
                    return emptyList()
                } else if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if (uri != null) {
                            resultSet.add(uri)
                        }
                    }
                }

//                if (tmpCaptureUri != null) {
//                    resultSet.toList().getOrNull(0)?.let { uri ->
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                            context.contentResolver.openInputStream(uri)?.let { inStream ->
//                                context.contentResolver.openOutputStream(tmpCaptureUri)
//                                    ?.let { outStream ->
//                                        inStream.copyTo(outStream)
////                                        context.contentResolver.persistedUriPermissions
//                                        context.contentResolver.delete(uri, null)
//                                    }
//                            }
//                        } else {
//                            TODO("VERSION.SDK_INT < R")
//                        }
//                    }
//                }

                return ArrayList(resultSet)
            } ?: tmpCaptureUri?.let { uri ->
                listOf(uri)
            } ?: emptyList()
        }
        return emptyList()
    }
}

data class InputFileOptions(
    val accept: List<String>,
    val multiple: Boolean,
    val capture: Boolean
)

