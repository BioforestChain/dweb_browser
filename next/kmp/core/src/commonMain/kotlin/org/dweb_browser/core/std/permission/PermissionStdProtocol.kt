package org.dweb_browser.core.std.permission

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.permission.ext.requestPermission
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.some
import org.dweb_browser.helper.toJsonElement

interface PermissionHooks {
  suspend fun onRequestPermissions(
    applicantIpc: Ipc, permissions: List<PermissionProvider>
  ): Map<PermissionProvider, AuthorizationRecord>
}

private inline fun <T> PureRequest.mapOfPermissions(valueSelector: (PERMISSION_ID) -> T) =
  query("permissions").split(',').associateWith(valueSelector)

/**
 * 权限模块需要附着到原生模块上才能完整，这里只提供一些基本标准
 */
suspend fun NativeMicroModule.permissionStdProtocol(hooks: PermissionHooks): PermissionTable {
  /**
   * 权限注册表
   */
  val permissionTable = PermissionTable(this)

  nativeFetchAdaptersManager.append(100) { fromMM, request ->
    if (request.url.protocol.name == "file" && request.url.host.endsWith(".dweb")
      // 如果是提供者请求提供者，那么直接跳过，这里只处理客户请求提供者
      && fromMM.mmid != request.url.host
    ) {
      for (adapter in permissionAdapterManager.adapters) {
        if (
        // 首先进行 host 判断
          adapter.providerMmid == request.url.host &&
          // 然后再进行路由判断
          adapter.routes.some { request.href.startsWith(it) }
        ) {
          /// 首先进行查询
          val authStatus = when (val authStatus = permissionTable.query(
            adapter.providerMmid, adapter.pid
          )[fromMM.mmid]) {
            AuthorizationStatus.UNKNOWN, null ->
              /// 根据查询结果，尝试直接帮它进行权限申请，如果可以的话
              if (fromMM is NativeMicroModule) fromMM.requestPermission(adapter.pid)
              else AuthorizationStatus.UNKNOWN

            else -> authStatus
          }

          return@append if (AuthorizationStatus.GRANTED == authStatus)
          // 授权成功，直接返回null，让路由接着往下走
            null
          // 否则返回未认证的异常
          else PureResponse(
            HttpStatusCode.Forbidden,
            body = IPureBody.from("Insufficient permissions: ${authStatus.status}")
          )
        }
      }

      null
    } else null
  }

  /**
   * 请求锁
   */
  val requestsLock = ReasonLock()
  protocol("permission.std.dweb") {
    routes(
      /**
       * 服务者 查询权限的授权情况。
       * 这里只能查询“模块自己定义的权限”
       */
      "/query" bind HttpMethod.Get by defineJsonResponse {
        debugPermission("query", "enter")
        request.mapOfPermissions { permission ->
          permissionTable.query(ipc.remote.mmid, permission)
        }.toJsonElement()
      },
      /**
       * 请求者 检查权限的状态。
       * 这里只能检查“请求者的权限状态”，无法查询其它模块的状态
       */
      "/check" bind HttpMethod.Get by defineJsonResponse {
        debugPermission("check", "enter")
        request.mapOfPermissions { permission ->
          permissionTable.check(ipc.remote.mmid, permission)
        }.toJsonElement()
      },
      /**
       * 请求者 申请权限
       */
      "/request" bind HttpMethod.Get by defineJsonResponse {
        debugPermission("request", "enter")
        /**
         * 需要通过hooks询问结果的
         */
        val needAskPermissions = mutableListOf<PermissionProvider>()

        val lockPrefix = "${ipc.remote.mmid}::"
        // 这里授权需要上锁，避免重复发起
        val unlockTokens =
          requestsLock.lock(request.mapOfPermissions { pid -> "$lockPrefix$pid" }.values)

        val result = request.mapOfPermissions { permission ->
          // 首先查询授权状态，如果是拒绝或者是未知
          when (val status = permissionTable.check(ipc.remote.mmid, permission)) {
            AuthorizationStatus.UNKNOWN -> {
              when (val provider = permissionAdapterManager.getByPid(permission)) {
                // 如果没有适配器，返回拒绝
                null -> AuthorizationStatus.DENIED
                // 将会询问 hooks，目前先定义为“未知”
                else -> {
                  needAskPermissions.add(provider);
                  AuthorizationStatus.UNKNOWN
                }
              }
            }

            else -> status
          }
        }.toMutableMap();
        /// 将需要询问的权限交给钩子，等得到授权结果。更新result表；并保存
        if (needAskPermissions.isNotEmpty()) {
          // 询问授权结果
          for ((provider, record) in hooks.onRequestPermissions(ipc, needAskPermissions)) {
            result[provider.pid] = record.status
            permissionTable.addRecord(record)
          }
        }
        // 解锁
        requestsLock.unlock(unlockTokens)
        // 返回最终结果
        result.toJsonElement()
      },
      /**
       * 服务者 撤销授权记录
       * 这样会导致下一次调用强制询问的发生
       */
      "/delete" bind HttpMethod.Get by defineJsonResponse {
        debugPermission("delete", "enter")
        val providerMmid = ipc.remote.mmid
        val applicantMmid = request.query("mmid")
        request.mapOfPermissions { permission ->
          permissionTable.removeRecord(providerMmid, permission, applicantMmid)
        }.toJsonElement()
      },
    )
  }

  return permissionTable
}