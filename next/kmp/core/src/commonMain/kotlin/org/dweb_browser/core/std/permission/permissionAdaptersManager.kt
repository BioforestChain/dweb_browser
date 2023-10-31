package org.dweb_browser.core.std.permission

import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.compose.SimpleI18nResource


/**
 * 权限提供商
 * 一般来说，如果一个模块声明了类型是 Service
 */
class PermissionProvider(
  val module: MicroModule,
  /**
   * 权限捕捉的路由，比如：
   * file://gaubee.com.dweb/info
   *
   */
  val routes: List<String>,
  /**
   * 徽章
   */
  val badge: String? = null,
  /**
   * 权限标题，可以理解成短语，用户在熟悉详情后，可以通过识别短语来快速判断申请的权限信息
   */
  val title: SimpleI18nResource,
  /**
   * 权限描述，用户第一次进行授权时，会默认展开的提示。未来默认收起，但是可以点击title展开
   */
  val description: SimpleI18nResource? = null
)

/**
 * 权限适配器管理器
 * 这个适配器只适用于内部构建使用，不支持运行时动态注册
 */
val permissionAdapterManager = AdapterManager<PermissionProvider>()