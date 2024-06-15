package org.dweb_browser.browser.jsProcess

import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.sys.tray.ext.registryTray

/**桌面端托盘模块*/
actual suspend fun registryTray(runtime: NativeMicroModule.NativeRuntime) {
  // 激活调试jsProcess
  runtime.registryTray(
    title = "Open Js Process Devtool",
    url = "file://js.browser.dweb/open-devTool"
  )
}
