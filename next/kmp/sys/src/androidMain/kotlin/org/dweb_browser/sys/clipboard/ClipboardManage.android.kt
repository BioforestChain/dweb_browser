package org.dweb_browser.sys.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.ktor.util.sha1
import org.dweb_browser.helper.getAppContextUnsafe

private val clipboard by lazy {
  getAppContextUnsafe().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

actual class ClipboardManage {
  /**
   * @param content 剪辑中的实际文本
   * @param label 剪辑数据的用户可见标签
   */
  actual fun writeText(
    content: String,
    label: String?,
  ) = tryWriteClipboard {
    clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
  }

  private val imageTmpDir = getAppContextUnsafe().cacheDir.resolve("clipboard").apply { mkdirs() }

  @OptIn(ExperimentalStdlibApi::class)
  actual fun writeImage(
    base64DataUri: String,
    label: String?,
  ) = tryWriteClipboard {
    val (imageData, imageMime) = splitBase64DataUriToFile(base64DataUri)
    val imageHash = sha1(imageData).toHexString()

    val imageFile = imageTmpDir.resolve(imageHash).also { imageFile ->
      if (!imageFile.exists()) {
        imageTmpDir.resolve("$imageHash.tmp").apply {
          createNewFile()
          writeBytes(imageData)
          this.renameTo(imageFile)
        }
      }
    }
    clipboard.setPrimaryClip(ClipData.newIntent(label, Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imageFile))
      type = imageMime
    }))
  }

  actual fun writeUrl(url: String, label: String?) = tryWriteClipboard {
    clipboard.setPrimaryClip(ClipData.newRawUri(label, Uri.parse(url)))
  }

  actual fun clear() = runCatching {
    clipboard.clearPrimaryClip()
    true
  }.getOrElse { false }


  actual fun read(): ClipboardData {
    var value: CharSequence? = null
    if (clipboard.hasPrimaryClip()) {
      value =
        if (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
          val item = clipboard.primaryClip?.getItemAt(0)
          item?.text
        } else {
          val item = clipboard.primaryClip?.getItemAt(0)
          item?.coerceToText(getAppContextUnsafe()).toString()
        }
    }
    var type = "text/plain"
    if (value != null && value.toString().startsWith("data:")) {
      type = value.toString().split(";").toTypedArray()[0].split(":").toTypedArray()[1]
    }
    return ClipboardData(value.toString(), type)
  }
}