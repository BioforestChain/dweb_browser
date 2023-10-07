package org.dweb_browser.browser.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import org.dweb_browser.browser.download.DownloadProgressEvent
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest
import org.dweb_browser.microservice.std.dns.nativeFetch
import org.dweb_browser.microservice.sys.download.JmmDownloadController
import org.dweb_browser.microservice.sys.download.JmmDownloadStatus

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
  val downloadStatus: MutableState<JmmDownloadStatus> = mutableStateOf(JmmDownloadStatus.IDLE)
)

sealed class JmmIntent {
  data object ButtonFunction : JmmIntent()
  data object DestroyActivity : JmmIntent()
}

class JmmManagerViewHelper(
  jmmAppInstallManifest: JmmAppInstallManifest,
  private val jmmController: org.dweb_browser.browser.jmm.JmmController
) {
  val uiState: JmmUIState = JmmUIState(jmmAppInstallManifest)
  //private var downLoadObserver: DownLoadObserver? = null

  init {
    jmmController.jmmNMM.ioAsyncScope.launch {
      /// TODO 重新实现这个DownloadNMM
      val queryProgress =
        jmmController.jmmNMM.nativeFetch("file://download.browser.dweb/progress")
      if (queryProgress.isOk()) {
        val bodyStream = queryProgress.stream()

        bodyStream.getReader("getDownloadProgress").consumeEachJsonLine<DownloadProgressEvent> {
          uiState.downloadSize.value = current
          uiState.downloadStatus.value = when (state) {
            DownloadState.Init -> JmmDownloadStatus.IDLE
            DownloadState.Downloading -> JmmDownloadStatus.DownLoading
            DownloadState.Paused -> JmmDownloadStatus.PAUSE
            DownloadState.Canceld -> JmmDownloadStatus.CANCEL
            DownloadState.Failed -> JmmDownloadStatus.FAIL
            DownloadState.Completed -> JmmDownloadStatus.DownLoadComplete
          }
        }
      } else {
        jmmController.getApp(jmmAppInstallManifest.id)?.let { curJmmMetadata ->
          if (jmmAppInstallManifest.version.isGreaterThan(curJmmMetadata.version)) {
            uiState.downloadStatus.value = JmmDownloadStatus.NewVersion
          } else {
            uiState.downloadStatus.value = JmmDownloadStatus.INSTALLED
          }
        } ?: run { uiState.downloadStatus.value = JmmDownloadStatus.IDLE }

        if (uiState.downloadStatus.value != JmmDownloadStatus.INSTALLED) {
          jmmController.win.coroutineScope.launch {
            initDownLoadStatusListener()
          }
        }
      }
    }
//    BrowserUIApp.Instance.mBinderService?.invokeFindDownLoadInfo(jmmAppInstallManifest.id)?.let {
//      uiState.downloadSize.value = it.dSize
//      uiState.downloadStatus.value = it.downloadStatus
//    } ?: jmmController.getApp(jmmAppInstallManifest.id)?.let { curJmmMetadata ->
//      if (jmmAppInstallManifest.version.isGreaterThan(curJmmMetadata.version)) {
//        uiState.downloadStatus.value = JmmDownloadStatus.NewVersion
//      } else {
//        uiState.downloadStatus.value = JmmDownloadStatus.INSTALLED
//      }
//    } ?: run { uiState.downloadStatus.value = JmmDownloadStatus.IDLE }
//
//    if (uiState.downloadStatus.value != JmmDownloadStatus.INSTALLED) {
//      jmmController.win.coroutineScope.launch {
//        initDownLoadStatusListener()
//      }
//    }
  }

  private fun initDownLoadStatusListener() {
    jmmController.onDownload { downloadInfo ->
      if (downloadInfo.id != uiState.jmmAppInstallManifest.id) return@onDownload
      if (downloadInfo.downloadStatus == JmmDownloadStatus.IDLE) return@onDownload

      when (downloadInfo.downloadStatus) {
        JmmDownloadStatus.DownLoading -> {
          uiState.downloadStatus.value = downloadInfo.downloadStatus
          uiState.downloadSize.value = downloadInfo.dSize
        }

        else -> {
          uiState.downloadStatus.value = downloadInfo.downloadStatus
        }
      }
      if (downloadInfo.downloadStatus == JmmDownloadStatus.INSTALLED) { // 移除监听列表
        // downLoadObserver?.close()
        // TODO 移除监听
      }
    }
  }

  suspend fun handlerIntent(action: JmmIntent) {
    when (action) {
      is JmmIntent.ButtonFunction -> {
        when (uiState.downloadStatus.value) {
          JmmDownloadStatus.IDLE, JmmDownloadStatus.FAIL, JmmDownloadStatus.CANCEL, JmmDownloadStatus.NewVersion -> { // 空闲点击是下载，失败点击也是重新下载
            jmmController.downloadAndSaveZip(uiState.jmmAppInstallManifest)
          }

          JmmDownloadStatus.DownLoadComplete -> { /* TODO 无需响应 */
          }

          JmmDownloadStatus.DownLoading -> {
            jmmController.updateDownloadState(JmmDownloadController.PAUSE)
          }

          JmmDownloadStatus.PAUSE -> {
            jmmController.updateDownloadState(JmmDownloadController.RESUME)
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
