package org.dweb_browser.sys.filechooser

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.platform.awaitComposeWindow
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter

actual class FileChooserManage actual constructor() {
  actual suspend fun openFileChooser(
    microModule: MicroModule.Runtime,
    accept: String,
    multiple: Boolean,
    limit: Int
  ): List<String> {
    return when (val composeWindow = microModule.awaitComposeWindow()) {
      null -> emptyList()
      else -> {
        val fc = JFileChooser();
        fc.isMultiSelectionEnabled = multiple
        val fileNameFilter = acceptToNameFilter(accept)
        fc.addChoosableFileFilter(object : FileFilter() {
          override fun accept(file: File): Boolean {
            return fileNameFilter(file.name)
          }

          override fun getDescription(): String {
            return accept
          }
        })
        when (fc.showOpenDialog(composeWindow)) {
          JFileChooser.APPROVE_OPTION -> {
            fc.selectedFiles.map { it.absolutePath }
          }

          else -> emptyList()
        }
      }
    }
  }
}