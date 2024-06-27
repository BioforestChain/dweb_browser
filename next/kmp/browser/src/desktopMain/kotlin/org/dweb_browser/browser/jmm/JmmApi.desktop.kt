package org.dweb_browser.browser.jmm

import org.dweb_browser.platform.desktop.webview.jxBrowserEngine

actual fun getChromeWebViewVersion(): String? = jxBrowserEngine.chromiumVersion