package org.dweb_browser.browser.jmm.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import org.dweb_browser.browser.jmm.JmmHistoryMetadata
import org.dweb_browser.browser.jmm.JmmInstallerController
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmStatusEvent
import org.dweb_browser.helper.platform.noLocalProvidedFor
import org.dweb_browser.helper.falseAlso

internal val LocalShowWebViewVersion = compositionLocalOf {
  mutableStateOf(false)
}

internal val LocalJmmViewHelper = compositionLocalOf<JmmInstallerModel> {
  noLocalProvidedFor("LocalJmmViewHelper")
}


data class JmmUIState(
  val jmmHistoryMetadata: JmmHistoryMetadata,
  val downloadSize: MutableState<Long> = mutableLongStateOf(0L),
)

class JmmInstallerModel(
  private val jmmHistoryMetadata: JmmHistoryMetadata,
  private val controller: JmmInstallerController
) {
  val uiState: JmmUIState = JmmUIState(jmmHistoryMetadata = jmmHistoryMetadata)

  fun startDownload() = controller.ioAsyncScope.launch {
    if (jmmHistoryMetadata.taskId == null ||
      (jmmHistoryMetadata.state.state != JmmStatus.INSTALLED &&
          jmmHistoryMetadata.state.state != JmmStatus.Completed)
    ) {
      controller.createDownloadTask()
    }
    // 已经注册完监听了，开始
    controller.start()
  }

  fun pause() = controller.ioAsyncScope.launch {
    controller.pause()
  }

  fun start() = controller.ioAsyncScope.launch {
    controller.start().falseAlso {
      jmmHistoryMetadata.updateState(JmmStatus.Failed)
    }
  }

  fun open() = controller.ioAsyncScope.launch {
    controller.openApp(uiState.jmmHistoryMetadata.metadata.id)
    controller.closeSelf() // 打开应用后，需要关闭当前安装界面
  }
}
