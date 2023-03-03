package info.bagen.rust.plaoc.microService.sys.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.util.DwebBrowserUtil
import info.bagen.rust.plaoc.util.NotificationUtil
import java.text.SimpleDateFormat
import java.util.*

data class JmmUIState(
  var downloadInfo: MutableState<DownLoadInfo> = mutableStateOf(DownLoadInfo())
)

enum class DownLoadStatus {
  IDLE, DownLoading, Install, PAUSE, OPEN, FAIL
}

data class DownLoadInfo(
  var jmmMetadata: JmmMetadata? = null,
  var path: String = "", // 文件下载路径
  var notificationId: Int = 0, // 通知栏的id
  var size: Long = 0L, // 文件大小
  var dSize: Long = 1L, // 已下载大小
  // var progress: Float = 0f, // 进度 0~1
  var downLoadStatus: DownLoadStatus = DownLoadStatus.IDLE, // 标记当前下载状态
)

sealed class JmmIntent {
  object ButtonFunction : JmmIntent()
  class SetTypeAndJmmMetaData(val jmmMetadata: JmmMetadata) : JmmIntent()
  class UpdateDownLoadProgress(val current: Long, val total: Long) : JmmIntent()
  class UpdateDownLoadStatus(val downLoadStatus: DownLoadStatus) : JmmIntent()
}

private val currentTime = {
  val simpleDateFormat = SimpleDateFormat("yyyy-mm-dd-hh:MM:ss")
  simpleDateFormat.format(Date())
}

class JmmManagerViewModel : ViewModel() {
  val uiState = JmmUIState()

  fun handlerIntent(action: JmmIntent) {

    when (action) {
      is JmmIntent.SetTypeAndJmmMetaData -> {
        val downLoadInfo = DwebBrowserUtil.INSTANCE.mBinderService?.invokeGetDownLoadInfo(
          action.jmmMetadata.downloadUrl
        ) ?: DownLoadInfo(
          jmmMetadata = action.jmmMetadata,
          path = "${App.appContext.cacheDir}/DL_${action.jmmMetadata.title}-${currentTime}.bfsa",
          notificationId = (NotificationUtil.notificationId++),
        )
        uiState.downloadInfo.value = uiState.downloadInfo.value.copy(
          jmmMetadata = action.jmmMetadata,
          path = downLoadInfo.path,
          notificationId = downLoadInfo.notificationId,
          size = downLoadInfo.size,
          dSize = downLoadInfo.dSize,
          downLoadStatus = downLoadInfo.downLoadStatus,
        )
      }
      is JmmIntent.ButtonFunction -> {
        when (uiState.downloadInfo.value.downLoadStatus) {
          DownLoadStatus.IDLE -> {
            DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadAndSaveZip(uiState.downloadInfo.value)
          }
          DownLoadStatus.Install -> { /* TODO 无需响应 */ }
          DownLoadStatus.DownLoading, DownLoadStatus.PAUSE -> {
            DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadStatusChange(
              uiState.downloadInfo.value.jmmMetadata?.downloadUrl ?: ""
            )
          }
          DownLoadStatus.FAIL -> { // 按钮显示重新下载
            DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadAndSaveZip(uiState.downloadInfo.value)
          }
          DownLoadStatus.OPEN -> {

          }
        }
      }
      is JmmIntent.UpdateDownLoadProgress -> {
        uiState.downloadInfo.value =
          uiState.downloadInfo.value.copy(
            size = action.total, dSize = action.current, downLoadStatus = DownLoadStatus.DownLoading
          )
      }
      is JmmIntent.UpdateDownLoadStatus -> {
        uiState.downloadInfo.value =
          uiState.downloadInfo.value.copy(downLoadStatus = action.downLoadStatus)
      }
    }

  }
}