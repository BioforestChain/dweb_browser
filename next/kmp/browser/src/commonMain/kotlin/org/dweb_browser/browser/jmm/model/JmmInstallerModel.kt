package org.dweb_browser.browser.jmm.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.jmm.JmmInstallerController
import org.dweb_browser.browser.jmm.debugJMM
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.helper.falseAlso

internal val LocalShowWebViewVersion = compositionLocalOf {
  mutableStateOf(false)
}

internal val LocalJmmViewHelper = compositionLocalOf<JmmInstallerModel> {
  noLocalProvidedFor("LocalJmmViewHelper")
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
  Canceled,

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

class JmmInstallerModel(
  jmmAppInstallManifest: JmmAppInstallManifest, private val controller: JmmInstallerController
) {
  val uiState: JmmUIState = JmmUIState(jmmAppInstallManifest)

  fun startDownload() = controller.ioAsyncScope.launch {
    if (controller.jmmHistoryMetadata.taskId == null ||
      (uiState.downloadStatus.value != JmmStatus.INSTALLED &&
          uiState.downloadStatus.value != JmmStatus.Completed)
    ) {
      controller.createDownloadTask(
        uiState.jmmAppInstallManifest.bundle_url, uiState.jmmAppInstallManifest.bundle_size
      )
      watchProcess()
    }
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

  private suspend fun watchProcess() {
    controller.watchProcess { state, current, total ->
      debugJMM("ViewHelper", "watchProcess=> $state, $current, $total")
      uiState.downloadStatus.value = state
      uiState.downloadSize.value = current
    }
  }

  /**
   * 打开之后更新状态值，主要是为了退出应用后重新打开时需要
   */
  fun refreshStatus(hasNewVersion: Boolean) = controller.ioAsyncScope.launch {
    debugJMM(
      "refreshStatus",
      "是否是恢复 ${controller.jmmHistoryMetadata.taskId} 是否有新版本:${hasNewVersion}"
    )
    if (controller.exists()) {
      // 监听推送的变化
      watchProcess()
      // 继续/恢复 下载，不管什么状态都会推送过来
      // controller.start()
    } else if (hasNewVersion) {
      uiState.downloadStatus.value = JmmStatus.NewVersion
    } else if (controller.hasInstallApp()) {
      uiState.downloadStatus.value = JmmStatus.INSTALLED
    } else {
      uiState.downloadStatus.value = JmmStatus.Init
    }
  }
}
