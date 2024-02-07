package org.dweb_browser.browser.web.data

enum class ConstUrl(val url: String) {
  BLANK("about:blank"),
}

/**
 * target: The target in which to load the URL, an optional parameter that defaults to _self. (String)
 *  _self: Opens in the Cordova WebView if the URL is in the white list, otherwise it opens in the InAppBrowser.
 *  _blank: Opens in the InAppBrowser.
 *  _system: Opens in the system's web browser.
 */
enum class AppBrowserTarget(val type: String) {
  SELF("_self"),
  BLANK("_blank"),
  SYSTEM("_system")
}