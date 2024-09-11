package org.dweb_browser.browser.download.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.dweb_browser.browser.download.DownloadController

class DecompressModel(private val downloadController: DownloadController) {
  var showProgress by mutableStateOf(false)
    private set
  var showError by mutableStateOf(false)
    private set
  var errMsg by mutableStateOf("")
    private set

  fun hide() {
    showError = false
    showProgress = false
  }

  fun showError(message: String) {
    showProgress = false
    showError = true
    errMsg = message
  }

  fun hidePopup() {
    showProgress = false
    showError = false
    errMsg = ""
  }
}