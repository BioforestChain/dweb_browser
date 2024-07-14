package org.dweb_browser.sys.keychain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.platform.DeviceKeyValueStore
import org.dweb_browser.helper.utf8Binary

class AndroidDeviceKeyValueStoreManager {
  private val storeMaps = mutableMapOf<MMID, DeviceKeyValueStore>()
  fun getStore(mmid: MMID) =
    storeMaps.getOrPut("Keychain $mmid") { DeviceKeyValueStore("Keychain $mmid") }
}

internal class AndroidKeysManager(val storeManager: AndroidDeviceKeyValueStoreManager) :
  KeysManager(AndroidMmidsManager()) {
  override fun initKeys(remoteMmid: MMID): MutableSet<String> {
    val store = storeManager.getStore(remoteMmid)
    return store.getKeys().toMutableSet()
  }

  override fun saveKeys(remoteMmid: MMID, keys: Set<String>) {
  }
}

@OptIn(ExperimentalSerializationApi::class)
internal class AndroidMmidsManager : MmidsManager() {
  private val store = DeviceKeyValueStore(METADATA)
  override fun initMmids(): MutableSet<String> {
    return store.getRawItem(MMIDS.utf8Binary)?.let { Cbor.decodeFromByteArray(it) }
      ?: mutableSetOf()
  }

  override fun saveMmid() {
    store.setRawItem(MMIDS.utf8Binary, Cbor.encodeToByteArray(mmids))
  }
}