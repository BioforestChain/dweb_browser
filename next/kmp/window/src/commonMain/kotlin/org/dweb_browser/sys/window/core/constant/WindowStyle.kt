package org.dweb_browser.sys.window.core.constant

import kotlinx.serialization.Serializable

@Serializable
data class WindowStyle(
  val topBarOverlay: Boolean? = null,
  val bottomBarOverlay: Boolean? = null,
  val keyboardOverlaysContent: Boolean? = null,
  val topBarContentColor: String? = null,
  val topBarContentDarkColor: String? = null,
  val topBarBackgroundColor: String? = null,
  val topBarBackgroundDarkColor: String? = null,
  val bottomBarContentColor: String? = null,
  val bottomBarContentDarkColor: String? = null,
  val bottomBarBackgroundColor: String? = null,
  val bottomBarBackgroundDarkColor: String? = null,
  val bottomBarTheme: String? = null,
  val themeColor: String? = null,
  val themeDarkColor: String? = null,
)
