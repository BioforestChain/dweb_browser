package info.bagen.dwebbrowser.microService.browser.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.JmmController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.download.DownLoadInfo
import org.dweb_browser.browserUI.download.DownLoadObserver
import org.dweb_browser.browserUI.download.DownLoadStatus
import org.dweb_browser.browserUI.download.compareAppVersionHigh
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.browserUI.util.NotificationUtil
import org.dweb_browser.microservice.help.JmmAppInstallManifest
import java.util.Calendar

data class JmmUIState(
  var downloadInfo: MutableState<DownLoadInfo>,
  val jmmAppInstallManifest: JmmAppInstallManifest,
)

sealed class JmmIntent {
  object ButtonFunction : JmmIntent()
  object DestroyActivity : JmmIntent()
}

class JmmManagerViewModel(
  jmmAppInstallManifest: JmmAppInstallManifest, private val jmmController: JmmController
) : ViewModel() {
  val uiState: JmmUIState
  private var downLoadObserver: DownLoadObserver? = null

  init {
    val downLoadInfo = createDownLoadInfoByJmm(jmmAppInstallManifest)
    uiState = JmmUIState(mutableStateOf(downLoadInfo), jmmAppInstallManifest)
    if (downLoadInfo.downLoadStatus != DownLoadStatus.INSTALLED) {
      downLoadObserver = DownLoadObserver(jmmAppInstallManifest.id)
      initDownLoadStatusListener()
    }
  }

  private fun initDownLoadStatusListener() {
    viewModelScope.launch(Dispatchers.IO) {
      downLoadObserver?.observe {
        if (it.downLoadStatus == DownLoadStatus.IDLE &&
          uiState.downloadInfo.value.downLoadStatus == DownLoadStatus.NewVersion
        ) {// TODO 为了规避更新被IDLE重置
          return@observe
        }

        when (it.downLoadStatus) {
          DownLoadStatus.DownLoading -> {
            uiState.downloadInfo.value = uiState.downloadInfo.value.copy(
              downLoadStatus = it.downLoadStatus,
              dSize = it.downLoadSize,
              size = it.totalSize
            )
          }

          else -> {
            uiState.downloadInfo.value = uiState.downloadInfo.value.copy(
              downLoadStatus = it.downLoadStatus
            )
          }
        }
        if (it.downLoadStatus == DownLoadStatus.INSTALLED) { // 移除监听列表
          downLoadObserver?.close()
        }
      }
    }
  }

  fun handlerIntent(action: JmmIntent) {
    viewModelScope.launch(Dispatchers.IO) {
      when (action) {
        is JmmIntent.ButtonFunction -> {
          when (uiState.downloadInfo.value.downLoadStatus) {
            DownLoadStatus.IDLE, DownLoadStatus.FAIL, DownLoadStatus.CANCEL, DownLoadStatus.NewVersion -> { // 空闲点击是下载，失败点击也是重新下载
              BrowserUIApp.Instance.mBinderService?.invokeDownloadAndSaveZip(
                uiState.downloadInfo.value
              )
            }

            DownLoadStatus.DownLoadComplete -> { /* TODO 无需响应 */
            }

            DownLoadStatus.DownLoading, DownLoadStatus.PAUSE -> {
              BrowserUIApp.Instance.mBinderService?.invokeDownloadStatusChange(
                uiState.downloadInfo.value.id//jmmMetadata.id
              )
            }

            DownLoadStatus.INSTALLED -> { // 点击打开app触发的事件
              //jmmController?.openApp(uiState.downloadInfo.value.jmmMetadata.id)
              jmmController?.openApp(uiState.downloadInfo.value.id)
            }
          }
        }

        is JmmIntent.DestroyActivity -> {
          downLoadObserver?.close()
        }
      }
    }
  }

  private fun createDownLoadInfoByJmm(jmmAppInstallManifest: JmmAppInstallManifest): DownLoadInfo {
    return jmmController.getApp(jmmAppInstallManifest.id)?.let { curJmmMetadata ->
      if (compareAppVersionHigh(curJmmMetadata.version, jmmAppInstallManifest.version)) {
        DownLoadInfo(
          id = jmmAppInstallManifest.id,
          url = jmmAppInstallManifest.bundle_url,
          name = jmmAppInstallManifest.name,
          downLoadStatus = DownLoadStatus.NewVersion,
          path = "${App.appContext.cacheDir}/DL_${jmmAppInstallManifest.id}_${Calendar.MILLISECOND}.bfsa",
          notificationId = (NotificationUtil.notificationId++),
          metaData = jmmAppInstallManifest,
        )
      } else {
        DownLoadInfo(
          id = jmmAppInstallManifest.id,
          url = jmmAppInstallManifest.bundle_url,
          name = jmmAppInstallManifest.name,
          downLoadStatus = DownLoadStatus.INSTALLED
        )
      }
    } ?: run {
      DownLoadInfo(
        id = jmmAppInstallManifest.id,
        url = jmmAppInstallManifest.bundle_url,
        name = jmmAppInstallManifest.name,
        downLoadStatus = DownLoadStatus.IDLE,
        path = "${App.appContext.cacheDir}/DL_${jmmAppInstallManifest.id}_${Calendar.MILLISECOND}.bfsa",
        notificationId = (NotificationUtil.notificationId++),
        metaData = jmmAppInstallManifest,
      )
    }
  }
}
