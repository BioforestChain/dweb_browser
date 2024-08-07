package org.dweb_browser.browser.desk.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.HighlightOff
import androidx.compose.material.icons.twotone.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.desk.DeskI18n

internal data class AppMenuModel(val type: AppMenuType, val enable: Boolean)
internal enum class AppMenuType {
  OFF {
    override val data: AppModelTypeData
      get() = AppModelTypeData(
        BrowserI18nResource.Desktop.quit.text, Icons.TwoTone.HighlightOff
      )
  },

  DETAIL {
    override val data: AppModelTypeData
      get() = AppModelTypeData(
        BrowserI18nResource.Desktop.detail.text, Icons.TwoTone.Description
      )
  },

  UNINSTALL {
    override val data: AppModelTypeData
      get() = AppModelTypeData(
        DeskI18n.uninstall.text, Icons.TwoTone.Delete, color = Color(0xFFEF5350)
      )
  },

  SHARE {
    override val data: AppModelTypeData
      get() = AppModelTypeData(BrowserI18nResource.Desktop.share.text, Icons.TwoTone.Share)
  },

  DELETE {
    override val data: AppModelTypeData
      get() = AppModelTypeData(
        BrowserI18nResource.Desktop.delete.text,
        Icons.TwoTone.Delete,
        color = Color.Red
      )
  };

  data class AppModelTypeData(
    val title: String,
    val icon: ImageVector,
    val color: Color = Color.Black,
  )

  abstract val data: AppModelTypeData
}

internal fun DesktopAppModel.getAppMenuDisplays(): List<AppMenuModel> {
  val displays = mutableListOf<AppMenuModel>()
  when {
    isWebLink -> {
      displays.add(AppMenuModel(AppMenuType.DELETE, true))
    }

    else -> {
      displays.add(
        AppMenuModel(AppMenuType.OFF, running == DesktopAppModel.DesktopAppRunStatus.Opened)
      )
      if (!isSystemApp) {
        displays.add(AppMenuModel(AppMenuType.DETAIL, true))
        displays.add(AppMenuModel(AppMenuType.UNINSTALL, true))
      }
    }
  }

  displays.add(AppMenuModel(AppMenuType.SHARE, false))

  return displays
}