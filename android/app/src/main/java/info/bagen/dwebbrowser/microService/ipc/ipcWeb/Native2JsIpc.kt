package info.bagen.dwebbrowser.microService.ipc.ipcWeb

import android.webkit.WebMessagePort
import info.bagen.dwebbrowser.microService.ipc.IPC_ROLE
import info.bagen.dwebbrowser.microService.ipc.Ipc
import java.util.concurrent.atomic.AtomicInteger


val ALL_MESSAGE_PORT_CACHE = mutableMapOf<Int, MessagePort>();
private var all_ipc_id_acc = AtomicInteger(1);
fun saveNative2JsIpcPort(port: WebMessagePort) = all_ipc_id_acc.getAndAdd(1).also { port_id ->
    ALL_MESSAGE_PORT_CACHE[port_id] = MessagePort.from(port);
}


/**
 * Native2JsIpc 的远端是在 webView 中的，所以底层使用 WebMessagePort 与指通讯
 *
 * ### 原理
 * 连接发起方执行 `fetch('file://js.sys.dweb/create-ipc')` 后，
 * 由 js-worker 创建了 channel-port1/2，然后 js-process(native) 负责中转这个信道（在nwjs中，我们直接使用内存引用，在mobile中，我们需要拦截webRequest），并为其存下一个 id(number)。
 * 最终将这个 id 通过 fetch 返回值返回。
 *
 * 那么连接发起方就可以通过这个 id(number) 和 Native2JsIpc 构造器来实现与 js-worker 的直连
 */
open class Native2JsIpc(
  val port_id: Int,
  remote: Ipc.MicroModuleInfo,
  role: IPC_ROLE = IPC_ROLE.CLIENT,
) : MessagePortIpc(
    ALL_MESSAGE_PORT_CACHE[port_id]
        ?: throw Exception("no found port2(js-process) by id: $port_id"), remote, role
) {
    init {
        onClose {
            ALL_MESSAGE_PORT_CACHE.remove(port_id)
        }
    }
}