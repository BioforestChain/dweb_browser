package org.dweb_browser.dwebview.ipcWeb

import kotlinx.atomicfu.atomic
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.dwebview.DWebMessagePort
import org.dweb_browser.dwebview.IWebMessagePort

val ALL_MESSAGE_PORT_CACHE = mutableMapOf<Int, MessagePort>();
private var all_ipc_id_acc = atomic(1);
suspend fun saveNative2JsIpcPort(port: IWebMessagePort): Int {
  require(port is DWebMessagePort)
  return all_ipc_id_acc.getAndAdd(1).also { portId ->
    ALL_MESSAGE_PORT_CACHE[portId] = MessagePort.from(port);
  }
}


/**
 * Native2JsIpc 的远端是在 webView 中的，所以底层使用 WebMessagePort 与指通讯
 *
 * ### 原理
 * 连接发起方执行 `fetch('file://js.browser.dweb/create-ipc')` 后，
 * 由 js-worker 创建了 channel-port1/2，然后 js-process(native) 负责中转这个信道（在nwjs中，我们直接使用内存引用，在mobile中，我们需要拦截webRequest），并为其存下一个 id(number)。
 * 最终将这个 id 通过 fetch 返回值返回。
 *
 * 那么连接发起方就可以通过这个 id(number) 和 Native2JsIpc 构造器来实现与 js-worker 的直连
 */
open class Native2JsIpc(
  val portId: Int,
  remote: IMicroModuleManifest,
  role: IPC_ROLE = IPC_ROLE.CLIENT,
) : MessagePortIpc(
  ALL_MESSAGE_PORT_CACHE[portId] ?: throw Exception("no found port2(js-process) by id: $portId"),
  remote,
  role
) {
  init {
    onClose {
      ALL_MESSAGE_PORT_CACHE.remove(portId)
    }
  }
}