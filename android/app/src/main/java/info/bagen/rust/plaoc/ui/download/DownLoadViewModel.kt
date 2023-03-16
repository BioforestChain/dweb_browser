package info.bagen.rust.plaoc.ui.download

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.jmm.DownLoadObserver
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.ui.DownLoadStatus
import info.bagen.rust.plaoc.microService.sys.jmm.ui.createDownLoadInfoByJmm
import info.bagen.rust.plaoc.ui.entity.AppInfo
import info.bagen.rust.plaoc.ui.view.DialogInfo
import info.bagen.rust.plaoc.util.DwebBrowserUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class DownLoadUIState(
  val downLoadState: MutableState<DownLoadStatus> = mutableStateOf(DownLoadStatus.IDLE),
  val downLoadProgress: MutableState<Float> = mutableStateOf(0f),
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

class DownLoadViewModel(val mmid: Mmid, val url: String) : ViewModel() {
  val uiState = mutableStateOf(DownLoadUIState())

  private val jmmMetadata: JmmMetadata
  private val downLoadObserver: DownLoadObserver

  init {
    jmmMetadata = JmmMetadata(
      id = mmid,
      server = JmmMetadata.MainServer(
        root = "file:///bundle",
        entry = "/cotDemo.worker.js"
      ),
      downloadUrl = url,
      title = "测试",
      subtitle = "测试",
      icon = "https://www.bfmeta.info/imgs/logo3.webp",
      images = listOf(
      "http://qiniu-waterbang.waterbang.top/bfm/cot-home_2058.webp",
      "http://qiniu-waterbang.waterbang.top/bfm/defi.png",
      "http://qiniu-waterbang.waterbang.top/bfm/nft.png"
      )
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
          DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadAndSaveZip(
            createDownLoadInfoByJmm(jmmMetadata)
          )
        }
        is DownLoadIntent.DownLoadStatusChange -> {
          DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadStatusChange(mmid)
        }
      }
    }
  }
}
