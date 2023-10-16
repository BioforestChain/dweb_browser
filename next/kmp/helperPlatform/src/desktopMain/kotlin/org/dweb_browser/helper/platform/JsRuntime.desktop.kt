package org.dweb_browser.helper.platform

import org.dweb_browser.helper.platform.jsruntime.JsRuntimeCore

actual class JsRuntime private actual constructor() {
  actual val core = JsRuntimeCore()
}