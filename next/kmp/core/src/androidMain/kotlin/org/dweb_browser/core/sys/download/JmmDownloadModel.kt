package org.dweb_browser.core.sys.download

import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.sys.download.db.DownloadDBStore
import org.dweb_browser.helper.APP_DIR_TYPE
import org.dweb_browser.helper.FilesUtil
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ZipUtil
import org.dweb_browser.helper.ioAsyncExceptionHandler
import java.util.concurrent.atomic.AtomicInteger

@Serializable
enum class JmmDownloadStatus {
  /** 初始化中，做下载前的准备，包括寻址、创建文件、保存任务等工作 */
  Init,

  /** 下载中*/
  Downloading,

  /** 暂停下载*/
  Paused,

  /** 取消下载*/
  Canceld,

  /** 下载失败*/
  Failed,

  /** 下载完成*/
  Completed,

  /**安装中*/
  INSTALLED,

  /** 新版本*/
  NewVersion;
}

@Serializable
data class JmmDownloadInfo(
  val id: MMID,
  val url: String, // 下载地址
  val name: String, // 软件名称
  var path: String = "", // 文件下载路径
  var notificationId: Int = 0, // 通知栏的id
  var size: Long = 0L, // 文件大小
  var dSize: Long = 1L, // 已下载大小
  // var progress: Float = 0f, // 进度 0~1
  var downloadStatus: JmmDownloadStatus = JmmDownloadStatus.Init, // 标记当前下载状态
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
  private val downloadAppMap = mutableMapOf<MMID, JmmDownloadInfo>() // 用于监听下载列表

  companion object {
    private var notificationId = AtomicInteger(999)
  }

  fun exists(mmid: MMID) = downloadAppMap.containsKey(mmid)

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler // 用于全局的协程调用
  private val downloadSignal: Signal<JmmDownloadInfo> = Signal()
  val onDownload = downloadSignal.toListener()

  internal suspend fun downloadApp(context: Context, jmm: JmmAppInstallManifest): Boolean {
    val downloadInfo = downloadAppMap.getOrPut(jmm.id) {
      jmm.toDownloadInfo(context)
    }
    when (downloadInfo.downloadStatus) {
      JmmDownloadStatus.Paused -> { // 如果找到，并且状态是暂停的，直接修改状态为下载，即可继续下载
        downloadInfo.downloadStatus = JmmDownloadStatus.Downloading
      }

      JmmDownloadStatus.Downloading -> { // 如果状态是正在下载的，不进行任何变更
      }

      else -> { // 其他状态直接重新下载即可
        HttpDownload.downloadAndSave(
          downloadInfo = downloadInfo,
          isStop = {
            if (downloadInfo.downloadStatus == JmmDownloadStatus.Canceld ||
              downloadInfo.downloadStatus == JmmDownloadStatus.Failed
            ) {
              downloadAppMap.remove(jmm.id)
              true
            } else {
              false
            }
          },
          isPause = {
            // debugDownload("Downloading", "${jmm.id}, downloadStatus=${downloadInfo.downloadStatus}")
            downloadInfo.downloadStatus == JmmDownloadStatus.Paused
          }) { current, total ->
          // debugDownload("Downloading", "current=$current, total=$total")
          ioAsyncScope.launch {
            downloadInfo.callDownLoadProgress(context, current, total)
          }
        }
      }
    }
    return true
  }

  private suspend fun JmmDownloadInfo.callDownLoadProgress(
    context: Context, current: Long, total: Long
  ) {
    if (current < 0) { // 专门针对下载异常情况，直接返回-1和0
      this.downloadStatus = JmmDownloadStatus.Failed
      downloadSignal.emit(this)

      // 下载失败后也需要移除
      downloadAppMap.remove(id)
      return
    }
    this.downloadStatus = JmmDownloadStatus.Downloading
    this.size = total
    this.dSize = current
    downloadSignal.emit(this)
    if (current == total) { // 下载完成，隐藏通知栏
      this.downloadStatus = JmmDownloadStatus.Completed
      delay(1000)
      val unzip = ZipUtil.ergodicDecompress(
        this.path,
        FilesUtil.getAppUnzipPath(context, APP_DIR_TYPE.SystemApp),
        isDeleted = false,
        mmid = id
      )
      downloadInstalled(context, unzip)
    }
  }

  /**
   * 用于下载完成，安装的结果处理
   */
  private suspend fun JmmDownloadInfo.downloadInstalled(context: Context, success: Boolean) {
    if (success) {
      DownloadDBStore.saveAppInfo(context, id, metaData) // 保存的
      // 删除下面的方法，调用saveJmmMetadata时，会自动更新datastore，而datastore在jmmNMM中有执行了installApp
      this.downloadStatus = JmmDownloadStatus.Completed
      downloadSignal.emit(this)
    } else {
      this.downloadStatus = JmmDownloadStatus.Failed
      downloadSignal.emit(this)
    }
    downloadAppMap.remove(id) // 下载完成后需要移除
  }


  private fun JmmAppInstallManifest.toDownloadInfo(context: Context) = JmmDownloadInfo(
    id = id,
    url = bundle_url,
    name = name,
    size = bundle_size,
    downloadStatus = JmmDownloadStatus.Init,
    path = "${context.cacheDir.absolutePath}/DL_${id}_${System.currentTimeMillis()}.zip",
    notificationId = notificationId.addAndGet(1),
    metaData = this,
  )
}