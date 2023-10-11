package org.dweb_browser.browser.download

import io.ktor.http.URLBuilder
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.MimeTypes
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.sys.download.db.DownloadDBStore
import org.dweb_browser.helper.APP_DIR_TYPE
import org.dweb_browser.helper.FilesUtil
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ZipUtil
import org.dweb_browser.helper.ioAsyncExceptionHandler


data class DownloadTask(
  /** 下载编号 */
  val id: String,
  /** 下载链接 */
  val url: String,
  /** 创建时间 */
  val createTime: Long,
  /** 来源模块 */
  val originMmid: MMID,
  /** 来源链接 */
  val originUrl: String?,
  /** 下载回调链接 */
  val completeCallbackUrl: String?,
  /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
  val mime: String,
  /** 标记当前下载状态 */
  val status: DownloadProgressEvent,
) {
  /** 文件路径 */
  val filepath: String = getFilePath()
  private fun getFilePath(): String {
    // 放到各个模块各自下载文件夹下，这些模块可能来自网络
    return "$originMmid/download${url.substring(url.lastIndexOf("/"))}"
  }
}

@Serializable
enum class DownloadState {
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
}

@Serializable
data class DownloadProgressEvent(
  var current: Long = 0,
  var total: Long = 1,
  var state: DownloadState = DownloadState.Init
)

class DownloadController(val mm: DownloadNMM) {
  fun openDownloadWindow() {

  }

  val downloadTaskMap = mutableMapOf<MMID, DownloadTask>() // 用于监听下载列表
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler // 用于全局的协程调用
  private val downloadSignal: Signal<DownloadTask> = Signal()
  val onDownload = downloadSignal.toListener()

  internal suspend fun downloadFactory(downloadTask: DownloadTask): Boolean {
    when (downloadTask.status.state) {
      DownloadState.Paused -> { // 如果找到，并且状态是暂停的，直接修改状态为下载，即可继续下载
        downloadTask.status.state = DownloadState.Downloading
      }

      DownloadState.Downloading -> { // 如果状态是正在下载的，不进行任何变更
      }

      else -> { // 其他状态直接重新下载即可
        HttpDownload.downloadAndSave(
          downloadInfo = downloadTask,
          isStop = {
            if (downloadTask.status.state == DownloadState.Canceld ||
              downloadTask.status.state == DownloadState.Failed
            ) {
              downloadTaskMap.remove(downloadTask.id)
              true
            } else {
              false
            }
          },
          isPause = {
            downloadTask.status.state == DownloadState.Paused
          }) { current, total ->
          ioAsyncScope.launch {
            downloadTask.callDownLoadProgress(current, total)
          }
        }
      }
    }
    return true
  }

  internal fun DownloadTask.updateDownloadState(event: DownloadState) {
    status.state = event
  }

  private suspend fun DownloadTask.callDownLoadProgress(
    current: Long, total: Long
  ) {
    if (current < 0) { // 专门针对下载异常情况，直接返回-1和0
      this.status.state = DownloadState.Init
      downloadSignal.emit(this)

      // 下载失败后也需要移除
      downloadTaskMap.remove(id)
      return
    }
    this.status.state = DownloadState.Downloading
    this.status.total = total
    this.status.current = current
    downloadSignal.emit(this)
    if (current == total) { // 下载完成，隐藏通知栏
      this.status.state = DownloadState.Completed
      jmmFactory()
    }
  }

  /**
   * 针对JMM模块做特殊处理，因为jmm是下载app的zip,因此需要解压到systemApp
   */
  private suspend fun DownloadTask.jmmFactory() {
    if (this.originMmid == "jmm.browser.dweb" && mime == MimeTypes.getMimeType(".zip")) {
      mm.nativeFetch(URLBuilder("file://file.std.dweb/unCompress").apply{
        parameters["sourcePath"] = filepath
        parameters["targetPath"] = FileNMM.getVirtualFsPath(mm,"system-app").fsFullPath.toString()
      }.buildString())
    }
  }

  /**
   * 用于下载完成，安装的结果处理
   */
//  private suspend fun DownloadTask.installed(success: Boolean) {
//    if (success) {
//      DownloadDBStore.saveAppInfo(id) // 保存
//      this.status.state = DownloadState.Completed
//      downloadSignal.emit(this)
//    } else {
//      this.status.state = DownloadState.Init
//      downloadSignal.emit(this)
//    }
//    downloadTaskMap.remove(id) // 下载完成后需要移除
//  }
}