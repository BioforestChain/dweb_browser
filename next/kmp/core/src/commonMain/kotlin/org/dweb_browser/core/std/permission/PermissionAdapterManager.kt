package org.dweb_browser.core.std.permission

import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.permission.AuthorizationRecord.Companion.generateAuthorizationRecord
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

/**
 * 权限提供商
 * 一般来说，如果一个模块声明了类型是 Service
 */
class PermissionProvider(
  /**
   * 权限提供者
   */
  val providerModule: IMicroModuleManifest,
  /**
   * 权限的唯一标识
   */
  val pid: PERMISSION_ID,
  /**
   * 权限捕捉的路由，比如：
   * file://gaubee.com.dweb/info
   *
   * 这里的 routes 是根据规则已经处理过的，可以直接用 startsWith 来匹配使用
   *
   */
  val routes: List<String>,
  /**
   * 徽章
   */
  val badges: List<StrictImageResource>,
  /**
   * 权限标题，可以理解成短语，用户在熟悉详情后，可以通过识别短语来快速判断申请的权限信息
   */
  val title: SimpleI18nResource,
  /**
   * 权限描述，用户第一次进行授权时，会默认展开的提示。未来默认收起，但是可以点击title展开
   */
  val description: SimpleI18nResource? = null,
) {
  /**
   * 权限提供者
   */
  val providerMmid get() = providerModule.mmid

  fun getAuthorizationRecord(granted: Boolean, applicantMmid: MMID) =
    generateAuthorizationRecord(pid, applicantMmid, granted)

  companion object {
    fun DwebPermission.toProvider(providerModule: IMicroModuleManifest, baseUrl: String? = null) =
      from(providerModule, this, baseUrl)

    fun from(
      providerModule: IMicroModuleManifest, permission: DwebPermission, baseUrl: String? = null
    ): PermissionProvider? {
      val pid = permission.pid ?: providerModule.mmid
      if (!(pid == providerModule.mmid || pid.startsWith("${providerModule.mmid}/"))) {
        return null
      }
      val routes = permission.routes.mapNotNull {
        URLBuilder(it).run {
          if (host != providerModule.mmid) {
            return@run null
          }
          if (encodedPath.isEmpty()) {
            encodedPath = "/"
          }
          buildUnsafeString()
        }
      }
      if (routes.isEmpty()) {
        return null
      }
      return PermissionProvider(
        providerModule = providerModule,
        pid = pid,
        routes = routes,
        badges = permission.badges.map { StrictImageResource.from(it, baseUrl) },
        title = SimpleI18nResource(
          Language.current to (permission.title ?: providerModule.name),
          ignoreWarn = true
        ),
        description = permission.description?.let {
          SimpleI18nResource(
            Language.current to it,
            ignoreWarn = true
          )
        },
      )
    }
  }
}

/**
 * 权限适配器管理器
 * 这个适配器只适用于内部构建使用，不支持运行时动态注册
 */
class PermissionAdapterManager : AdapterManager<PermissionProvider>() {
  private val pidMap = mutableMapOf<PERMISSION_ID, PermissionProvider>()
  fun getByPid(pid: PERMISSION_ID) = pidMap[pid]
  override fun append(order: Int, adapter: PermissionProvider): () -> Boolean {
    if (pidMap.containsKey(adapter.pid)) {
      throw Exception("permission id:'${adapter.pid}' already registered")
    }

    return super.append(order, adapter).also { pidMap[adapter.pid] = adapter }
  }

  override fun remove(adapter: PermissionProvider): Boolean {
    return super.remove(adapter).also { pidMap.remove(adapter.pid) }
  }
}

val permissionAdapterManager = PermissionAdapterManager()