package org.dweb_browser.sys.filechooser

import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile
import org.dweb_browser.core.module.MicroModule

actual class FileChooserManage actual constructor() {
  actual suspend fun openFileChooser(
    microModule: MicroModule.Runtime,
    accept: String,
    multiple: Boolean
  ): List<String> {
    val pickerType = when (accept) {
      "image/*" -> PickerType.Image
      "video/*" -> PickerType.Video
      else -> PickerType.File()
    }

    if (multiple) {
      val pickFiles = FileKit.pickFile(pickerType, mode = PickerMode.Multiple())

      return pickFiles?.map {
        it.file.absolutePath
      } ?: emptyList()
    } else {
      val pickFile = FileKit.pickFile(pickerType)

      return pickFile?.file?.absolutePath?.let { listOf(it) } ?: emptyList()
    }
  }

  suspend fun openFileChooser(
    microModule: MicroModule.Runtime,
    accept: List<String>,
    multiple: Boolean
  ): List<String> {
    val pickerType = PickerType.File(extensions = accept)

    if (multiple) {
      val pickFiles = FileKit.pickFile(pickerType, mode = PickerMode.Multiple())

      return pickFiles?.map {
        it.file.absolutePath
      } ?: emptyList()
    } else {
      val pickFile = FileKit.pickFile(pickerType)

      return pickFile?.file?.absolutePath?.let { listOf(it) } ?: emptyList()
    }
  }

  suspend fun openFolderChooser(initialDirectory: String?) =
    FileKit.pickDirectory(initialDirectory = initialDirectory)?.file?.absolutePath ?: ""
}