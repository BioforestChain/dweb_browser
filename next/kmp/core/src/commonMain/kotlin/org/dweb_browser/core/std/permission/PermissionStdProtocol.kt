package org.dweb_browser.core.std.permission

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureClientRequest
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.NativeMicroModule
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

/*
  nativeFetchAdaptersManager.append(100) { fromMM, request ->
    if (
    // 如果是提供者请求提供者，那么直接跳过，这里只处理客户请求提供者
      fromMM.mmid != request.url.host &&
      // 目前只拦截 dweb 协议
      request.url.protocol.name == "file" && request.url.host.endsWith(".dweb")
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
*/

  /**
   * key: a -> b
   * connect: a -> p -> b
   * forward: b-ipc1
   *
   * key: d -> b
   * connect: d -> p -> b
   * forward: b-ipc2
   *
   * key: a -> c
   * connect: a -> p -> c
   *
   */
  val ignoreMM = listOf("permission.sys.dweb", "permission.std.dweb", "file.std.dweb")
  onConnect { (clientIpc, reason) ->
    // 一个是为了自启动permission，另一个是构建PermissionTable时产生的调用（这个如果下面执行 connect 会死循环）
    debugPermission("onConnect", "enter -> ${clientIpc.remote.mmid}, $reason")
    if (ignoreMM.contains(clientIpc.remote.mmid) || ignoreMM.contains(reason.url.host)) return@onConnect
    /**
     * 判断当前请求的 reason mmid 是否是 permission，其中包括 dweb_protocols 也不能包含
     * 是：那么直接运行即可
     * 否：先获取permission和reason之间的连接，然后判断 permissionAdapterManager 是否运行访问，如果允许
     *    则可以跳转，如果不允许，则不跳转
     */
    val nextMMID = reason.url.host
    if (nextMMID != mmid && !dweb_protocols.contains(nextMMID)) {
      val forwardReason =
        PureClientRequest.fromJson("file://$nextMMID", IpcMethod.CONNECT, forwardReason)
      val forwardIpc = connect(nextMMID, forwardReason)
      /**
       * 如果授权成功，即可进行请求
       */
      clientIpc.onRequest { (ipcMessage, ipc) ->
        debugPermission("clientIpc.onRequest", "enter")
        for (adapter in permissionAdapterManager.adapters) {
          if (adapter.providerMmid == reason.url.host && // 首先进行 host 判断
            adapter.routes.some { reason.href.startsWith(it) } // 然后再进行路由判断
          ) {
            val clientNMM = bootstrapContext.dns.query(clientIpc.remote.mmid)
            /// 首先进行查询
            val authStatus = when (val authStatus = permissionTable.query(
              adapter.providerMmid, adapter.pid
            )[clientIpc.remote.mmid]) {
              AuthorizationStatus.UNKNOWN, null ->
                /// 根据查询结果，尝试直接帮它进行权限申请，如果可以的话
                if (clientNMM is NativeMicroModule) clientNMM.requestPermission(adapter.pid)
                else AuthorizationStatus.UNKNOWN

              else -> authStatus
            }

            if (AuthorizationStatus.GRANTED == authStatus) { // 授权成功，或者无需授权，让路由接着往下走
              debugPermission("clientIpc.onMessage", "permission success")
              forwardIpc.postMessage(ipcMessage)
            } else { // 否则返回未认证的异常
              debugPermission("clientIpc.onMessage", "permission fail")
              PureResponse(
                HttpStatusCode.Forbidden,
                body = IPureBody.from("Insufficient permissions: ${authStatus.status}")
              )
            }
          }
        }
      }

      forwardIpc.onMessage { (ipcMessage, _) ->
        debugPermission("forwardIpc.onMessage", "enter")
        clientIpc.postMessage(ipcMessage)
      }
    }
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