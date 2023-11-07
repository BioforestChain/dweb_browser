package org.dweb_browser.sys.clipboard

import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.base64Encoding
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIPasteboard

actual fun writeClipboard(label: String?, content: String?, type: ClipboardType): ClipboardWriteResponse {

    val pasteboard = UIPasteboard.generalPasteboard
    if (type == ClipboardType.STRING) {
        pasteboard.string = content
    } else if (type == ClipboardType.IMAGE) {
        if (content == null) {
            return ClipboardWriteResponse(false,"image content is null")
        }
        var tmpImage = content!!.replace("data:image/png;base64,", "")
        var data: NSData? = NSData.create(base64Encoding = tmpImage)
            ?: return ClipboardWriteResponse(false,"image data is null")
        pasteboard.image = data?.let { UIImage.imageWithData(it) }
    } else if (type == ClipboardType.URL) {
        if (content == null) {
            return ClipboardWriteResponse(false,"url content is null")
        }
        pasteboard.URL = NSURL.URLWithString(content!!)
    }
    return ClipboardWriteResponse(true)
}

actual fun readClipboard(): ClipboardData {
    val pasteboard = UIPasteboard.generalPasteboard
    var value: String = ""
    var type: String = ""
    if (pasteboard.hasStrings) {
        value = pasteboard.string.toString()
        type = "text/plain"
    } else if (pasteboard.hasImages) {
        val image = pasteboard.image
        if (image != null) {
            val data = UIImagePNGRepresentation(image!!)
            var base64 = data?.base64Encoding()
            value = "data:image/png;base64,$base64"
            type = "text/png"
        }
    } else if (pasteboard.hasURLs) {
        value = pasteboard.URL?.absoluteString.toString()
        type = "text/plain"
    }
    return ClipboardData(value,type)
}
