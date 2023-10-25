package org.dweb_browser.browser.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.jmm.JmmController
import org.dweb_browser.browser.jmm.JmmNMM
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.sys.download.JmmDownloadStatus
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.helper.falseAlso

internal val LocalShowWebViewVersion = compositionLocalOf {
  mutableStateOf(false)
}
internal val LocalShowWebViewHelper = compositionLocalOf {
  mutableStateOf(false)
}

internal val LocalJmmViewHelper = compositionLocalOf<JmmManagerViewHelper> {
  noLocalProvidedFor("LocalJmmViewHelper")
}
internal val LocalJmmNMM = compositionLocalOf<JmmNMM> {
  noLocalProvidedFor("JmmNMM")
}

data class JmmUIState(
  val jmmAppInstallManifest: JmmAppInstallManifest,
  val downloadSize: MutableState<Long> = mutableLongStateOf(0L),
  val downloadStatus: MutableState<JmmDownloadStatus> = mutableStateOf(JmmDownloadStatus.Init)
)

class JmmManagerViewHelper(
  jmmAppInstallManifest: JmmAppInstallManifest, private val jmmController: JmmController
) {
  val uiState: JmmUIState = JmmUIState(jmmAppInstallManifest)
  private val jmmNMM = jmmController.jmmNMM

  fun startDownload() = jmmNMM.ioAsyncScope.launch {
    val taskId = jmmController.createDownloadTask(uiState.jmmAppInstallManifest.bundle_url)
    jmmController.taskId = taskId
    jmmController.watchProcess(taskId) {
      println("watch=> ${this.status.state.name} ${this.status.current}")

      if (this.status.state == DownloadState.Downloading) {
        uiState.downloadSize.value = this.status.current
        uiState.downloadStatus.value = JmmDownloadStatus.Downloading
      }
      // 下载完成触发解压
      if (this.status.state == DownloadState.Completed) {
        val success = jmmController.decompress(this)
        if (success) {
          uiState.downloadStatus.value = JmmDownloadStatus.INSTALLED
        } else {
          uiState.downloadSize.value = 0L
          uiState.downloadStatus.value = JmmDownloadStatus.Failed
        }
      }
    }
    // 已经注册完监听了，开始
    jmmController.start()
  }

  fun pause() = jmmNMM.ioAsyncScope.launch {
    jmmController.pause().falseAlso {
      uiState.downloadStatus.value = JmmDownloadStatus.Failed
    }
  }

  fun start() = jmmNMM.ioAsyncScope.launch {
    jmmController.start().falseAlso {
      uiState.downloadStatus.value = JmmDownloadStatus.Failed
    }
  }

  fun open() = jmmNMM.ioAsyncScope.launch {
    jmmController.openApp(uiState.jmmAppInstallManifest.id)
  }

}
