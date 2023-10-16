package org.dweb_browser.helper.platform

import org.dweb_browser.helper.platform.jsruntime.JsRuntimeCore

expect class JsRuntime private constructor() {
  internal val core: JsRuntimeCore
}