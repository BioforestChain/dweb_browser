package org.dweb_browser.sys.keychain

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule

expect class KeychainStore(runtime: MicroModule.Runtime) {
  suspend fun getItem(remoteMmid: MMID, key: String): ByteArray?
  suspend fun setItem(remoteMmid: MMID, key: String, value: ByteArray): Boolean
  suspend fun hasItem(remoteMmid: MMID, key: String): Boolean
  suspend fun deleteItem(remoteMmid: MMID, key: String): Boolean
  suspend fun keys(remoteMmid: MMID): List<String>
  suspend fun mmids(): List<MMID>
}