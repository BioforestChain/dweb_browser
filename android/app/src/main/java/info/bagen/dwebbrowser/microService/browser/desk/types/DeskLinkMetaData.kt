package info.bagen.dwebbrowser.microService.browser.desk.types

import org.dweb_browser.helper.ImageResource

data class DeskLinkMetaData(
  val title: String, val icon: ImageResource? = null, val url: String
)