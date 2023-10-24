package org.dweb_browser.shared.microService.sys.clipboard

expect fun writeClipboard(label: String?, content: String?, type: ClipboardType): ClipboardWriteResponse

expect fun readClipboard(): ClipboardData