package info.bagen.rust.plaoc.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import info.bagen.libappmgr.network.ApiService
import info.bagen.libappmgr.utils.FilesUtil
import info.bagen.libappmgr.utils.ZipUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.broadcast.BFSBroadcastAction
import info.bagen.rust.plaoc.broadcast.BFSBroadcastReceiver
import info.bagen.rust.plaoc.microService.sys.jmm.ui.*
import info.bagen.rust.plaoc.util.NotificationUtil
import kotlinx.coroutines.*
import java.io.File

internal interface IDwebBrowserBinder {
  fun invokeDownloadAndSaveZip(downLoadInfo: DownLoadInfo)
  fun invokeDownloadStatusChange(url: String)
  fun invokeGetDownLoadInfo(url: String): DownLoadInfo?
}

class DwebBrowserService : Service() {
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

    override fun invokeDownloadStatusChange(url: String) {
      downloadStatusChange(url)
    }

    override fun invokeGetDownLoadInfo(url: String): DownLoadInfo? {
      return getDownLoadInfo(url)
    }
  }

  private fun getDownLoadInfo(url: String): DownLoadInfo? {
    return downloadMap[url]
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
    if (downloadMap.containsKey(downLoadInfo.jmmMetadata!!.downloadUrl)) {
      Toast.makeText(this, "正在下载中，请稍后...", Toast.LENGTH_SHORT).show()
      return true
    }
    downloadMap[downLoadInfo.jmmMetadata!!.downloadUrl] = downLoadInfo
    NotificationUtil.INSTANCE.createNotificationForProgress(
      downLoadInfo.jmmMetadata!!.downloadUrl,
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
        downloadMap.remove(jmmMetadata.downloadUrl)
        NotificationUtil.INSTANCE.updateNotificationForProgress(
          100, notificationId, "下载完成"
        ) {
          val intent = Intent(App.appContext, JmmManagerActivity::class.java).apply {
            putExtra(JmmManagerActivity.KEY_JMM_METADATA, jmmMetadata)
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
        val unzip = ZipUtil.ergodicDecompress(this.path, FilesUtil.getAppUnzipPath())
        if (unzip) {
          App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
            JmmIntent.UpdateDownLoadStatus(DownLoadStatus.INSTALLED) )
          JmmManagerActivity.downLoadStatus[jmmMetadata.id]?.resolve(DownLoadStatus.INSTALLED)
        } else {
          App.jmmManagerActivity?.jmmManagerViewModel?.handlerIntent(
            JmmIntent.UpdateDownLoadStatus(DownLoadStatus.FAIL) )
          JmmManagerActivity.downLoadStatus[jmmMetadata.id]?.resolve(DownLoadStatus.FAIL)
        }
        JmmManagerActivity.downLoadStatus.remove(jmmMetadata.id)
      }
    }
  }

  fun downloadStatusChange(url: String) {
    downloadMap[url]?.apply {
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