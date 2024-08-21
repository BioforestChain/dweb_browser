package org.dweb_browser.browser.jmm.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmDetailController
import org.dweb_browser.browser.jmm.JmmMetadata
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmStatusEvent
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.browser.resources.Res
import org.dweb_browser.browser.resources.install_button_downloading
import org.dweb_browser.browser.resources.install_button_install
import org.dweb_browser.browser.resources.install_button_installing
import org.dweb_browser.browser.resources.install_button_jump_home
import org.dweb_browser.browser.resources.install_button_lower
import org.dweb_browser.browser.resources.install_button_open
import org.dweb_browser.browser.resources.install_button_paused
import org.dweb_browser.browser.resources.install_button_retry
import org.dweb_browser.browser.resources.install_button_update
import org.dweb_browser.helper.toSpaceSize
import org.jetbrains.compose.resources.stringResource

/**
 * 通过 JmmStatusEvent，返回需要显示的状态和文件大小或者进度值
 */
@Composable
internal fun rememberJmmUiKit(controller: JmmDetailController) =
  remember(controller, controller.metadata) {
    JmmUiKit(controller, controller.metadata)
  }

internal class JmmUiKit(
  val controller: JmmDetailController,
  val metadata: JmmMetadata,
) {
  val jmmStatusEvent get() = metadata.state
  val jmmStatus get() = jmmStatusEvent.state

  val labelStart
    @Composable get() = if (!canSupportTarget) {
      stringResource(Res.string.install_button_jump_home)
    } else when (jmmStatus) {
      JmmStatus.Init, JmmStatus.Canceled -> stringResource(Res.string.install_button_install)
      JmmStatus.NewVersion -> stringResource(Res.string.install_button_update)
      JmmStatus.Downloading -> stringResource(Res.string.install_button_downloading)
      JmmStatus.Paused -> stringResource(Res.string.install_button_paused)
      JmmStatus.Completed -> stringResource(Res.string.install_button_installing)
      JmmStatus.INSTALLED -> stringResource(Res.string.install_button_open)
      JmmStatus.Failed -> stringResource(Res.string.install_button_retry)
      JmmStatus.VersionLow -> stringResource(Res.string.install_button_lower)
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