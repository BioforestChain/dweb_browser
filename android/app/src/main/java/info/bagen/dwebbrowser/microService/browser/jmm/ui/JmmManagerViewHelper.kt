package info.bagen.dwebbrowser.microService.browser.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.microService.browser.jmm.JmmController
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.download.isGreaterThan
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest
import org.dweb_browser.microservice.sys.download.DownloadController
import org.dweb_browser.microservice.sys.download.DownloadStatus

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
  val downloadStatus: MutableState<DownloadStatus> = mutableStateOf(DownloadStatus.IDLE)
)

sealed class JmmIntent {
  data object ButtonFunction : JmmIntent()
  data object DestroyActivity : JmmIntent()
}

class JmmManagerViewHelper(
  jmmAppInstallManifest: JmmAppInstallManifest, private val jmmController: JmmController
) {
  val uiState: JmmUIState = JmmUIState(jmmAppInstallManifest)
  //private var downLoadObserver: DownLoadObserver? = null

  init {
    BrowserUIApp.Instance.mBinderService?.invokeFindDownLoadInfo(jmmAppInstallManifest.id)?.let {
      uiState.downloadSize.value = it.dSize
      uiState.downloadStatus.value = it.downloadStatus
    } ?: jmmController.getApp(jmmAppInstallManifest.id)?.let { curJmmMetadata ->
      if (jmmAppInstallManifest.version.isGreaterThan(curJmmMetadata.version)) {
        uiState.downloadStatus.value = DownloadStatus.NewVersion
      } else {
        uiState.downloadStatus.value = DownloadStatus.INSTALLED
      }
    } ?: run { uiState.downloadStatus.value = DownloadStatus.IDLE }

    if (uiState.downloadStatus.value != DownloadStatus.INSTALLED) {
      jmmController.win.coroutineScope.launch {
        initDownLoadStatusListener()
      }
    }
  }

  private fun initDownLoadStatusListener() {
    jmmController.onDownload { downloadInfo ->
      if (downloadInfo.id != uiState.jmmAppInstallManifest.id) return@onDownload
      if (downloadInfo.downloadStatus == DownloadStatus.IDLE) return@onDownload

      when (downloadInfo.downloadStatus) {
        DownloadStatus.DownLoading -> {
          uiState.downloadStatus.value = downloadInfo.downloadStatus
          uiState.downloadSize.value = downloadInfo.dSize
        }

        else -> {
          uiState.downloadStatus.value = downloadInfo.downloadStatus
        }
      }
      if (downloadInfo.downloadStatus == DownloadStatus.INSTALLED) { // 移除监听列表
        // downLoadObserver?.close()
        // TODO 移除监听
      }
    }
  }

  suspend fun handlerIntent(action: JmmIntent) {
    when (action) {
      is JmmIntent.ButtonFunction -> {
        when (uiState.downloadStatus.value) {
          DownloadStatus.IDLE, DownloadStatus.FAIL, DownloadStatus.CANCEL, DownloadStatus.NewVersion -> { // 空闲点击是下载，失败点击也是重新下载
            jmmController.downloadAndSaveZip(uiState.jmmAppInstallManifest)
          }

          DownloadStatus.DownLoadComplete -> { /* TODO 无需响应 */
          }

          DownloadStatus.DownLoading -> {
            jmmController.updateDownloadState(DownloadController.PAUSE)
          }

          DownloadStatus.PAUSE -> {
            jmmController.updateDownloadState(DownloadController.RESUME)
          }

          /*DownloadStatus.DownLoading, DownloadStatus.PAUSE -> {
            jmmController.downloadAndSaveZip(uiState.jmmAppInstallManifest)
            BrowserUIApp.Instance.mBinderService?.invokeDownloadStatusChange(
              uiState.jmmAppInstallManifest.id
            )
          }*/

          DownloadStatus.INSTALLED -> { // 点击打开app触发的事件
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
