package org.dweb_browser.sys.filechooser

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.WARNING

actual class FileChooserManage {
  actual suspend fun openFileChooser(
    microModule: MicroModule, mimeTypes: String, multiple: Boolean, limit: Int
  ): List<String> {
    WARNING("Not yet implemented openFileChooser")
    return emptyList()
  }
}
