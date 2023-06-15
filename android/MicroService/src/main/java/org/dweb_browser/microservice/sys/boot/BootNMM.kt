package org.dweb_browser.microservice.sys.boot

import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.message.IpcEvent
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.core.Router
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugBoot(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("boot", tag, msg, err)

class BootNMM(initMmids: List<Mmid>? = null) : NativeMicroModule("boot.sys.dweb") {
  /**
   * 开机启动项注册表
   * TODO 这里需要从数据库中读取
   */
  private val registeredMmids = mutableSetOf<Mmid>()

  init {
    if (initMmids != null) {
      registeredMmids += initMmids
    }
  }

  override val routers: Router = mutableMapOf()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      "/register" bind Method.GET to defineHandler { _, ipc ->
        register(ipc.remote.mmid)
      },
      "/unregister" bind Method.GET to defineHandler { _, ipc ->
        unregister(ipc.remote.mmid)
      }
    )
  }

  override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
    for (mmid in registeredMmids) {
      debugBoot("launch", mmid)
      bootstrapContext.dns.bootstrap(mmid)
      bootstrapContext.dns.connect(mmid).ipcForFromMM.postMessage(event)
    }
  }

  override suspend fun _shutdown() {
    routers.clear()
  }

  /**
   * 注册一个boot程序
   * TODO 这里应该有用户授权，允许开机启动
   */
  private fun register(mmid: Mmid) = this.registeredMmids.add(mmid);

  /**
   * 移除一个boot程序
   * TODO 这里应该有用户授权，取消开机启动
   */
  private fun unregister(mmid: Mmid) = this.registeredMmids.remove(mmid);
}
