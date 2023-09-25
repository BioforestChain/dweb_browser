package org.dweb_browser.browserUI.download

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import org.dweb_browser.browserUI.database.DeskAppInfoStore
import org.dweb_browser.browserUI.network.ApiService
import org.dweb_browser.browserUI.util.FilesUtil
import org.dweb_browser.browserUI.util.NotificationUtil
import org.dweb_browser.browserUI.util.ZipUtil
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.microservice.help.types.MMID
import java.io.File

internal interface IDwebBrowserBinder {
  suspend fun invokeDownloadAndSaveZip(downLoadInfo: DownLoadInfo)
  suspend fun invokeDownloadStatusChange(mmid: MMID)
  suspend fun invokeUpdateDownloadStatus(mmid: MMID, controller: DownLoadController)
  fun invokeFindDownLoadInfo(mmid: MMID): DownLoadInfo?
}

enum class DownLoadController { PAUSE, RESUME, CANCEL }

class DwebBrowserService : Service() {
  private val downloadMap = mutableMapOf<MMID, DownLoadInfo>() // 用于监听下载列表
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler // 用于全局的协程调用

  override fun onBind(intent: Intent?): IBinder {
    return DwebBrowserBinder()
  }

  //服务中的Binder对象，实现自定义接口IMyBinder，决定暴露那些方法给绑定该服务的Activity
  inner class DwebBrowserBinder : Binder(), IDwebBrowserBinder {
    //注意实现接口的方式
    override suspend fun invokeDownloadAndSaveZip(downLoadInfo: DownLoadInfo) {
      downloadAndSaveZip(downLoadInfo)//暴露给Activity的方法
    }

    override suspend fun invokeDownloadStatusChange(mmid: MMID) {
      downloadStatusChange(mmid)
    }

    override suspend fun invokeUpdateDownloadStatus(mmid: MMID, controller: DownLoadController) {
      updateDownloadStatus(mmid, controller)
    }

    override fun invokeFindDownLoadInfo(mmid: MMID) = findDownLoadInfo(mmid)
  }

  private fun findDownLoadInfo(mmid: MMID) = downloadMap[mmid]

  override fun onDestroy() {
    super.onDestroy()
    ioAsyncScope.cancel()
  }

  fun downloadAndSaveZip(downLoadInfo: DownLoadInfo): Boolean {
    // 1. 根据path进行下载，并且创建notification
    if (downloadMap.containsKey(downLoadInfo.id)) {
      ioAsyncScope.launch(mainAsyncExceptionHandler) {
        Toast.makeText(this@DwebBrowserService, "正在下载中，请稍后...", Toast.LENGTH_SHORT).show()
      }
      return false
    }
    downloadMap[downLoadInfo.id] = downLoadInfo
    NotificationUtil.INSTANCE.createNotificationForProgress(
      downLoadInfo.id, downLoadInfo.notificationId, downLoadInfo.name
    ) // 显示通知
    DownLoadObserver.emit(downLoadInfo.id, DownLoadStatus.DownLoading) // 同步更新所有注册
    ioAsyncScope.launch {
      try {
        ApiService.instance.downloadAndSave(
          url = downLoadInfo.url,
          file = File(downLoadInfo.path),
          downLoadInfo.metaData.bundle_size,
          isStop = {
            when (downLoadInfo.downLoadStatus) {
              DownLoadStatus.PAUSE -> {
                DownLoadObserver.emit(downLoadInfo.id, downLoadInfo.downLoadStatus)
                true
              }

              DownLoadStatus.CANCEL -> {
                DownLoadObserver.emit(downLoadInfo.id, downLoadInfo.downLoadStatus)
                downLoadInfo.downLoadStatus = DownLoadStatus.IDLE // 如果取消的话，那么就置为空
                true
              }

              else -> false
            }
          },
          onProgress = { current, total ->
            downLoadInfo.callDownLoadProgress(current, total)
          }
        )
      } catch (e: Exception) {
        Log.e("DwebBrowserService", "downloadAndSaveZip 下载失败: ${e.message}")
        downLoadInfo.downloadInstalled(false)
      }
    }
    return true
  }

  private suspend fun breakPointDownLoadAndSave(downLoadInfo: DownLoadInfo) {
    ioAsyncScope.launch {
      try {
        ApiService.instance.breakpointDownloadAndSave(
          url = downLoadInfo.url, file = File(downLoadInfo.path), total = downLoadInfo.size,
          isStop = {
            when (downLoadInfo.downLoadStatus) {
              DownLoadStatus.PAUSE -> {
                DownLoadObserver.emit(downLoadInfo.id, downLoadInfo.downLoadStatus)
                true
              }

              DownLoadStatus.CANCEL -> {
                DownLoadObserver.emit(downLoadInfo.id, downLoadInfo.downLoadStatus)
                downLoadInfo.downLoadStatus = DownLoadStatus.IDLE // 如果取消的话，那么就置为空
                true
              }

              else -> false
            }
          },
          onProgress = { current, total ->
            downLoadInfo.callDownLoadProgress(current, total)
          }
        )
      } catch (e: Exception) {
        Log.e("DwebBrowserService", "breakPointDownLoadAndSave 下载失败: ${e.message}")
        downLoadInfo.downloadInstalled(false)
      }
    }
  }

