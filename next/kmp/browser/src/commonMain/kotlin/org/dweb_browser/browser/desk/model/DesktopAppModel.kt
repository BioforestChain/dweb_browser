package org.dweb_browser.browser.desk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.image.compose.ImageLoadResult

internal data class DesktopAppModel(
  val name: String,
  val mmid: MMID,
  val data: DesktopAppData,
  val icon: StrictImageResource?,
  val isSystemApp: Boolean,
  var running: DesktopAppRunStatus = DesktopAppRunStatus.NONE,
  var image: ImageLoadResult? = null,
  val id: UUID = randomUUID()
) {

  var size: Size by mutableStateOf(Size.Zero)
  var offset: Offset by mutableStateOf(Offset.Zero)

  enum class DesktopAppRunStatus {
    NONE, TORUNNING, RUNNING
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
    return "$id, $mmid, $running"
  }
}