package org.dweb_browser.browser.desk.model

internal sealed class DesktopAppData {
  abstract val mmid: String

  data class App(override val mmid: String) : DesktopAppData()
  data class WebLink(override val mmid: String, val url: String) : DesktopAppData()
}