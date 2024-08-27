package org.dweb_browser.browser.desk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.sys.window.core.constant.WindowMode

internal class TaskbarAppModel(
  val mmid: String,
  val icon: StrictImageResource?,
  running: Boolean,
  var isShowClose: Boolean = false,
  var focus: Boolean = false,
  var mode: WindowMode = WindowMode.FLOAT
) {
  var running by mutableStateOf(running)
  var opening by mutableStateOf(false)
}