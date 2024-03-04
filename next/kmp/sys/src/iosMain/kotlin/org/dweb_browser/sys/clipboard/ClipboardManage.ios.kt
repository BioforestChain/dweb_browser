package org.dweb_browser.sys.clipboard

import kotlinx.cinterop.BetaInteropApi
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.base64Encoding
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIPasteboard

actual class ClipboardManage {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.CLIPBOARD) {
        AuthorizationStatus.GRANTED
      } else null
    }
  }

  @OptIn(BetaInteropApi::class)
  actual fun write(
    label: String?,
    content: String?,
    type: ClipboardType
  ): ClipboardWriteResponse {
    val pasteboard = UIPasteboard.generalPasteboard
    when (type) {
      ClipboardType.STRING -> {
        pasteboard.string = content
      }

      ClipboardType.IMAGE -> {
        if (content == null) {
          return ClipboardWriteResponse(false, "image content is null")
        }
        val tmpImage = content.replace("data:image/png;base64,", "")
        val data: NSData = NSData.create(base64Encoding = tmpImage)
          ?: return ClipboardWriteResponse(false, "image data is null")
        pasteboard.image = UIImage.imageWithData(data)
      }

      ClipboardType.URL -> {
        if (content == null) {
          return ClipboardWriteResponse(false, "url content is null")
        }
        pasteboard.URL = NSURL.URLWithString(content)
      }
    }
    return ClipboardWriteResponse(true)
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