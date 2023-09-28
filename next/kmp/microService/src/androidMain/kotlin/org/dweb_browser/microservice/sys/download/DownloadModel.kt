package org.dweb_browser.microservice.sys.download

import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.APP_DIR_TYPE
import org.dweb_browser.helper.FilesUtil
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ZipUtil
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.sys.download.db.DownloadDBStore
import java.util.concurrent.atomic.AtomicInteger

@Serializable
enum class DownloadController { PAUSE, RESUME, CANCEL }

@Serializable
enum class DownloadStatus {
  IDLE, DownLoading, DownLoadComplete, PAUSE, INSTALLED, FAIL, CANCEL, NewVersion;

  /*fun toServiceWorkerEvent() =
    when (this) {
      IDLE -> DownloadControllerEvent.Start.event
      PAUSE -> DownloadControllerEvent.Pause.event
      CANCEL -> DownloadControllerEvent.Cancel.event
      DownLoading -> DownloadControllerEvent.Progress.event
      else -> DownloadControllerEvent.End.event
    }*/
}

@Serializable
data class DownloadInfo(
  val id: MMID,
  val url: String, // 下载地址
  val name: String, // 软件名称
  var path: String = "", // 文件下载路径
  var notificationId: Int = 0, // 通知栏的id
  var size: Long = 0L, // 文件大小
  var dSize: Long = 1L, // 已下载大小
  // var progress: Float = 0f, // 进度 0~1
  var downloadStatus: DownloadStatus = DownloadStatus.IDLE, // 标记当前下载状态
  val metaData: JmmAppInstallManifest, // 保存app数据，如jmmMetadata
) {
  fun toEvent(): String {
    return ""
  }

  fun toData(): String {
    return ""
  }
}

class DownloadModel(val downloadNMM: DownloadNMM) {
  private val downloadAppMap = mutableMapOf<MMID, DownloadInfo>() // 用于监听下载列表
  private val downloadingMap = mutableMapOf<MMID, Boolean>() // 用于判断是否正在下载，便于更好的实现断点续传问题

  companion object {
    private var notificationId = AtomicInteger(999)
  }

  fun findDownloadApp(mmid: MMID) = downloadAppMap[mmid]
  fun exists(mmid: MMID) = downloadAppMap.containsKey(mmid)

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler // 用于全局的协程调用
  private val downloadSignal: Signal<DownloadInfo> = Signal()
  val onDownload = downloadSignal.toListener()

  internal suspend fun downloadApp(context: Context, jmm: JmmAppInstallManifest): Boolean {
    if (downloadingMap.containsKey(jmm.id)) return false
    if (downloadAppMap.containsKey(jmm.id)) downloadingMap[jmm.id] = true
    val downloadInfo = jmm.toDownloadInfo(context)
    downloadAppMap[jmm.id] = downloadInfo
    HttpDownload.downloadAndSave(downloadInfo, isStop = {
      when (downloadInfo.downloadStatus) {
        DownloadStatus.CANCEL, DownloadStatus.FAIL, DownloadStatus.PAUSE -> true
        else -> false
      }
    }) { current, total ->
      debugDownload("Downloading", "current=$current, total=$total")
      ioAsyncScope.launch {
        downloadInfo.callDownLoadProgress(context, current, total)
      }
    }
    return true
  }

  internal suspend fun breakDownloadApp() {

  }

  internal fun updateDownloadState(mmid: MMID, event: DownloadController) {
    downloadAppMap[mmid]?.apply {
      downloadStatus = when (event) {
        DownloadController.CANCEL -> DownloadStatus.CANCEL
        DownloadController.RESUME -> DownloadStatus.DownLoading
        DownloadController.PAUSE -> DownloadStatus.PAUSE
    }
    }
  }

  private suspend fun DownloadInfo.callDownLoadProgress(
    context: Context, current: Long, total: Long
  ) {
    if (current < 0) { // 专门针对下载异常情况，直接返回-1和0
      this.downloadStatus = DownloadStatus.FAIL
      downloadSignal.emit(this)

      // 下载失败后也需要移除
      downloadAppMap.remove(id)
      downloadingMap.remove(id)
      return
    }
    this.downloadStatus = DownloadStatus.DownLoading
    this.size = total
    this.dSize = current
    downloadSignal.emit(this)
    if (current == total) { // 下载完成，隐藏通知栏
      this.downloadStatus = DownloadStatus.DownLoadComplete
      delay(1000)
      val unzip = ZipUtil.ergodicDecompress(
        this.path, FilesUtil.getAppUnzipPath(context, APP_DIR_TYPE.SystemApp), mmid = id
      )
      downloadInstalled(context, unzip)
    }
  }

  /**
   * 用于下载完成，安装的结果处理
   */
  private suspend fun DownloadInfo.downloadInstalled(context: Context, success: Boolean) {
    if (success) {
      DownloadDBStore.saveAppInfo(context, id, metaData) // 保存的
      // 删除下面的方法，调用saveJmmMetadata时，会自动更新datastore，而datastore在jmmNMM中有执行了installApp
      this.downloadStatus = DownloadStatus.INSTALLED
      downloadSignal.emit(this)
    } else {
      this.downloadStatus = DownloadStatus.FAIL
      downloadSignal.emit(this)
    }
    downloadAppMap.remove(id) // 下载完成后需要移除
    downloadingMap.remove(id)
  }


  private fun JmmAppInstallManifest.toDownloadInfo(context: Context) = DownloadInfo(
    id = id,
    url = bundle_url,
    name = name,
    size = bundle_size,
    downloadStatus = DownloadStatus.IDLE,
    path = "${context.cacheDir.absolutePath}/DL_${id}_${System.currentTimeMillis()}.zip",
    notificationId = notificationId.addAndGet(1),
    metaData = this,
  )
}