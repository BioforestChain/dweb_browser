package org.dweb_browser.sys.keychain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.platform.DeviceKeyValueStore

class AndroidDeviceKeyValueStoreManager {
  private val storeMaps = mutableMapOf<MMID, DeviceKeyValueStore>()
  fun getStore(mmid: MMID) =
    storeMaps.getOrPut("Keychain $mmid") { DeviceKeyValueStore("Keychain $mmid") }
}

internal class AndroidKeysManager(val storeManager: AndroidDeviceKeyValueStoreManager) :
  KeysManager(AndroidMmidsManager()) {
  override fun initKeys(remoteMmid: MMID): MutableSet<String> {
    val store = storeManager.getStore(remoteMmid)
    return store.keys().toMutableSet()
  }

  override fun saveKeys(remoteMmid: MMID, keys: Set<String>) {
  }
}

@OptIn(ExperimentalSerializationApi::class)
internal class AndroidMmidsManager : MmidsManager() {
  private val store = DeviceKeyValueStore(METADATA)
  override fun initMmids(): MutableSet<String> {
    return store.getItem(MMIDS)?.let { Cbor.decodeFromByteArray(it) }
      ?: mutableSetOf()
  }

  override fun saveMmid() {
    store.setItem(MMIDS, Cbor.encodeToByteArray(mmids))
  }
}