  private suspend fun DownLoadInfo.callDownLoadProgress(current: Long, total: Long) {
    if (current < 0) { // 专门针对下载异常情况，直接返回-1和0
      this.downLoadStatus = DownLoadStatus.FAIL
      DownLoadObserver.emit(this.id, DownLoadStatus.FAIL)
      downloadMap.remove(id) // 下载失败后也需要移除
      DownLoadObserver.close(id) // 同时移除当前mmid所有关联推送
      return
    }
    this.downLoadStatus = DownLoadStatus.DownLoading
    this.size = total
    this.dSize = current
    NotificationUtil.INSTANCE.updateNotificationForProgress(
      (current * 1.0 / total * 100).toInt(), notificationId
    )
    DownLoadObserver.emit(this.id, DownLoadStatus.DownLoading, current, total)

    if (current == total) {
      this.downLoadStatus = DownLoadStatus.DownLoadComplete
      // TODO 这边需要做到跳转
      /*NotificationUtil.INSTANCE.updateNotificationForProgress(
        100, notificationId, "下载完成"
      ) {
        NotificationUtil.INSTANCE.cancelNotification(notificationId)
        val intent = Intent(App.appContext, JmmManagerActivity::class.java).apply {
          action = JmmManagerActivity.ACTION_LAUNCH
          putExtra(JmmManagerActivity.KEY_JMM_METADATA, jmmMetadata)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
          `package` = BrowserUIApp.Instance.appContext.packageName
        }

        val pendingIntent =
          PendingIntent.getActivity(
            BrowserUIApp.Instance.appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )
        pendingIntent
      }*/
      NotificationUtil.INSTANCE.cancelNotification(notificationId) // 下载完成，隐藏通知栏
      DownLoadObserver.emit(this.id, DownLoadStatus.DownLoadComplete)
      runBlocking { delay(1000) }
      val unzip = ZipUtil.ergodicDecompress(
        this.path, FilesUtil.getAppUnzipPath(this@DwebBrowserService), mmid = id
      )
      downloadInstalled(unzip)
    }
  }

  /**
   * 用于下载完成，安装的结果处理
   */
  private suspend fun DownLoadInfo.downloadInstalled(success: Boolean) {
    if (success) {
      DeskAppInfoStore.saveAppInfo(id, metaData) // 保存的
      // 删除下面的方法，调用saveJmmMetadata时，会自动更新datastore，而datastore在jmmNMM中有执行了installApp
      DownLoadObserver.emit(this.id, DownLoadStatus.INSTALLED)
      this.downLoadStatus = DownLoadStatus.INSTALLED
    } else {
      DownLoadObserver.emit(this.id, DownLoadStatus.FAIL)
      this.downLoadStatus = DownLoadStatus.FAIL
    }
    downloadMap.remove(id) // 下载完成后需要移除
    DownLoadObserver.close(id) // 移除当前mmid所有关联推送
  }

  suspend fun downloadStatusChange(mmid: MMID) = downloadMap[mmid]?.apply {
    if (size == dSize) return@apply // 如果下载完成，就不执行操作
    when (this.downLoadStatus) {
      DownLoadStatus.PAUSE -> updateDownloadStatus(mmid, DownLoadController.RESUME)
      else -> updateDownloadStatus(mmid, DownLoadController.PAUSE)
    }
  }

  suspend fun updateDownloadStatus(mmid: MMID, controller: DownLoadController) =
    downloadMap[mmid]?.apply {
      if (size == dSize) return@apply
      when (controller) {
        DownLoadController.PAUSE -> if (this.downLoadStatus == DownLoadStatus.DownLoading) {
          this.downLoadStatus = DownLoadStatus.PAUSE
          NotificationUtil.INSTANCE.updateNotificationForProgress(
            (this.dSize * 1.0 / this.size * 100).toInt(), this.notificationId, "暂停"
          )
          DownLoadObserver.emit(this.id, downLoadStatus, dSize, size)
        }

        DownLoadController.RESUME -> if (this.downLoadStatus == DownLoadStatus.PAUSE) {
          this.downLoadStatus = DownLoadStatus.DownLoading
          NotificationUtil.INSTANCE.updateNotificationForProgress(
            (this.dSize * 1.0 / this.size * 100).toInt(), this.notificationId, "下载中"
          )
          DownLoadObserver.emit(this.id, downLoadStatus, dSize, size)
          breakPointDownLoadAndSave(this)
        } else {
          downloadMap.remove(this.id)
          downloadAndSaveZip(this)
        }

        DownLoadController.CANCEL -> if (this.downLoadStatus != DownLoadStatus.CANCEL) {
          this.downLoadStatus = DownLoadStatus.CANCEL
          downloadMap.remove(this.id)
          NotificationUtil.INSTANCE.cancelNotification(this.notificationId)
          DownLoadObserver.emit(this.id, downLoadStatus, dSize, size)
        }
      }
    }
}

/**
 * 比较版本的大小
 */
fun String.isGreaterThan(compare: String): Boolean {
  val thisSplit = this.split(".")
  val compareSplit = compare.split(".")
  val minLength = minOf(thisSplit.size, compareSplit.size)
  try {
    for (index in 0 until minLength) {
      val source = thisSplit[index].toInt()
      val target = compareSplit[index].toInt()
      if (source == target) continue
      return source > target // 除非一样，否则直接返回结果
    }
  } catch (e: Exception) {
    Log.e("DwebBrowserService", "isGreaterThan issue -> $this, $compare")
    return false
  }
  // 按照最小的长度判断，都相同时，则根据长度直接返回
  return thisSplit.size > compareSplit.size
}