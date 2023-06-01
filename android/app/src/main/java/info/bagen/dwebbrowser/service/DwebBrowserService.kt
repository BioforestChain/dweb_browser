package info.bagen.dwebbrowser.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import info.bagen.dwebbrowser.microService.sys.jmm.ui.DownLoadInfo
import info.bagen.dwebbrowser.microService.sys.jmm.ui.DownLoadStatus
import info.bagen.dwebbrowser.network.ApiService
import info.bagen.dwebbrowser.util.FilesUtil
import info.bagen.dwebbrowser.util.ZipUtil
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.sys.jmm.ui.JmmManagerActivity
import info.bagen.dwebbrowser.datastore.JmmMetadataDB
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.mainAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.runBlockingCatching
import info.bagen.dwebbrowser.microService.sys.jmm.DownLoadObserver
import info.bagen.dwebbrowser.microService.sys.jmm.debugJMM
import info.bagen.dwebbrowser.microService.sys.mwebview.dwebServiceWorker.DownloadControllerEvent
import info.bagen.dwebbrowser.microService.sys.mwebview.dwebServiceWorker.emitEvent
import info.bagen.dwebbrowser.util.NotificationUtil
import kotlinx.coroutines.*
import java.io.File

internal interface IDwebBrowserBinder {
  fun invokeDownloadAndSaveZip(downLoadInfo: DownLoadInfo)
  fun invokeDownloadStatusChange(mmid: Mmid)
  fun invokeUpdateDownloadStatus(mmid: Mmid, controller: DownLoadController)
}

enum class DownLoadController{ PAUSE, RESUME, CANCEL }

class DwebBrowserService : Service() {
  private val downloadMap = mutableMapOf<Mmid, DownLoadInfo>() // 用于监听下载列表

  override fun onBind(intent: Intent?): IBinder {
    return DwebBrowserBinder()
  }

