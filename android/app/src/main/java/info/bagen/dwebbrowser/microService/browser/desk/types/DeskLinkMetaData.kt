package info.bagen.dwebbrowser.microService.browser.desk.types

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ImageResource

@Serializable
data class DeskLinkMetaData(
  val title: String, val icon: ImageResource? = null, val url: String
)