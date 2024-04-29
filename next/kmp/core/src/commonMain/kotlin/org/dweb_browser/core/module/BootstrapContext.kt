package org.dweb_browser.core.module

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMPT
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.pure.http.PureRequest


interface BootstrapContext {
  val dns: DnsApi
}

interface DnsApi {
  /**
   * 动态安装应用
   */
  suspend fun install(mm: MicroModule)

  /**
   * 动态卸载应用
   */
  suspend fun uninstall(mmpt: MMPT): Boolean

  /**
   * 动态js应用查询
   */
  fun query(mmpt: MMPT): MicroModule?

  /**
   * 根据类目搜索模块
   * > 这里暂时不需要支持复合搜索，未来如果有需要另外开接口
   * @param category
   */
  suspend fun search(category: MICRO_MODULE_CATEGORY): MutableList<MicroModule>

  /**
   * 重启应用
   */
  suspend fun restart(mmpt: MMPT)

  /**
   * 与其它应用建立连接
   */
  suspend fun connect(mmpt: MMPT, reason: PureRequest? = null): Ipc// ConnectResult

  /**
   * 启动其它应用
   */
  suspend fun open(mmpt: MMPT): Boolean

  /**
   * 关闭其他应用
   */
  suspend fun close(mmpt: MMPT): Boolean
}