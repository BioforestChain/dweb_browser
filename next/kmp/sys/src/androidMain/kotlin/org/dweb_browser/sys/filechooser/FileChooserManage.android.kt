package org.dweb_browser.sys.filechooser

import org.dweb_browser.core.module.MicroModule

actual class FileChooserManage {
  actual suspend fun openFileChooser(
    microModule: MicroModule.Runtime, accept: String, multiple: Boolean
  ): List<String> {
    return FileChooserActivity.launchAndroidFileChooser(microModule, accept, multiple)
  }
}
