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
  val jmmStatusEvent get() = metadata.state
  val jmmStatus get() = jmmStatusEvent.state
  val labelStart
    get() = if (!canSupportTarget) {
      BrowserI18nResource.install_button_jump_home.text
    } else when (jmmStatus) {
      JmmStatus.Init, JmmStatus.Canceled -> BrowserI18nResource.install_button_install.text
      JmmStatus.NewVersion -> BrowserI18nResource.install_button_update.text
      JmmStatus.Downloading -> BrowserI18nResource.install_button_downloading.text
      JmmStatus.Paused -> BrowserI18nResource.install_button_paused.text
      JmmStatus.Completed -> BrowserI18nResource.install_button_installing.text
      JmmStatus.INSTALLED -> BrowserI18nResource.install_button_open.text
      JmmStatus.Failed -> BrowserI18nResource.install_button_retry.text
      JmmStatus.VersionLow -> BrowserI18nResource.install_button_lower.text
    }
  val labelEnd
    get() = when (jmmStatus) {
      JmmStatus.Init,

      JmmStatus.Canceled,

      JmmStatus.NewVersion,

      JmmStatus.Downloading,

      JmmStatus.Paused -> jmmStatusEvent.progressText

      else -> null
    }
  val description
    get() = if (!canSupportTarget) {
      BrowserI18nResource.install_button_incompatible.text
    } else null

  /**
   * 应用是否是当前支持的大版本
   */
  private val canSupportTarget = metadata.manifest.canSupportTarget(JsMicroModule.VERSION)
  val showLinearProgress
    get() = when (jmmStatus) {
      JmmStatus.Downloading, JmmStatus.Paused -> true
      else -> false
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