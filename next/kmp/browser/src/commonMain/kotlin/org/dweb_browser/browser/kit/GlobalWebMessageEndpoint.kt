package org.dweb_browser.browser.kit

import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.core.ipc.WebMessageEndpoint
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeInt

/**
 * 创建一个 WebMessageEndpoint ，会全局保存
 * 使用一个 globalId 替代 内存实例，从而方便传输
 *
 * 通常用于 JsBridge 中
 */
class GlobalWebMessageEndpoint(
  port: IWebMessagePort,
  debugIdPrefix: String = "native2js",
  parentScope: CoroutineScope = kotlinIpcPool.scope,
) : WebMessageEndpoint("$debugIdPrefix@G$globalIdAcc", parentScope, port) {
  companion object {
    private var globalIdAcc by SafeInt(1);
    private val ALL = SafeHashMap<Int, GlobalWebMessageEndpoint>();

    /**
     * Native2JsIpc 的远端是在 webView 中的，所以底层使用 WebMessagePort 与指通讯
     *
     * ### 原理
     * 连接发起方执行 `fetch('file://js.browser.dweb/create-ipc')` 后，
     * 由 js-worker 创建了 channel-port1/2，然后 js-process(native) 负责中转这个信道（在nwjs中，我们直接使用内存引用，在mobile中，我们需要拦截webRequest），并为其存下一个 id(number)。
     * 最终将这个 id 通过 fetch 返回值返回。
     *
     * 那么连接发起方就可以通过这个 id(number) 和 Native2JsIpc 构造器来实现与 js-worker 的直连
     *
     */
    fun get(globalId: Int) =
      ALL[globalId] ?: throw Exception("no found GlobalWebMessageEndpoint by globalId: $globalId")
  }

  val globalId: Int = globalIdAcc++

  init {
    ALL[globalId] = this
    onClosed {
      ALL.remove(globalId)
    }
  }
}