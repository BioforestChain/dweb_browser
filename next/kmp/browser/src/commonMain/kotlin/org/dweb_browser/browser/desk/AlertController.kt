package org.dweb_browser.browser.desk

import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.getValue
import org.dweb_browser.helper.setValue

class AlertController {
  data class AlertMessage(val title: String?, val message: String)

  val alertMessagesFlow = MutableStateFlow(listOf<AlertMessage>())
  var alertMessages by alertMessagesFlow
  fun showAlert(reason: Throwable) {
    val title = reason.message
    val message = reason.stackTraceToString()
    alertMessages += AlertMessage(title, message)
  }
}