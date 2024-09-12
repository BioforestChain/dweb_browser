package org.dweb_browser.sys.keychain

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.dweb_browser.core.help.types.MMID

internal abstract class KeysManager(private val mmidsManager: MmidsManager) : SynchronizedObject() {
  protected abstract fun initKeys(remoteMmid: MMID): MutableSet<String>
  private val keysMap = mutableMapOf<String, MutableSet<String>>()
  fun getKeys(remoteMmid: MMID) = synchronized(this) {
    keysMap[remoteMmid] ?: initKeys(remoteMmid).also { keysMap[remoteMmid] = it }
  }

  fun getMmids() = mmidsManager.mmids.toList()


  protected abstract fun saveKeys(remoteMmid: MMID, keys: Set<String>)

  fun addKey(remoteMmid: MMID, key: String) {
    val keys = getKeys(remoteMmid)
    if (keys.add(key)) {
      saveKeys(remoteMmid, keys)
    }
    mmidsManager.addMmid(remoteMmid)
  }

  fun removeKey(remoteMmid: MMID, key: String) {
    val keys = getKeys(remoteMmid)
    if (keys.remove(key)) {
      saveKeys(remoteMmid, keys)
      if (keys.isEmpty()) {
        mmidsManager.removeMmid(remoteMmid)
      }
    }
  }
}

internal abstract class MmidsManager : SynchronizedObject() {
  companion object {
    internal const val METADATA: String = "Dweb-Keychain-Metadata keychain.sys.dweb"
    internal const val MMIDS: String = "mmids"
  }

  val mmids by lazy { initMmids() }
  protected abstract fun initMmids(): MutableSet<String>
  protected abstract fun saveMmid()

  fun addMmid(mmid: MMID) {
    if (mmids.add(mmid)) {
      saveMmid()
    }
  }

  fun removeMmid(mmid: MMID) {
    if (mmids.remove(mmid)) {
      saveMmid()
    }
  }
}
