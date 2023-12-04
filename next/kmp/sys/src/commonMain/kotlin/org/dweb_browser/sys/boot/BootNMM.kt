package org.dweb_browser.sys.boot

import io.ktor.http.HttpMethod
import org.dweb_browser.helper.printDebug
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.Router
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.std.dns.ext.onActivity

fun debugBoot(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("boot", tag, msg, err)

class BootNMM(initMmids: List<MMID>? = null) :
  NativeMicroModule("boot.sys.dweb", "Boot Management") {
  init {
    short_name = "Boot";
    categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Hub_Service)
  }

  /**
   * 开机启动项注册表
   * TODO 这里需要从数据库中读取
   */
  private val registeredMmids = mutableSetOf<MMID>()

  init {
    if (initMmids != null) {
      registeredMmids += initMmids
    }
  }

  override val routers: Router = mutableMapOf()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes("/register" bind HttpMethod.Get by defineBooleanResponse {
      register(ipc.remote.mmid)
    }, "/unregister" bind HttpMethod.Get by defineBooleanResponse {
      unregister(ipc.remote.mmid)
    })
    for (mmid in registeredMmids) {
      debugBoot("open", mmid)
      bootstrapContext.dns.open(mmid)
    }
    onActivity { (event, ipc) ->
      // 只响应 dns 模块的激活事件
      if (ipc.remote.mmid == "dns.std.dweb") {
        for (mmid in registeredMmids) {
          debugBoot("activity", mmid)
          bootstrapContext.dns.connect(mmid).ipcForFromMM.postMessage(event)
        }
      }
    }
  }


  override suspend fun _shutdown() {
    routers.clear()
  }

  /**
   * 注册一个boot程序
   * TODO 这里应该有用户授权，允许开机启动
   */
  private fun register(mmid: MMID) = this.registeredMmids.add(mmid);

  /**
   * 移除一个boot程序
   * TODO 这里应该有用户授权，取消开机启动
   */
  private fun unregister(mmid: MMID) = this.registeredMmids.remove(mmid);
}
