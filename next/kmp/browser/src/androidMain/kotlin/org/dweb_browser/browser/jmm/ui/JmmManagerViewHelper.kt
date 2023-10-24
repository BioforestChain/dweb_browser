package org.dweb_browser.browser.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.jmm.JmmController
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.sys.download.JmmDownloadStatus
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.noLocalProvidedFor

internal val LocalShowWebViewVersion = compositionLocalOf {
  mutableStateOf(false)
}
internal val LocalShowWebViewHelper = compositionLocalOf {
  mutableStateOf(false)
}

internal val LocalJmmViewHelper = compositionLocalOf<JmmManagerViewHelper> {
  noLocalProvidedFor("LocalJmmViewHelper")
}

data class JmmUIState(
  val jmmAppInstallManifest: JmmAppInstallManifest,
  val downloadSize: MutableState<Long> = mutableLongStateOf(0L),
  val downloadStatus: MutableState<JmmDownloadStatus> = mutableStateOf(JmmDownloadStatus.Init)
)

sealed class JmmIntent {
  data object ButtonFunction : JmmIntent()
  data object DestroyActivity : JmmIntent()
}

class JmmManagerViewHelper(
  jmmAppInstallManifest: JmmAppInstallManifest,
  private val jmmController: JmmController
) {
  val uiState: JmmUIState = JmmUIState(jmmAppInstallManifest)
   val viewHandler = Signal<Pair<String,DownloadState>>()
  val onDownload = viewHandler.toListener()

  suspend fun handlerIntent(action: JmmIntent) {
    when (action) {
      is JmmIntent.ButtonFunction -> {
        when (uiState.downloadStatus.value) {
          JmmDownloadStatus.Init,JmmDownloadStatus.Failed,JmmDownloadStatus.Canceld, JmmDownloadStatus.NewVersion -> { // 空闲点击是下载，失败点击也是重新下载
            val taskId = jmmController.createDownloadTask(uiState.jmmAppInstallManifest.bundle_url)
            jmmController.taskId = taskId
            jmmController.watchProcess(taskId) {
              println("watch=> ${this.status.state.name} ${this.status.current}")

              if (this.status.state == DownloadState.Downloading) {
              }
              // 下载完成触发解压
              if(this.status.state == DownloadState.Completed) {
                jmmController.unCompress(this)
              }
            }
            // 已经注册完监听了，开始
            jmmController.start(taskId)
          }

          JmmDownloadStatus.Completed -> { /* TODO 无需响应 */
          }

          JmmDownloadStatus.Downloading -> {
//            val success = jmmController.updateDownloadState(
//              JmmDownloadController.PAUSE
//            )
//            if (success) {
//              uiState.downloadStatus.value = JmmDownloadStatus.Paused
//            }
          }

          JmmDownloadStatus.Paused -> {
//            val success =jmmController.updateDownloadState(
//              JmmDownloadController.RESUME
//            )
//            if (success) {
//              uiState.downloadStatus.value = JmmDownloadStatus.Downloading
//            }
          }

          /*DownloadStatus.DownLoading, DownloadStatus.PAUSE -> {
            jmmController.downloadAndSaveZip(uiState.jmmAppInstallManifest)
            BrowserUIApp.Instance.mBinderService?.invokeDownloadStatusChange(
              uiState.jmmAppInstallManifest.id
            )
          }*/

          JmmDownloadStatus.INSTALLED -> { // 点击打开app触发的事件
            jmmController.openApp(uiState.jmmAppInstallManifest.id)
          }
        }
      }

      is JmmIntent.DestroyActivity -> {
        // downLoadObserver?.close()
        // TODO 移除监听
      }
    }
  }
}
