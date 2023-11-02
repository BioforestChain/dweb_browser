package org.dweb_browser.core.std.permission

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.createStore

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
      recordMap.remove(applicantMmid)

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