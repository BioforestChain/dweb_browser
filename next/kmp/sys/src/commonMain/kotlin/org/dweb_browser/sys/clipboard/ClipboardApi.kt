package org.dweb_browser.sys.clipboard

expect class ClipboardApi() {
  fun write(
    label: String?,
    content: String?,
    type: ClipboardType
  ): ClipboardWriteResponse

  fun read(): ClipboardData
}