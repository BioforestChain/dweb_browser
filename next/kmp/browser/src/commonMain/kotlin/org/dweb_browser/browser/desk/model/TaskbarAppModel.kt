package org.dweb_browser.browser.desk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.dweb_browser.helper.StrictImageResource

internal class TaskbarAppModel(
  val mmid: String,
  val icon: StrictImageResource?,
  running: Boolean,
  var isShowClose: Boolean = false,
) {
  var running by mutableStateOf(running)
  var opening by mutableStateOf(false)
}