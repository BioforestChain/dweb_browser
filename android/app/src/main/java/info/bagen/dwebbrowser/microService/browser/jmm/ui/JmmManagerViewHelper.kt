package info.bagen.dwebbrowser.microService.browser.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.JmmController
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.download.DownLoadInfo
import org.dweb_browser.browserUI.download.DownLoadObserver
import org.dweb_browser.browserUI.download.DownLoadStatus
import org.dweb_browser.browserUI.download.isGreaterThan
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.browserUI.util.NotificationUtil
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest
import java.util.Calendar

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
  val downloadStatus: MutableState<DownLoadStatus> = mutableStateOf(DownLoadStatus.IDLE)
)

sealed class JmmIntent {
  data object ButtonFunction : JmmIntent()
  data object DestroyActivity : JmmIntent()
}

class JmmManagerViewHelper(
  jmmAppInstallManifest: JmmAppInstallManifest, private val jmmController: JmmController
) {
  val uiState: JmmUIState = JmmUIState(jmmAppInstallManifest)
  private var downLoadObserver: DownLoadObserver? = null

  init {
    BrowserUIApp.Instance.mBinderService?.invokeFindDownLoadInfo(jmmAppInstallManifest.id)?.let {
      uiState.downloadSize.value = it.dSize
      uiState.downloadStatus.value = it.downLoadStatus
    } ?: jmmController.getApp(jmmAppInstallManifest.id)?.let { curJmmMetadata ->
      if (jmmAppInstallManifest.version.isGreaterThan(curJmmMetadata.version)) {
        uiState.downloadStatus.value = DownLoadStatus.NewVersion
      } else {
        uiState.downloadStatus.value = DownLoadStatus.INSTALLED
      }
    } ?: run { uiState.downloadStatus.value = DownLoadStatus.IDLE }

    if (uiState.downloadStatus.value != DownLoadStatus.INSTALLED) {
      jmmController.win.coroutineScope.launch {
        initDownLoadStatusListener()
      }
    }
  }

  private suspend fun initDownLoadStatusListener() {
    downLoadObserver = DownLoadObserver(uiState.jmmAppInstallManifest.id).also { observe ->
      observe.observe {
        if (it.downLoadStatus == DownLoadStatus.IDLE) return@observe

        when (it.downLoadStatus) {
          DownLoadStatus.DownLoading -> {
            uiState.downloadStatus.value = it.downLoadStatus
            uiState.downloadSize.value = it.downLoadSize
          }

          else -> {
            uiState.downloadStatus.value = it.downLoadStatus
          }
        }
        if (it.downLoadStatus == DownLoadStatus.INSTALLED) { // 移除监听列表
          downLoadObserver?.close()
        }
      }
    }
  }

  suspend fun handlerIntent(action: JmmIntent) {
    when (action) {
      is JmmIntent.ButtonFunction -> {
        when (uiState.downloadStatus.value) {
          DownLoadStatus.IDLE, DownLoadStatus.FAIL, DownLoadStatus.CANCEL, DownLoadStatus.NewVersion -> { // 空闲点击是下载，失败点击也是重新下载
            BrowserUIApp.Instance.mBinderService?.invokeDownloadAndSaveZip(
              uiState.jmmAppInstallManifest.toDownLoadInfo()
            )
          }

          DownLoadStatus.DownLoadComplete -> { /* TODO 无需响应 */
          }

          DownLoadStatus.DownLoading, DownLoadStatus.PAUSE -> {
            BrowserUIApp.Instance.mBinderService?.invokeDownloadStatusChange(
              uiState.jmmAppInstallManifest.id
            )
          }

          DownLoadStatus.INSTALLED -> { // 点击打开app触发的事件
            jmmController.openApp(uiState.jmmAppInstallManifest.id)
          }
        }
      }

      is JmmIntent.DestroyActivity -> {
        downLoadObserver?.close()
      }
    }
  }

  private fun JmmAppInstallManifest.toDownLoadInfo() = DownLoadInfo(
    id = id,
    url = bundle_url,
    name = name,
    downLoadStatus = DownLoadStatus.IDLE,
    path = "${App.appContext.cacheDir}/DL_${id}_${Calendar.MILLISECOND}.bfsa",
    notificationId = (NotificationUtil.notificationId++),
    metaData = this,
  )
}
