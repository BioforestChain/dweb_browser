package org.dweb_browser.core.std.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.createStore

/**
 * 权限表
 * 该表中有两个索引： mmid 和 permissionId
 */
class PermissionTable(private val nmm: NativeMicroModule.NativeRuntime) {
  /**
   * 认证记录
   *
   * 这里跟着 PERMISSION_ID 走，应用在被卸载的时候，它对别人的授权记录会被删除，但是别人对它的授权记录还是保留
   */
  private val permissionStore = nmm.createStore("authorization", true)
  private val authorizationMap =
    mutableStateMapOf<PERMISSION_ID, MutableMap<MMID/* applicantMmid */, AuthorizationRecord>>()

  class PermissionRow(
    val permissionId: PERMISSION_ID,
    val applicantMmid: MMID,
    val record: AuthorizationRecord,
  )

  @Composable
  fun AllData(): List<PermissionRow> {
    nmm.debugMM("PermissionTable/AllData")
    // 订阅变动
    val all by remember {
      derivedStateOf {
        mutableListOf<PermissionRow>().also { all ->
          for ((permissionId, map) in authorizationMap) {
            for ((applicantMmid, record) in map) {
              all += PermissionRow(permissionId, applicantMmid, record)
            }
          }
        }
      }
    }

    return all
  }

  private val lock = Mutex(true)

  init {
    nmm.scopeLaunch(cancelable = false) {
      for ((pid, recordMap) in permissionStore.getAll<MutableMap<MMID, AuthorizationRecord>>()) {
        authorizationMap[pid] =
          mutableStateMapOf(*recordMap.map { it.key to it.value }.toTypedArray())
      }
      lock.unlock()
    }
  }

  suspend fun addRecord(record: AuthorizationRecord) = lock.withLock {
    nmm.debugMM("PermissionTable/addRecord", record)
    val recordMap = authorizationMap.getOrPut(record.pid) { mutableStateMapOf() }
    recordMap[record.applicantMmid] = record
    permissionStore.set(record.pid, recordMap)
  }

  suspend fun removeRecord(providerMmid: MMID, pid: PERMISSION_ID, applicantMmid: MMID) =
    lock.withLock {
      nmm.debugMM("PermissionTable/removeRecord", pid)
      val recordMap = authorizationMap[pid] ?: return@withLock false
      val record = recordMap[applicantMmid] ?: return@withLock false
      if (record.providerMmid != providerMmid) {
        return@withLock false
      }
      recordMap.remove(applicantMmid)

      if (recordMap.isEmpty()) {
        permissionStore.delete(pid)
      } else {
        permissionStore.set(pid, recordMap)
      }
      true
    }

  /**
   * 查询权限的授权情况
   */
  suspend fun query(providerMmid: MMID, pid: PERMISSION_ID) = lock.withLock {
    nmm.debugMM("PermissionTable/query", pid)
    authorizationMap[pid]?.filter { it.value.providerMmid == providerMmid }
      ?.mapValues { it.value.safeStatus } ?: mapOf()
  }

  /**
   * 请求者 检查权限的状态
   */
  suspend fun check(applicantMmid: MMID, pid: PERMISSION_ID) = lock.withLock {
    nmm.debugMM("PermissionTable/check", pid)
    authorizationMap[pid]?.get(applicantMmid)?.safeStatus ?: AuthorizationStatus.UNKNOWN
  }

}