package org.dweb_browser.core.std.permission

import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.toJsonElement

/**
 * 权限表
 * 该表中有两个索引： mmid 和 permissionId
 */
class PermissionTable(private val nmm: NativeMicroModule) {
  /**
   * 认证记录
   *
   * 这里跟着 PERMISSION_ID 走，应用在被卸载的时候，它对别人的授权记录会被删除，但是别人对它的授权记录还是保留
   */
  private val store = nmm.createStore("authorization", true)
  private val authorizationMap = mutableMapOf<PERMISSION_ID, MutableMap<
      /** applicantMmid */
      MMID, AuthorizationRecord>>()

  private suspend fun queryMicroModule(mmid: MMID) = nmm.bootstrapContext.dns.query(mmid)

  private val lock = Mutex(true)

  init {
    nmm.ioAsyncScope.launch {
      for ((pid, recordMap) in store.getAll<MutableMap<MMID, AuthorizationRecord>>()) {
        authorizationMap[pid] = recordMap
      }
      lock.unlock()
    }
  }

  suspend fun addRecord(record: AuthorizationRecord) = lock.withLock {
    val recordMap = authorizationMap.getOrPut(record.pid) { mutableMapOf() }
    recordMap[record.applicantMmid] = record
    store.set(record.pid, recordMap)
  }

  suspend fun removeRecord(providerMmid: MMID, pid: PERMISSION_ID, applicantMmid: MMID) =
    lock.withLock {
      val recordMap = authorizationMap[pid] ?: return@withLock false
      val record = recordMap[applicantMmid] ?: return@withLock false
      if (record.providerMmid != providerMmid) {
        return@withLock false
      }
      recordMap.remove(pid)

      if (recordMap.isEmpty()) {
        store.delete(pid)
      } else {
        store.set(pid, recordMap)
      }
      true
    }

  /**
   * 查询权限的授权情况
   */
  suspend fun query(providerMmid: MMID, pid: PERMISSION_ID) = lock.withLock {
    authorizationMap[pid]?.filter { it.value.providerMmid == providerMmid }
      ?.mapValues { it.value.safeStatus } ?: mapOf()
  }

  /**
   * 请求者 检查权限的状态
   */
  suspend fun check(applicantMmid: MMID, pid: PERMISSION_ID) = lock.withLock {
    authorizationMap[pid]?.get(applicantMmid)?.safeStatus ?: AuthorizationStatus.UNKNOWN
  }

}

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
  val permissionTable = PermissionTable(this)
  val requestsLock = ReasonLock()
  protocol("permission.std.dweb") {
    routes(
      /**
       * 服务者 查询权限的授权情况。
       * 这里只能查询“模块自己定义的权限”
       */
      "/query" bind HttpMethod.Get to defineJsonResponse {
        request.mapOfPermissions { permission ->
          permissionTable.query(ipc.remote.mmid, permission)
        }.toJsonElement()
      },
      /**
       * 请求者 检查权限的状态。
       * 这里只能检查“请求者的权限状态”，无法查询其它模块的状态
       */
      "/check" bind HttpMethod.Get to defineJsonResponse {
        request.mapOfPermissions { permission ->
          permissionTable.check(ipc.remote.mmid, permission)
        }.toJsonElement()
      },
      /**
       * 请求者 申请权限
       */
      "/request" bind HttpMethod.Get to defineJsonResponse {
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
      "/delete" bind HttpMethod.Get to defineJsonResponse {
        val providerMmid = ipc.remote.mmid
        val applicantMmid = request.query("mmid")
        request.mapOfPermissions { permission ->
          permissionTable.removeRecord(providerMmid, permission, applicantMmid)
        }.toJsonElement()
      },
    )
  }

  permissionAdapterManager.onChange {

  }
  for (permissionProvider in permissionAdapterManager.adapters) {
//    permissionTable.
  }
  return permissionTable
}