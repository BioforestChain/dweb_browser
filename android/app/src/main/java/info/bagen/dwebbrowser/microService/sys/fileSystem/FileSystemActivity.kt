package info.bagen.dwebbrowser.microService.sys.fileSystem

import android.Manifest
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.base.BaseThemeActivity
import info.bagen.dwebbrowser.microService.sys.fileSystem.FileSystemController.Companion.controller
import kotlinx.coroutines.launch

class FileSystemActivity : BaseThemeActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleScope.launch {
      controller.granted =
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      finish()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    controller.granted = null
  }
}