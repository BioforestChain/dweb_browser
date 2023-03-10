package info.bagen.rust.plaoc.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import info.bagen.rust.plaoc.network.ApiService
import info.bagen.rust.plaoc.util.FilesUtil
import info.bagen.rust.plaoc.util.ZipUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.broadcast.BFSBroadcastAction
import info.bagen.rust.plaoc.broadcast.BFSBroadcastReceiver
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.runBlockingCatching
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.DownLoadObserver
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule
import info.bagen.rust.plaoc.microService.sys.jmm.ui.*
import info.bagen.rust.plaoc.ui.app.AppViewIntent
import info.bagen.rust.plaoc.ui.app.AppViewModel
import info.bagen.rust.plaoc.ui.app.AppViewState
import info.bagen.rust.plaoc.ui.app.NewAppUnzipType
import info.bagen.rust.plaoc.ui.entity.AppInfo
import info.bagen.rust.plaoc.util.NotificationUtil
import kotlinx.coroutines.*
import java.io.File

internal interface IDwebBrowserBinder {
  fun invokeDownloadAndSaveZip(downLoadInfo: DownLoadInfo)
  fun invokeDownloadStatusChange(mmid: Mmid)
}

class DwebBrowserService : Service() {
  private val downloadMap = mutableMapOf<Mmid, DownLoadInfo>() // 用于监听下载列表

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
  }

  private fun registerBFSBroadcastReceiver() {
    val intentFilter = IntentFilter()
    intentFilter.addAction(BFSBroadcastAction.BFSInstallApp.action)
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
    downloadMap[downLoadInfo.jmmMetadata.id] = downLoadInfo
    NotificationUtil.INSTANCE.createNotificationForProgress(
      downLoadInfo.jmmMetadata.id,
      downLoadInfo.notificationId,
      downLoadInfo.jmmMetadata.title
    ) // 显示通知
    DownLoadObserver.emit(downLoadInfo.jmmMetadata.id, DownLoadStatus.DownLoading) // 同步更新所有注册
    GlobalScope.launch(Dispatchers.IO) {
      ApiService.instance.downloadAndSave(
        downLoadInfo.jmmMetadata.downloadUrl, File(downLoadInfo.path),
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
        downLoadInfo.jmmMetadata.downloadUrl, File(downLoadInfo.path), downLoadInfo.size,
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
    DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.DownLoading, current, total)

    if (current == total) {
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
      // 如果完成后发现下载界面没有打开，手动打开
      // JmmManagerActivity.startActivity(jmmMetadata)
      // TODO 判断当前的下载的版本是否比较新，是否已存在。然后进行覆盖
      /*when (compareDownloadApp()) {
        NewAppUnzipType.LOW_VERSION -> return
        else -> {}
      }*/
      runBlocking { delay(1000) }
      val unzip =
        ZipUtil.ergodicDecompress(this.path, FilesUtil.getAppUnzipPath(), mmid = jmmMetadata.id)
      if (unzip) {
        runBlockingCatching {
          JsMicroModule(jmmMetadata).nativeFetch("${JmmNMM.hostName}/download?mmid=${jmmMetadata.id}")
        }
        DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.INSTALLED)
      } else {
        DownLoadObserver.emit(this.jmmMetadata.id, DownLoadStatus.FAIL)
      }
      downloadMap.remove(jmmMetadata.id) // 下载完成后需要移除
      DownLoadObserver.close(jmmMetadata.id) // 移除当前mmid所有关联推送
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
          DownLoadObserver.emit(this.jmmMetadata.id, downLoadStatus, dSize, size)
          breakPointDownLoadAndSave(this)
        }
        else -> {
          this.downLoadStatus = DownLoadStatus.PAUSE
          NotificationUtil.INSTANCE.updateNotificationForProgress(
            (this.dSize * 1.0 / this.size * 100).toInt(), this.notificationId, "暂停"
          )
          DownLoadObserver.emit(this.jmmMetadata.id, downLoadStatus, dSize, size)
        }
      }
    }
  }

  private fun compareDownloadApp(
    appViewModel: AppViewModel, appViewState: AppViewState, appInfo: AppInfo?, zipFile: String
  ): NewAppUnzipType {
    var ret = NewAppUnzipType.INSTALL
    if (appViewState.bfsDownloadPath != null && appInfo != null) {
      run OutSide@{
        appViewModel.uiState.appViewStateList.forEach { tempAppViewState ->
          if (tempAppViewState.appInfo?.bfsAppId == appInfo.bfsAppId) {
            if (compareAppVersionHigh(
                tempAppViewState.appInfo!!.version,
                appInfo.version
              )
            ) {
              ret = NewAppUnzipType.OVERRIDE
              appViewModel.handleIntent(
                AppViewIntent.OverrideDownloadApp(appViewState, appInfo, zipFile)
              )
            } else {
              ret = NewAppUnzipType.LOW_VERSION
              appViewModel.handleIntent(AppViewIntent.RemoveDownloadApp(appViewState))
            }
            return@OutSide
          }
        }
      }
      if (ret == NewAppUnzipType.INSTALL) {
        appViewModel.handleIntent(
          AppViewIntent.UpdateDownloadApp(
            appViewState,
            appInfo,
            zipFile
          )
        )
      }
    }
    return ret
  }

  private fun compareAppVersionHigh(localVersion: String, compareVersion: String): Boolean {
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
}