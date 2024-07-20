package org.dweb_browser.browser.desk.upgrade

import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.browser.desk.DeskNMM
import org.dweb_browser.browser.desk.DesktopController
import org.dweb_browser.browser.desk.debugDesk
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.downloadProgressFlow
import org.dweb_browser.browser.download.ext.existDownloadTask
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.removeDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.browser.web.openFileByPath
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.sys.device.ext.getDeviceAppVersion
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import org.dweb_browser.sys.toast.ext.showToast


/**
 * 获取当前版本，存储的版本，以及在线加载最新版本
 */
expect suspend fun loadApplicationNewVersion(): NewVersionItem?

enum class NewVersionType {
  Hide, NewVersion, Download, Install;
}

class NewVersionController(
  internal val deskNMM: DeskNMM.DeskRuntime,
  val desktopController: DesktopController,
) {
  private val store = NewVersionStore(deskNMM)
  var newVersionItem: NewVersionItem? = null
  val newVersionType = mutableStateOf(NewVersionType.Hide) // 用于显示新版本提醒的控制
  var openAgain: Boolean = false // 默认不需要重新打开，只有在授权后返回时，才需要重新打开

  fun updateVersionType(type: NewVersionType) {
    newVersionType.value = type
  }

  init {
    deskNMM.scopeLaunch(cancelable = true) { initNewVersionItem() }
  }

  private suspend fun initNewVersionItem() {
    val currentVersion = deskNMM.getDeviceAppVersion() // 获取当前系统的 app 版本
    val saveVersionItem = store.getNewVersion() // 获取之前下载存储的版本
    val loadVersionItem = loadApplicationNewVersion() // 获取服务器最新的版本
    // 直接判断 load 和 save 版本是否高于系统版本
    val loadHigher = loadVersionItem?.let {
      loadVersionItem.versionName.isGreaterThan(currentVersion)
    } ?: false
    val saveHigher = saveVersionItem?.let {
      saveVersionItem.versionName.isGreaterThan(currentVersion)
    } ?: false

    // 根据 loadHigher 和 saveHigher 赋值 newVersionItem
    newVersionItem = if (loadHigher && saveHigher) {
      if (loadVersionItem!!.versionName.isGreaterThan(saveVersionItem!!.versionName)) {
        saveVersionItem.taskId?.let { taskId -> deskNMM.removeDownload(taskId) }
        loadVersionItem
      } else {
        saveVersionItem
      }
    } else if (loadHigher) {
      saveVersionItem?.taskId?.let { taskId -> deskNMM.removeDownload(taskId) }
      loadVersionItem
    } else if (saveHigher) {
      saveVersionItem
    } else {
      null
    }

    // 如果 newVersionItem 为空的话，那么就不需要显示了；如果不为空，判断状态进行指定跳转
    newVersionItem?.let { newVersion ->
      if (newVersion.status.state == DownloadState.Completed) {
        updateVersionType(NewVersionType.Install)
      } else {
        updateVersionType(NewVersionType.NewVersion)
      }
    }
    debugDesk("NewVersion", "hasNew=${newVersionType.value} => $newVersionItem")
  }

  private suspend fun watchProcess(taskId: String, newVersionItem: NewVersionItem): Boolean {
    var success = false;
    deskNMM.downloadProgressFlow(taskId).collect { status ->
      if (status.state == DownloadState.Completed) {
        success = true
      }
      newVersionItem.updateDownloadStatus(status, store)
    }
    return success
  }

  val downloadApp = SuspendOnce(before = {
    val grant = deskNMM.requestSystemPermission(
      name = SystemPermissionName.STORAGE,
      title = NewVersionI18nResource.request_permission_title_storage.text,
      description = NewVersionI18nResource.request_permission_message_storage.text
    )
    if (!grant) {
      deskNMM.showToast(NewVersionI18nResource.toast_message_permission_fail.text)
      throw IllegalStateException(NewVersionI18nResource.toast_message_permission_fail.text)
    }
  }) {
    newVersionItem?.let { newVersion ->
      var taskId = newVersion.taskId
      if (taskId == null || !deskNMM.existDownloadTask(taskId)) {
        val downloadTask = deskNMM.createDownloadTask(url = newVersion.originUrl, external = true)
        taskId = downloadTask.id
        newVersion.initDownloadTask(downloadTask, store)
        deskNMM.scopeLaunch(cancelable = true) {
          if (watchProcess(taskId, newVersion)) {
            // 跳转到安装界面
            val realPath = downloadTask.filepath
            if (openFileByPath(realPath = realPath, justInstall = true)) { // 安装文件 TODO
              newVersionType.value = NewVersionType.Hide
              // 清除保存的新版本信息
              store.clear()
            } else {
              debugDesk("NewVersion", "no Install Apk Permission")
              updateVersionType(NewVersionType.Install)
            }
          }
        }
      }

      deskNMM.startDownload(taskId).falseAlso {
        deskNMM.showToast(NewVersionI18nResource.toast_message_download_fail.text)
      }
    }
  }


  suspend fun pause() = newVersionItem?.taskId?.let { deskNMM.pauseDownload(it) } ?: false

  fun openSystemInstallSetting() = run {
    openAgain = true // 为了使得返回的时候重新判断是否安装
    // TODO 这边似乎还没完成
    // OpenFileUtil.openSystemInstallSetting()
  }
}