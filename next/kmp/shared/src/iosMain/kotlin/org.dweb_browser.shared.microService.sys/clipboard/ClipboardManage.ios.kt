package org.dweb_browser.shared.microService.sys.clipboard

import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.base64Encoding
import platform.Foundation.create
import platform.UIKit.UIImage
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
        var data = NSData.create(base64Encoding = tmpImage)
        if (data != null) {
            pasteboard.image = UIImage.imageWithData(data)
        }
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
        var base64 = pasteboard.image?.pngData()?.base64Encoding()
        value = "data:image/png;base64,$base64"
        type = "text/png"
    } else if (pasteboard.hasURLs) {
        var base64 = pasteboard.image?.pngData()?.base64Encoding()
        value = pasteboard.URL?.absoluteString.toString()
        type = "text/plain"
    }
    return ClipboardData(value,type)
}

fun UIImage.pngData(): NSData {
    return pngData()
}