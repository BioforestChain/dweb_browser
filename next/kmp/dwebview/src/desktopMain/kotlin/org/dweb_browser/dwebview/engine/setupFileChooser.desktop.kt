package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.callback.OpenFileCallback
import com.teamdev.jxbrowser.browser.callback.OpenFilesCallback
import com.teamdev.jxbrowser.browser.callback.OpenFolderCallback
import kotlinx.coroutines.launch
import org.dweb_browser.sys.filechooser.FileChooserManage
import java.nio.file.Path

fun setupFileChooser(engine: DWebViewEngine) {
  val fileChooserManage = FileChooserManage()
  engine.browser.set(OpenFileCallback::class.java, OpenFileCallback { params, tell ->
    val acceptableExtensions: List<String> = params.acceptableExtensions()

    engine.lifecycleScope.launch {
      val pickerFiles =
        fileChooserManage.openFileChooser(engine.remoteMM, acceptableExtensions, false)
      pickerFiles.firstOrNull()?.let {
        tell.open(Path.of(it))
      } ?: tell.cancel()
    }
  })

  engine.browser.set(OpenFilesCallback::class.java, OpenFilesCallback { params, tell ->
    val acceptableExtensions: List<String> = params.acceptableExtensions()
    engine.lifecycleScope.launch {
      val pickerFiles =
        fileChooserManage.openFileChooser(engine.remoteMM, acceptableExtensions, true)

      if (pickerFiles.isEmpty()) tell.cancel() else
        tell.open(*pickerFiles.map { Path.of(it) }.toTypedArray())
    }
  })

  engine.browser.set(OpenFolderCallback::class.java, OpenFolderCallback { params, tell ->
    engine.lifecycleScope.launch {
      val folder = fileChooserManage.openFolderChooser(params.suggestedDirectory())

      if (folder.isEmpty()) tell.cancel() else tell.open(Path.of(folder))
    }
  })
}