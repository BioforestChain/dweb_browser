package org.dweb_browser.browserUI.download

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.dweb_browser.browserUI.ui.entity.AppInfo
import org.dweb_browser.browserUI.ui.view.DialogInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.helper.MMID
import java.util.Calendar

data class DownLoadUIState(
  val downLoadState: MutableState<DownLoadStatus> = mutableStateOf(DownLoadStatus.IDLE),
  val downLoadProgress: MutableState<Float> = mutableFloatStateOf(0f),
  var dialogInfo: DialogInfo = DialogInfo(),
  var downloadAppInfo: AppInfo? = null,
)

data class DownLoadProgress(
  var current: Long = 0L,
  var total: Long = 0L,
  var progress: Float = 0f,
  var downloadFile: String = "",
  var downloadUrl: String = ""
)

private fun DownLoadProgress.update(
  current: Long? = null,
  total: Long? = null,
  progress: Float? = null,
  downloadFile: String? = null,
  downloadUrl: String? = null
) {
  this.current = current ?: this.current
  this.total = total ?: this.total
  this.progress = progress ?: this.progress
  this.downloadFile = downloadFile ?: this.downloadFile
  this.downloadUrl = downloadUrl ?: this.downloadUrl
}

/**
 * MIV中的Intent部分
 */
sealed class DownLoadIntent {
  object DownLoad : DownLoadIntent()
  object DownLoadStatusChange : DownLoadIntent()

}

class DownLoadViewModel(val mmid: MMID, val url: String) : ViewModel() {
  val uiState = mutableStateOf(DownLoadUIState())

  private val downLoadInfo: DownLoadInfo
  private val downLoadObserver: DownLoadObserver

  init {
    downLoadInfo = DownLoadInfo(
      id = mmid,
      url = url,
      name = "",
      path = "${BrowserUIApp.Instance.appContext.cacheDir}/DL_${mmid}_${Calendar.MILLISECOND}.bfsa",
      downLoadStatus = DownLoadStatus.IDLE
    )
    downLoadObserver = DownLoadObserver(mmid)
    initDownLoadListener()
  }

  private fun initDownLoadListener() {
    viewModelScope.launch(Dispatchers.IO) {
      downLoadObserver.observe {
        uiState.value.downLoadState.value = it.downLoadStatus
        if (it.downLoadStatus == DownLoadStatus.DownLoading) {
          uiState.value.downLoadProgress.value = it.downLoadSize / it.totalSize * 1.0f
        }
        if (it.downLoadStatus == DownLoadStatus.INSTALLED) {
          downLoadObserver.close()
        }
      }
    }
  }

  fun handleIntent(action: DownLoadIntent) {
    viewModelScope.launch(Dispatchers.IO) {
      when (action) {
        is DownLoadIntent.DownLoad -> {
          BrowserUIApp.Instance.mBinderService?.invokeDownloadAndSaveZip(
            //createDownLoadInfoByJmm(jmmMetadata)
            downLoadInfo
          )
        }
        is DownLoadIntent.DownLoadStatusChange -> {
          BrowserUIApp.Instance.mBinderService?.invokeDownloadStatusChange(mmid)
        }
      }
    }
  }
}