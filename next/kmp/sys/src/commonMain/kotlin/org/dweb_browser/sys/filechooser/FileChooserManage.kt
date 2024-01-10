package org.dweb_browser.sys.filechooser

import org.dweb_browser.core.module.MicroModule

expect class FileChooserManage() {
  suspend fun openFileChooser(
    microModule: MicroModule, mimeTypes: String, multiple: Boolean, limit: Int
  ): List<String>
}
