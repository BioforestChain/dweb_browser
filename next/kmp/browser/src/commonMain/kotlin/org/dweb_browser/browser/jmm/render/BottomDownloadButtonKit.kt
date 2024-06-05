package org.dweb_browser.browser.jmm.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmInstallerController
import org.dweb_browser.browser.jmm.JmmMetadata
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmStatusEvent
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.helper.toSpaceSize

/**
 * 通过 JmmStatusEvent，返回需要显示的状态和文件大小或者进度值
 */
@Composable
internal fun rememberJmmUiKit(controller: JmmInstallerController) =
  remember(controller, controller.installMetadata) {
    JmmUiKit(controller, controller.installMetadata)
  }

internal class JmmUiKit(
  val controller: JmmInstallerController,
  val metadata: JmmMetadata,
) {
  val jmmStatusEvent = metadata.state
  val jmmStatus = metadata.state.state
  val labelStart: String
  val labelEnd: String?
  val description: String?

  /**
   * 应用是否是当前支持的大版本
   */
  private val canSupportTarget = metadata.manifest.canSupportTarget(JsMicroModule.VERSION)
  val showLinearProgress = jmmStatus == JmmStatus.Downloading || jmmStatus == JmmStatus.Paused

  init {
    // 应用是否是当前支持的大版本
    if (!canSupportTarget) {
      labelStart = BrowserI18nResource.install_button_jump_home.text
      labelEnd = null
      description = BrowserI18nResource.install_button_incompatible.text
    } else {
      description = null
      when (jmmStatus) {
        JmmStatus.Init, JmmStatus.Canceled -> {
          labelStart = BrowserI18nResource.install_button_install.text
          labelEnd = jmmStatusEvent.progressText
        }


        JmmStatus.NewVersion -> {
          labelStart = BrowserI18nResource.install_button_update.text
          labelEnd = jmmStatusEvent.progressText
        }


        JmmStatus.Downloading -> {
          labelStart = BrowserI18nResource.install_button_downloading.text
          labelEnd = jmmStatusEvent.progressText
        }


        JmmStatus.Paused -> {
          labelStart = BrowserI18nResource.install_button_paused.text
          labelEnd = jmmStatusEvent.progressText
        }


        JmmStatus.Completed -> {
          labelStart = BrowserI18nResource.install_button_installing.text
          labelEnd = null
        }


        JmmStatus.INSTALLED -> {
          labelStart = BrowserI18nResource.install_button_open.text
          labelEnd = null
        }


        JmmStatus.Failed -> {
          labelStart = BrowserI18nResource.install_button_retry.text
          labelEnd = null
        }


        JmmStatus.VersionLow -> {
          labelStart = BrowserI18nResource.install_button_lower.text
          labelEnd = null
        }

      }
    }

  }

  fun onClickDownloadButton() = controller.jmmNMM.scopeLaunch(cancelable = true) {
    if (!canSupportTarget) {
      controller.openReferrerPage()
      return@scopeLaunch
    }
    when (jmmStatus) {
      JmmStatus.Init, JmmStatus.Failed, JmmStatus.Canceled -> {
        controller.createAndStartDownload()
      }

      JmmStatus.NewVersion -> {
        controller.closeApp()
        controller.createAndStartDownload()
      }

      JmmStatus.Paused -> {
        controller.startDownload()
      }

      JmmStatus.Downloading -> {
        controller.pause()
      }

      JmmStatus.Completed -> {

      }

      JmmStatus.VersionLow -> {}

      JmmStatus.INSTALLED -> {

        controller.openApp()
      }
    }
  }
}

private val JmmStatusEvent.progressText: String?
  get() {
    var text = ""
    if (current > 0) {
      text += current.toSpaceSize()
    }
    if (total > 1 && total > current) {
      if (text.isNotEmpty()) {
        text += " / "
      }
      text += total.toSpaceSize()
    }
    return text.trim().ifEmpty { null } // 如果字符串是空的，直接返回 null
  }