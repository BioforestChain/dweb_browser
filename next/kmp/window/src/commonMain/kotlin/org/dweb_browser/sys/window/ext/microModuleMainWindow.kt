package org.dweb_browser.sys.window.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.UUID
import org.dweb_browser.sys.window.core.windowInstancesManager

/**
 * 打开主窗口并获得该实例
 *
 * PS JsMicroModule 不支持改函数，因为我们不能在native侧直接发送fetch，需要js侧自己发起
 */
suspend fun NativeMicroModule.NativeRuntime.openMainWindow() =
  nativeFetch("file://window.sys.dweb/openMainWindow").text().let { wid ->
    windowInstancesManager.get(wid)?.also { win ->
      win.state.constants.microModule.value = this
      onShutdown {
        win.state.constants.microModule.value = null
        scopeLaunch(cancelable = false) {
          win.tryCloseOrHide(true)
        }
      }
      win.focus()
    } ?: throw Exception("fail to open window for $mmid, not an application")
  }

/**
 * 根据窗口ID获得窗口的实例
 */
suspend fun MicroModule.Runtime.getWindow(wid: UUID) =
  windowInstancesManager.get(wid)?.also { it.state.constants.microModule.value = this }
    ?: throw Exception("fail to got window for $mmid, wid=$wid is invalid, maybe microModule not an application")

suspend fun NativeMicroModule.NativeRuntime.getOrOpenMainWindow() =
  getWindow(getOrOpenMainWindowId())

suspend fun NativeMicroModule.NativeRuntime.getMainWindow() = getWindow(getMainWindowId())
