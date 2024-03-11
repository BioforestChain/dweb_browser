package org.dweb_browser.browser.jmm

import org.dweb_browser.platform.desktop.webview.WebviewEngine

actual fun getChromeWebViewVersion(): String? = WebviewEngine.chromiumVersion