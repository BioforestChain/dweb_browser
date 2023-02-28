package info.bagen.rust.plaoc.microService.sys.jmm.ui

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.util.DwebBrowserUtil
import info.bagen.rust.plaoc.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class JmmUIState(
  val currentType: MutableState<TYPE> = mutableStateOf(TYPE.MALL),
  var jmmMetadata: JmmMetadata? = null,
  val downloadInfo: MutableState<DownLoadInfo> = mutableStateOf(DownLoadInfo())
)

data class DownLoadInfo(
  var url: String = "", // 文件下载地址
  var name: String = "", // 文件名字
  var path: String = "", // 文件下载路径
  var notificationId: Int = 0, // 通知栏的id
  var size: Long = 0L, // 文件大小
  var dSize: Long = 0L, // 已下载大小
  // var progress: Float = 0f, // 进度 0~1
  var pause: Boolean = false, // 用于标记当前下载状态
)

enum class TYPE { MALL/*应用商店*/, INSTALL, UNINSTALL }

sealed class JmmIntent {
  object DownLoadAndSave : JmmIntent()
  class SetTypeAndJmmMetaData(val type: TYPE, val jmmMetadata: JmmMetadata?) : JmmIntent()
  class UpdateDownLoadProgress(val current: Long, val total: Long) : JmmIntent()
  class UpdateDownLoadStatus(val pause: Boolean) : JmmIntent()
}

class JmmManagerViewModel : ViewModel() {
  val uiState = JmmUIState()

  fun handlerIntent(action: JmmIntent) {
    viewModelScope.launch(Dispatchers.IO) {
      when (action) {
        is JmmIntent.SetTypeAndJmmMetaData -> {
          Log.e("lin.huang", "JmmManagerViewModel:SetTypeAndJmmMetaData -> ${action.jmmMetadata}")
          action.jmmMetadata?.let {
            uiState.jmmMetadata = it
            val simpleDateFormat = SimpleDateFormat("yyyy-mm-dd-hh:MM:ss")
            val time = simpleDateFormat.format(Date())
            uiState.downloadInfo.value = uiState.downloadInfo.value.copy(
              url = it.main_url,
              name = it.title,
              path = "${App.appContext.cacheDir}/download_${it.title}-$time.bfsa",
              notificationId = (NotificationUtil.notificationId++)
            )
            Log.e(
              "lin.huang",
              "JmmManagerViewModel:SetTypeAndJmmMetaData downloadInfo-> ${uiState.downloadInfo}"
            )
          }
          uiState.currentType.value = action.type
        }
        is JmmIntent.DownLoadAndSave -> {
          Log.e("lin.huang", "JmmManagerViewModel:DownLoadAndSave -> ${uiState.downloadInfo}")
          DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadAndSaveZip(uiState.downloadInfo.value)
        }
        is JmmIntent.UpdateDownLoadProgress -> {
          Log.d("JmmManagerViewModel", "UpdateDownLoadProgress->${action.current},${action.total}")
          uiState.downloadInfo.value =
            uiState.downloadInfo.value.copy(size = action.total, dSize = action.current)
        }
        is JmmIntent.UpdateDownLoadStatus -> {
          uiState.downloadInfo.value = uiState.downloadInfo.value.copy(pause = action.pause)
        }
      }
    }
  }
}