package org.dweb_browser.browser.desk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict

internal class DesktopAppModel(
  val appMetaData: DeskAppMetaData,
  initRunningState: DesktopAppRunStatus = DesktopAppRunStatus.Close,
) {
  val name by lazy { appMetaData.short_name.ifEmpty { appMetaData.name } }
  val mmid get() = appMetaData.mmid
  val webLink by lazy {
    if (appMetaData.categories.contains(MICRO_MODULE_CATEGORY.Web_Browser) && appMetaData.mmid != "web.browser.dweb" && appMetaData.homepage_url.isNullOrEmpty()) {
      appMetaData.homepage_url
    } else null
  }
  val isWebLink get() = webLink != null
  val icon by lazy { appMetaData.icons.toStrict().pickLargest() }
  val isSystemApp get() = appMetaData.targetType == "nmm"
  var running by mutableStateOf(initRunningState)
  var size by mutableStateOf(Size.Zero)
  var offset by mutableStateOf(Offset.Zero)

  enum class DesktopAppRunStatus {
    Close, Opening, Opened
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as DesktopAppModel

    if (mmid != other.mmid) return false
    if (name != other.name) return false
    if (running != other.running) return false

    return true
  }


  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + mmid.hashCode() + 100 * running.hashCode()
    return result
  }

  override fun toString(): String {
    return "$mmid, $running"
  }
}