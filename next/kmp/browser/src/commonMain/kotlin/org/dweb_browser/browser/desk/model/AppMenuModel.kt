package org.dweb_browser.browser.desk.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
import org.dweb_browser.browser.BrowserI18nResource

internal data class AppMenuModel(val type: AppMenuType, val enable: Boolean)
internal enum class AppMenuType {
  OFF {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(
        BrowserI18nResource.Desktop.quit.text, Icons.Outlined.HighlightOff
      )
  },

  DETAIL {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(
        BrowserI18nResource.Desktop.detail.text, Icons.Outlined.Description
      )
  },

  UNINSTALL {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(
        BrowserI18nResource.Desktop.uninstall.text, Icons.Outlined.Delete
      )
  },

  SHARE {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(BrowserI18nResource.Desktop.share.text, Icons.Outlined.Share)
  },

  DELETE {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(BrowserI18nResource.Desktop.delete.text, Icons.Outlined.Delete)
  };

  data class MoreAppModelTypeData(val title: String, val icon: ImageVector)

  abstract val data: MoreAppModelTypeData
}

internal fun createAppMenuDisplays(
  app: DesktopAppModel
): List<AppMenuModel> {

  val displays = mutableListOf<AppMenuModel>()

  when (app.data) {
    is DesktopAppData.App -> {
      displays.add(
        AppMenuModel(AppMenuType.OFF, app.running == DesktopAppModel.DesktopAppRunStatus.RUNNING)
      )
      if (!app.isSystemApp) {
        displays.add(AppMenuModel(AppMenuType.DETAIL, true))
        displays.add(AppMenuModel(AppMenuType.UNINSTALL, true))
      }
    }

    is DesktopAppData.WebLink -> {
      displays.add(AppMenuModel(AppMenuType.DELETE, true))
    }
  }

  displays.add(AppMenuModel(AppMenuType.SHARE, false))

  return displays
}