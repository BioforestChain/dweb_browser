package org.dweb_browser.browser.jmm

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.PromiseOut

/**
 * 桥接ipc到js内部：
 * 使用 create-ipc 指令来创建一个代理的 WebMessagePortIpc ，然后我们进行中转
 */
expect suspend fun ipcBridge(
  fromMMID: MMID,
  remoteMM: MicroModule,
  pid: String,
  fromMMIDOriginIpcWM: MutableMap<MMID, PromiseOut<Ipc>>,
  targetIpc: Ipc?
): Ipc
