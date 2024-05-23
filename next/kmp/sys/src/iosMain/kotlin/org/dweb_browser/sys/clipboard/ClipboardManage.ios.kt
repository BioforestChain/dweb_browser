package org.dweb_browser.sys.clipboard

import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.base64Encoding
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIPasteboard

actual class ClipboardManage {
  val clipboard get() = UIPasteboard.generalPasteboard

  actual fun writeText(
    content: String,
    label: String?
  ) = tryWriteClipboard {
    clipboard.string = content
  }

  actual fun writeImage(
    base64DataUri: String,
    label: String?
  ) = tryWriteClipboard {
    val (imageData) = splitBase64DataUriToFile(base64DataUri)
    val data: NSData = imageData.toNSData()
    clipboard.image = UIImage.imageWithData(data)
  }

  actual fun writeUrl(
    url: String,
    label: String?
  ) = tryWriteClipboard {
    clipboard.URL = NSURL.URLWithString(url)
  }

  actual fun clear(): Boolean {
    clipboard.apply {
      string = null
      strings = null
      image = null
      images = null
      URL = null
      URLs = null
      color = null
      colors = null
    }
    return true
  }

  actual fun read(): ClipboardData {
    val pasteboard = UIPasteboard.generalPasteboard
    var value = ""
    var type = ""
    if (pasteboard.hasStrings) {
      value = pasteboard.string.toString()
      type = "text/plain"
    } else if (pasteboard.hasImages) {
      val image = pasteboard.image
      if (image != null) {
        val data = UIImagePNGRepresentation(image)
        val base64 = data?.base64Encoding()
        if (base64 != null) {
          value = "data:image/png;base64,$base64"
        }
        type = "text/png"
      }
    } else if (pasteboard.hasURLs) {
      value = pasteboard.URL?.absoluteString.toString()
      type = "text/plain"
    }
    return ClipboardData(value, type)
  }
}