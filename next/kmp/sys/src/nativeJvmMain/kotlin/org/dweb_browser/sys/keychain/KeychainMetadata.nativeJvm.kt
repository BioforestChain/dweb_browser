package org.dweb_browser.sys.keychain

import org.dweb_browser.keychainstore.keychainGetItem
import org.dweb_browser.keychainstore.keychainItemKeys
import org.dweb_browser.keychainstore.keychainSetItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.core.help.types.MMID


@OptIn(ExperimentalSerializationApi::class)
internal class EnumKeysPolyfill : KeysManager(KeychainStoreMmidsManager()) {
  override fun initKeys(remoteMmid: MMID): MutableSet<String> {
    val keys = keychainGetItem(MmidsManager.METADATA, remoteMmid) ?: return mutableSetOf()
    return Cbor.decodeFromByteArray(keys)
  }

  override fun saveKeys(remoteMmid: MMID, keys: Set<String>) {
    keychainSetItem(MmidsManager.METADATA, remoteMmid, Cbor.encodeToByteArray(keys))
  }
}

internal class EnumKeys : KeysManager(KeychainStoreMmidsManager()) {
  override fun initKeys(remoteMmid: MMID): MutableSet<String> {
    return keychainItemKeys("Dweb $remoteMmid").toMutableSet()
  }

  override fun saveKeys(remoteMmid: MMID, keys: Set<String>) {}
}

@OptIn(ExperimentalSerializationApi::class)
internal class KeychainStoreMmidsManager : MmidsManager() {

  override fun initMmids(): MutableSet<String> {
    val mmidsRaw = keychainGetItem(METADATA, MMIDS) ?: return mutableSetOf()
    return Cbor.decodeFromByteArray(mmidsRaw)
  }

  override fun saveMmid() {
    keychainSetItem(METADATA, MMIDS, Cbor.encodeToByteArray(mmids))
  }
}
