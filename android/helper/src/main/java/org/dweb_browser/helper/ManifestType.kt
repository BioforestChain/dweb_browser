package org.dweb_browser.helper

data class ImageResource(
  val src: String,
  val sizes: String? = null,
  val type: String? = null,
  val purpose: String? = null,
  val platform: String? = null
)

data class ShortcutItem(
  val name: String,
  val short_name: String? = null,
  val description: String? = null,
  val url: String,
  val icons: MutableList<ImageResource>? = null
)


enum class DisplayMode {
  fullscreen,
  standalone,
  minimal_ui,
  browser
}