package org.dweb_browser.browser.desk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.StrictImageResource

internal class DesktopAppModel(
  val name: String,
  val mmid: MMID,
  val data: DesktopAppData,
  val icon: StrictImageResource?,
  val isSystemApp: Boolean,
  running: DesktopAppRunStatus = DesktopAppRunStatus.Close,
) {
  var running by mutableStateOf(running)
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