  //服务中的Binder对象，实现自定义接口IMyBinder，决定暴露那些方法给绑定该服务的Activity
  inner class DwebBrowserBinder : Binder(), IDwebBrowserBinder {
    //注意实现接口的方式
    override fun invokeDownloadAndSaveZip(downLoadInfo: DownLoadInfo) {
      downloadAndSaveZip(downLoadInfo)//暴露给Activity的方法
    }

    override fun invokeDownloadStatusChange(mmid: Mmid) {
      downloadStatusChange(mmid)
    }

    override fun invokeUpdateDownloadStatus(mmid: Mmid, controller: DownLoadController) {
      updateDownloadStatus(mmid, controller)
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun downloadAndSaveZip(downLoadInfo: DownLoadInfo): Boolean {
    // 1. 根据path进行下载，并且创建notification
    if (downloadMap.containsKey(downLoadInfo.jmmMetadata.id)) {
      GlobalScope.launch(mainAsyncExceptionHandler) {
        Toast.makeText(this@DwebBrowserService, "正在下载中，请稍后...", Toast.LENGTH_SHORT).show()
      }
      return true
    }
    downloadMap[downLoadInfo.jmmMetadata.id] = downLoadInfo
    NotificationUtil.INSTANCE.createNotificationForProgress(
      downLoadInfo.jmmMetadata.id, downLoadInfo.notificationId, downLoadInfo.jmmMetadata.title
    ) // 显示通知
    DownLoadObserver.emit(downLoadInfo.jmmMetadata.id, DownLoadStatus.DownLoading) // 同步更新所有注册
    GlobalScope.launch(Dispatchers.IO) {
      sendStatusToEmitEvent(downLoadInfo.jmmMetadata.id, DownloadControllerEvent.Start.event) // 通知前台，开始下载
      ApiService.instance.downloadAndSave(
        downLoadInfo.jmmMetadata.downloadUrl, File(downLoadInfo.path),
        isStop = {
          when (downLoadInfo.downLoadStatus) {
            DownLoadStatus.PAUSE -> {
              DownLoadObserver.emit(downLoadInfo.jmmMetadata.id, downLoadInfo.downLoadStatus)
              sendStatusToEmitEvent(downLoadInfo.jmmMetadata.id, DownloadControllerEvent.Pause.event) // 通知前台，暂停下载
              true
            }
            DownLoadStatus.CANCEL -> {
              DownLoadObserver.emit(downLoadInfo.jmmMetadata.id, downLoadInfo.downLoadStatus)
              sendStatusToEmitEvent(downLoadInfo.jmmMetadata.id, DownloadControllerEvent.Cancel.event) // 通知前台，取消下载
              downLoadInfo.downLoadStatus = DownLoadStatus.IDLE // 如果取消的话，那么就置为空
              true
            }
            else -> false
          }
        },
        DLProgress = { current, total ->
          downLoadInfo.callDownLoadProgress(current, total)
        }
      )
    }
    return true
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun breakPointDownLoadAndSave(downLoadInfo: DownLoadInfo) {
    GlobalScope.launch(Dispatchers.IO) {
      sendStatusToEmitEvent(downLoadInfo.jmmMetadata.id, DownloadControllerEvent.Start.event) // 通知前台，开始下载
      ApiService.instance.breakpointDownloadAndSave(
        downLoadInfo.jmmMetadata.downloadUrl, File(downLoadInfo.path), downLoadInfo.size,
        isStop = {
          when (downLoadInfo.downLoadStatus) {
            DownLoadStatus.PAUSE -> {
              DownLoadObserver.emit(downLoadInfo.jmmMetadata.id, downLoadInfo.downLoadStatus)
              sendStatusToEmitEvent(downLoadInfo.jmmMetadata.id, DownloadControllerEvent.Pause.event) // 通知前台，暂停下载
              true
            }
            DownLoadStatus.CANCEL -> {
              DownLoadObserver.emit(downLoadInfo.jmmMetadata.id, downLoadInfo.downLoadStatus)
              sendStatusToEmitEvent(downLoadInfo.jmmMetadata.id, DownloadControllerEvent.Cancel.event) // 通知前台，取消下载
              downLoadInfo.downLoadStatus = DownLoadStatus.IDLE // 如果取消的话，那么就置为空
              true
            }
            else -> false
          }
        },
        DLProgress = { current, total ->
          downLoadInfo.callDownLoadProgress(current, total)
        }
      )
    }
  }

  private fun DownLoadInfo.callDownLoadProgress(current: Long, total: Long) {
    if (current < 0) { // 专门针对下载异常情况，直接返回-1和0
      this.downLoadStatus = DownLoadStatus.FAIL
      DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.FAIL)
      sendStatusToEmitEvent(this.jmmMetadata.id, DownloadControllerEvent.End.event)
      downloadMap.remove(jmmMetadata.id) // 下载失败后也需要移除
      DownLoadObserver.close(jmmMetadata.id) // 同时移除当前mmid所有关联推送
      return
    }
    this.downLoadStatus = DownLoadStatus.DownLoading
    this.size = total
    this.dSize = current
    NotificationUtil.INSTANCE.updateNotificationForProgress(
      (current * 1.0 / total * 100).toInt(), notificationId
    )
    DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.DownLoading, current, total)
    val mmid = this.jmmMetadata.id
    sendStatusToEmitEvent(mmid, DownloadControllerEvent.Progress.event, "${(current * 1.0 / total * 100).toInt()}") // 通知前台，下载进度

    if (current == total) {
      this.downLoadStatus = DownLoadStatus.DownLoadComplete
      NotificationUtil.INSTANCE.updateNotificationForProgress(
        100, notificationId, "下载完成"
      ) {
        NotificationUtil.INSTANCE.cancelNotification(notificationId)
        val intent = Intent(App.appContext, JmmManagerActivity::class.java).apply {
          action = JmmManagerActivity.ACTION_LAUNCH
          putExtra(JmmManagerActivity.KEY_JMM_METADATA, jmmMetadata)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
          `package` = App.appContext.packageName
        }

        val pendingIntent =
          PendingIntent.getActivity(App.appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        pendingIntent
      }
      DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.DownLoadComplete)
      runBlocking { delay(1000) }
      val unzip =
        ZipUtil.ergodicDecompress(this.path, FilesUtil.getAppUnzipPath(), mmid = jmmMetadata.id)
      if (unzip) {
        JmmMetadataDB.saveJmmMetadata(jmmMetadata.id, jmmMetadata)
        // 删除下面的方法，调用saveJmmMetadata时，会自动更新datastore，而datastore在jmmNMM中有执行了installApp
        DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.INSTALLED)
        sendStatusToEmitEvent(this.jmmMetadata.id, DownloadControllerEvent.End.event)
        this.downLoadStatus = DownLoadStatus.INSTALLED
      } else {
        DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.FAIL)
        sendStatusToEmitEvent(this.jmmMetadata.id, DownloadControllerEvent.End.event)
        this.downLoadStatus = DownLoadStatus.FAIL
      }
      downloadMap.remove(jmmMetadata.id) // 下载完成后需要移除
      DownLoadObserver.close(jmmMetadata.id) // 移除当前mmid所有关联推送
    }
  }

  fun downloadStatusChange(mmid: Mmid) = downloadMap[mmid]?.apply {
    if (size == dSize) return@apply // 如果下载完成，就不执行操作
    when (this.downLoadStatus) {
      DownLoadStatus.PAUSE -> updateDownloadStatus(mmid, DownLoadController.RESUME)
      else -> updateDownloadStatus(mmid, DownLoadController.PAUSE)
    }
  }

  fun updateDownloadStatus(mmid: Mmid, controller: DownLoadController) = downloadMap[mmid]?.apply {
    if (size == dSize) return@apply
    when (controller) {
      DownLoadController.PAUSE -> if (this.downLoadStatus == DownLoadStatus.DownLoading) {
        this.downLoadStatus = DownLoadStatus.PAUSE
        NotificationUtil.INSTANCE.updateNotificationForProgress(
          (this.dSize * 1.0 / this.size * 100).toInt(), this.notificationId, "暂停"
        )
        DownLoadObserver.emit(this.jmmMetadata.id, downLoadStatus, dSize, size)
      }
      DownLoadController.RESUME -> if (this.downLoadStatus == DownLoadStatus.PAUSE) {
        this.downLoadStatus = DownLoadStatus.DownLoading
        NotificationUtil.INSTANCE.updateNotificationForProgress(
          (this.dSize * 1.0 / this.size * 100).toInt(), this.notificationId, "下载中"
        )
        DownLoadObserver.emit(this.jmmMetadata.id, downLoadStatus, dSize, size)
        breakPointDownLoadAndSave(this)
      } else {
        downloadMap.remove(this.jmmMetadata.id)
        downloadAndSaveZip(this)
      }
      DownLoadController.CANCEL -> if (this.downLoadStatus != DownLoadStatus.CANCEL) {
        this.downLoadStatus = DownLoadStatus.CANCEL
        NotificationUtil.INSTANCE.cancelNotification(this.notificationId)
        DownLoadObserver.emit(this.jmmMetadata.id, downLoadStatus, dSize, size)
      }
    }
  }

  private fun sendStatusToEmitEvent(mmid: Mmid, eventName: String, data: String = "") {
//    debugJMM("sendStatusToEmitEvent=>","mmid=>$mmid eventName=>$eventName data=>$data")
    runBlockingCatching(ioAsyncExceptionHandler) {
      emitEvent(mmid, eventName, data) // 通知前台，下载进度
    }
  }
}

fun compareAppVersionHigh(localVersion: String, compareVersion: String): Boolean {
  var localSplit = localVersion.split(".")
  val compareSplit = compareVersion.split(".")
  var tempLocalVersion = localVersion
  if (localSplit.size < compareSplit.size) {
    val cha = compareSplit.size - localSplit.size
    for (i in 0 until cha) {
      tempLocalVersion += ".0"
    }
    localSplit = tempLocalVersion.split(".")
  }
  try {
    for (i in compareSplit.indices) {
      val local = Integer.parseInt(localSplit[i])
      val compare = Integer.parseInt(compareSplit[i])
      if (compare > local) return true
    }
  } catch (e: Throwable) {
    Log.e("DwebBrowserService", "compareAppVersionHigh issue -> $localVersion, $compareVersion")
  }
  return false
}
