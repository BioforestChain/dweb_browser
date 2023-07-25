package org.dweb_browser.microservice.core

import org.dweb_browser.helper.MicroModuleCategory
import org.dweb_browser.helper.Mmid
import org.http4k.core.Request


interface BootstrapContext {
  val dns: DnsMicroModule
}

interface DnsMicroModule {
  /**
   * 动态安装应用
   */
  fun install(mm: MicroModule)

  /**
   * 动态卸载应用
   */
  fun uninstall(mm: Mmid): Boolean

  /**
   * 动态js应用查询
   */
  fun query(mmid: Mmid): MicroModule?

  /**
   * 根据类目搜索模块
   * > 这里暂时不需要支持复合搜索，未来如果有需要另外开接口
   * @param category
   */
  suspend fun search(category: MicroModuleCategory): MutableList<MicroModule>

  /**
   * 重启应用
   */
  suspend fun restart(mmid: Mmid)

  /**
   * 与其它应用建立连接
   */
  suspend fun connect(mmid: Mmid, reason: Request? = null): ConnectResult

  /**
   * 启动其它应用
   */
  suspend fun open(mmid: Mmid): Boolean

  /**
   * 关闭其他应用
   */
  suspend fun close(mmid: Mmid): Boolean
}