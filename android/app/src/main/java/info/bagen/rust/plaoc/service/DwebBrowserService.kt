package info.bagen.rust.plaoc.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import info.bagen.rust.plaoc.network.ApiService
import info.bagen.rust.plaoc.util.FilesUtil
import info.bagen.rust.plaoc.util.ZipUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.broadcast.BFSBroadcastAction
import info.bagen.rust.plaoc.broadcast.BFSBroadcastReceiver
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.ui.*
import info.bagen.rust.plaoc.util.NotificationUtil
import kotlinx.coroutines.*
import java.io.File

internal interface IDwebBrowserBinder {
  fun invokeDownloadAndSaveZip(downLoadInfo: DownLoadInfo)
  fun invokeDownloadStatusChange(mmid: Mmid)
  fun invokeGetDownLoadInfo(jmmMetadata: JmmMetadata): DownLoadInfo?
}

class DwebBrowserService : Service() {
  companion object {
    val poDownLoadStatus = mutableMapOf<Mmid, PromiseOut<DownLoadStatus>>()
  }

  private val downloadMap = mutableMapOf<String, DownLoadInfo>()

  private var bfsBroadcastReceiver: BFSBroadcastReceiver? = null

  override fun onBind(intent: Intent?): IBinder {
    return DwebBrowserBinder()
  }

  override fun onCreate() {
    super.onCreate()
    registerBFSBroadcastReceiver()
  }

  override fun onDestroy() {
    unRegisterBFSBroadcastReceiver()
    super.onDestroy()
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

    override fun invokeGetDownLoadInfo(jmmMetadata: JmmMetadata): DownLoadInfo? {
      return getDownLoadInfo(jmmMetadata)
    }
  }

  private fun getDownLoadInfo(jmmMetadata: JmmMetadata): DownLoadInfo? {
    return downloadMap[jmmMetadata.id]
  }

  private fun registerBFSBroadcastReceiver() {
    val intentFilter = IntentFilter()
    intentFilter.addAction(BFSBroadcastAction.BFSInstallApp.action)
    intentFilter.addAction(BFSBroadcastAction.DownLoadStatusChanged.action)
    bfsBroadcastReceiver = BFSBroadcastReceiver()
    registerReceiver(bfsBroadcastReceiver, intentFilter)
  }

  private fun unRegisterBFSBroadcastReceiver() {
    bfsBroadcastReceiver?.let { unregisterReceiver(it) }
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun downloadAndSaveZip(downLoadInfo: DownLoadInfo): Boolean {
    // 1. 根据path进行下载，并且创建notification
    if (downloadMap.containsKey(downLoadInfo.jmmMetadata!!.id)) {
      Toast.makeText(this, "正在下载中，请稍后...", Toast.LENGTH_SHORT).show()
      return true
    }
    downloadMap[downLoadInfo.jmmMetadata!!.id] = downLoadInfo
    NotificationUtil.INSTANCE.createNotificationForProgress(
      downLoadInfo.jmmMetadata!!.id,
      downLoadInfo.notificationId,
      downLoadInfo.jmmMetadata!!.title
    ) // 显示通知
    App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
      JmmIntent.UpdateDownLoadStatus(DownLoadStatus.DownLoading)
    )
    GlobalScope.launch(Dispatchers.IO) {
      ApiService.instance.downloadAndSave(
        downLoadInfo.jmmMetadata!!.downloadUrl, File(downLoadInfo.path),
        isStop = { downLoadInfo.downLoadStatus == DownLoadStatus.PAUSE },
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
      ApiService.instance.breakpointDownloadAndSave(
        downLoadInfo.jmmMetadata!!.downloadUrl, File(downLoadInfo.path), downLoadInfo.size,
        isStop = { downLoadInfo.downLoadStatus == DownLoadStatus.PAUSE },
        DLProgress = { current, total ->
          downLoadInfo.callDownLoadProgress(current, total)
        }
      )
    }
  }

  private fun DownLoadInfo.callDownLoadProgress(current: Long, total: Long) {
    this.size = total
    this.dSize = current
    NotificationUtil.INSTANCE.updateNotificationForProgress(
      (current * 1.0 / total * 100).toInt(), notificationId
    )
    App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
      JmmIntent.UpdateDownLoadProgress(current, total)
    )

    if (current == total) {
      jmmMetadata?.let { jmmMetadata ->
        downloadMap.remove(jmmMetadata.id) // 下载完成后需要移除
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
            PendingIntent.getActivity(App.appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
          pendingIntent
        }
        App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
          JmmIntent.UpdateDownLoadStatus(DownLoadStatus.DownLoadComplete)
        ) ?: { // 如果完成后发现下载界面没有打开，手动打开
          JmmManagerActivity.startActivity(jmmMetadata)
        }
        runBlocking { delay(2000) }
        val unzip =
          ZipUtil.ergodicDecompress(this.path, FilesUtil.getAppUnzipPath(), mmid = jmmMetadata.id)
        if (unzip) {
          App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
            JmmIntent.UpdateDownLoadStatus(DownLoadStatus.INSTALLED)
          )
          poDownLoadStatus[jmmMetadata.id]?.resolve(DownLoadStatus.INSTALLED)
        } else {
          App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
            JmmIntent.UpdateDownLoadStatus(DownLoadStatus.FAIL)
          )
          poDownLoadStatus[jmmMetadata.id]?.resolve(DownLoadStatus.FAIL)
        }
        poDownLoadStatus.remove(jmmMetadata.id)
      }
    }
  }

  fun downloadStatusChange(mmid: Mmid) {
    downloadMap[mmid]?.apply {
      if (size == dSize) return@apply // 如果下载完成，就不执行操作
      when (this.downLoadStatus) {
        DownLoadStatus.PAUSE -> {
          this.downLoadStatus = DownLoadStatus.DownLoading
          NotificationUtil.INSTANCE.updateNotificationForProgress(
            (this.dSize * 1.0 / this.size * 100).toInt(), this.notificationId, "下载中"
          )
          App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
            JmmIntent.UpdateDownLoadStatus(this.downLoadStatus)
          )
          breakPointDownLoadAndSave(this)
        }
        else -> {
          this.downLoadStatus = DownLoadStatus.PAUSE
          NotificationUtil.INSTANCE.updateNotificationForProgress(
            (this.dSize * 1.0 / this.size * 100).toInt(), this.notificationId, "暂停"
          )
          App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
            JmmIntent.UpdateDownLoadStatus(this.downLoadStatus)
          )
        }
      }
    }
  }
}