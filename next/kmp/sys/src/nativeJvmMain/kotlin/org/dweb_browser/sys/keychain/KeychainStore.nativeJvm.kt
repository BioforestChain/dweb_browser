package org.dweb_browser.sys.keychain

import keychainstore.keychainDeleteItem
import keychainstore.keychainGetItem
import keychainstore.keychainHasItem
import keychainstore.keychainSetItem
import keychainstore.keychainSupportEnumKeys
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.trueAlso

actual class KeychainStore actual constructor(val runtime: KeychainNMM.KeyChainRuntime) {
  companion object {
    private val supportEnumKeys = keychainSupportEnumKeys()
    private val enumKeys = when {
      supportEnumKeys -> EnumKeys()
      else -> EnumKeysPolyfill()
    }
  }

  @Throws(Exception::class)
  actual suspend fun getItem(remoteMmid: MMID, key: String): ByteArray? {
    if (!hasItem(remoteMmid, key)) {
      return null
    }
    tryThrowUserRejectAuth(
      runtime = runtime,
      remoteMmid = remoteMmid,
      title = KeychainI18nResource.keychain_get_title.text,
      description = KeychainI18nResource.keychain_get_description.text(key)
    )
    return keychainGetItem("Dweb $remoteMmid", key)
  }

  actual suspend fun setItem(
    remoteMmid: MMID,
    key: String,
    value: ByteArray,
  ): Boolean {
    runCatching {
      tryThrowUserRejectAuth(
        runtime = runtime,
        remoteMmid = remoteMmid,
        title = KeychainI18nResource.keychain_set_title.text,
        description = KeychainI18nResource.keychain_set_description.text(key),
      )
    }.getOrElse { return false }

    return keychainSetItem("Dweb $remoteMmid", key, value).trueAlso {
      enumKeys.addKey(remoteMmid, key)
    }
  }


  actual suspend fun hasItem(remoteMmid: MMID, key: String): Boolean {
    return keychainHasItem("Dweb $remoteMmid", key)
  }

  actual suspend fun deleteItem(remoteMmid: MMID, key: String): Boolean {
    if (!hasItem(remoteMmid, key)) {
      return false
    }
    runCatching {
      tryThrowUserRejectAuth(
        runtime = runtime,
        remoteMmid = remoteMmid,
        title = KeychainI18nResource.keychain_delete_title.text,
        description = KeychainI18nResource.keychain_delete_description.text(key),
      )
    }.getOrElse { return false }

    return keychainDeleteItem("Dweb $remoteMmid", key).trueAlso {
      enumKeys.removeKey(remoteMmid, key)
    }
  }

  actual suspend fun keys(remoteMmid: MMID): List<String> {
    return enumKeys.getKeys(remoteMmid).toList()
  }

  actual suspend fun mmids(): List<MMID> {
    return enumKeys.getMmids()
  }

}

/**
 * 尝试抛出用户拒绝认证的信息
 */
expect suspend fun tryThrowUserRejectAuth(
  runtime: KeychainNMM.KeyChainRuntime,
  remoteMmid: MMID,
  title: String,
  description: String,
)