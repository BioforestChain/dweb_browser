package org.dweb_browser.browser.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.jmm.JmmInstallerController
import org.dweb_browser.browser.jmm.JmmNMM
import org.dweb_browser.browser.jmm.debugJMM
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.helper.falseAlso

internal val LocalShowWebViewVersion = compositionLocalOf {
  mutableStateOf(false)
}
internal val LocalShowWebViewHelper = compositionLocalOf {
  mutableStateOf(false)
}

internal val LocalJmmViewHelper = compositionLocalOf<JmmManagerViewHelper> {
  noLocalProvidedFor("LocalJmmViewHelper")
}
internal val LocalJmmNMM = compositionLocalOf<JmmNMM> {
  noLocalProvidedFor("JmmNMM")
}

@Serializable
enum class JmmStatus {
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

data class JmmUIState(
  val jmmAppInstallManifest: JmmAppInstallManifest,
  val downloadSize: MutableState<Long> = mutableLongStateOf(0L),
  val downloadStatus: MutableState<JmmStatus> = mutableStateOf(JmmStatus.Init)
)

class JmmManagerViewHelper(
  jmmAppInstallManifest: JmmAppInstallManifest, private val controller: JmmInstallerController
) {
  val uiState: JmmUIState = JmmUIState(jmmAppInstallManifest)

  fun startDownload() = controller.ioAsyncScope.launch {
    val taskId = controller.createDownloadTask(uiState.jmmAppInstallManifest.bundle_url)
    watchProcess(taskId)
    // 已经注册完监听了，开始
    controller.start()
  }

  fun pause() = controller.ioAsyncScope.launch {
    controller.pause().falseAlso {
      uiState.downloadStatus.value = JmmStatus.Failed
    }
  }

  fun start() = controller.ioAsyncScope.launch {
    controller.start().falseAlso {
      uiState.downloadStatus.value = JmmStatus.Failed
    }
  }

  fun open() = controller.ioAsyncScope.launch {
    controller.openApp(uiState.jmmAppInstallManifest.id)
  }

  private suspend fun watchProcess(taskId: TaskId) {
    controller.watchProcess(taskId) {
      println("watch=> ${this.status.state.name} ${this.status.current}")

      if (this.status.state == DownloadState.Downloading) {
        uiState.downloadSize.value = this.status.current
        uiState.downloadStatus.value = JmmStatus.Downloading
      }

      if (this.status.state == DownloadState.Paused) {
        uiState.downloadSize.value = this.status.current
        uiState.downloadStatus.value = JmmStatus.Paused
      }

      if (this.status.state == DownloadState.Failed) {
        uiState.downloadSize.value = this.status.current
        uiState.downloadStatus.value = JmmStatus.Failed
      }

      // 下载完成触发解压
      if (this.status.state == DownloadState.Completed) {
        val success = controller.decompress(this)
        if (success) {
          uiState.downloadStatus.value = JmmStatus.INSTALLED
        } else {
          uiState.downloadSize.value = 0L
          uiState.downloadStatus.value = JmmStatus.Failed
        }
      }
    }
  }

  /**
   * 打开之后更新状态值，主要是为了退出应用后重新打开时需要
   */
  fun refreshStatus(hasNewVersion: Boolean) = controller.ioAsyncScope.launch {
    debugJMM(
      "refreshStatus",
      "是否是恢复 ${controller.downloadTaskId} 是否有新版本:${hasNewVersion}"
    )
    controller.downloadTaskId?.let { taskId ->
      // 继续/恢复 下载，不管什么状态都会推送过来
      controller.start()
      // 监听推送的变化
      watchProcess(taskId)
    } ?: if (hasNewVersion) {
      uiState.downloadStatus.value = JmmStatus.NewVersion
    } else if (controller.hasInstallApp()) {
      uiState.downloadStatus.value = JmmStatus.INSTALLED
    } else {
      null
    }
  }
}
