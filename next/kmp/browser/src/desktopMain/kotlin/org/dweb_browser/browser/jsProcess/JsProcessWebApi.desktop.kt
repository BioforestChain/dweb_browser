package org.dweb_browser.browser.jsProcess

import io.ktor.http.URLBuilder
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUnsafeString

/**桌面端托盘模块*/
actual suspend fun registryTray(runtime: NativeMicroModule.NativeRuntime) {
  // 激活调试jsProcess
  runtime.nativeFetch(URLBuilder("file://tray.sys.dweb/registry").apply {
    parameters["title"] = "Js Process"
    parameters["url"] = "file://js.browser.dweb/open-devTool"
  }.buildUnsafeString())
}
