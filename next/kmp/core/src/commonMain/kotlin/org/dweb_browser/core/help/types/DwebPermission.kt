package org.dweb_browser.core.help.types

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ImageResource

@Serializable
data class DwebPermission(
  /**
   * 权限的全局唯一标识，不可重复
   * 比如 gaubee.dweb/info
   *
   * 必须是包含 module.id 为前缀，如果缺省，那么直接使用 "$module.id/"
   *
   */
  val pid: String? = null,
  /**
   * 权限捕捉的路由，比如：
   * file://gaubee.com.dweb/info
   *
   * 其中，协议必须是 file，host 必须和 module 的 id 一致。
   * 这里要求写完整，因为难免未来会作出其它协议的支持，比如 dweb://deeplink 协议、比如 https://\*.dweb 的请求
   *
   * 这里使用“前缀路径捕捉”，也就是说 file://gaubee.com.dweb/info\* 与 都会被被拦截。如果想拦截全部，请编写 file://gaubee.com.dweb/ (PS: 这里末尾的`/`可以缺省，但缺省不意味着能捕捉 file://gaubee.com.dweb.gaubee2.com.dweb)
   *
   */
  val routes: List<String>,
  /**
   * 徽章，和icon类似，但是是附着在 module.icon 的右下方，如果不提供，默认使用 module.icon 来作为图标
   */
  val badges: List<ImageResource> = listOf(),
  /**
   * 权限标题，如果没有提供，默认使用 manifest.name
   */
  val title: String? = null,
  /**
   * 权限描述
   */
  val description: String? = null,
)