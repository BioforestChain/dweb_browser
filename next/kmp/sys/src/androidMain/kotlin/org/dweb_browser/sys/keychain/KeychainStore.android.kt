package org.dweb_browser.sys.keychain

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.keychain.core.EncryptKey
import org.dweb_browser.sys.keychain.core.EncryptKeyV1
import org.dweb_browser.sys.keychain.core.UseKeyParams

internal const val ANDROID_KEY_STORE = "AndroidKeyStore"

actual class KeychainStore actual constructor(val runtime: KeychainNMM.KeyChainRuntime) {
  companion object {
    private val getOrRecoveryList = listOf(EncryptKeyV1.recoveryKey)
    private val currentGenerator = EncryptKeyV1.generateKey
    private suspend fun getOrRecoveryOrCreateKey(params: UseKeyParams): EncryptKey {
      for (getter in getOrRecoveryList) {
        getter(params)?.also {
          return it
        }
      }
      return currentGenerator(params)
    }
  }

  private val encryptKey = SuspendOnce1({
    runCatching {
      getResult()
    }.getOrElse {
      reset(doCancel = false)
    }
  }) { params: UseKeyParams -> getOrRecoveryOrCreateKey(params) }


  /**
   * 数据加密
   */
  suspend fun encryptData(
    sourceData: ByteArray,
    remoteMmid: MMID,
    reason: UseKeyParams.UseKeyReason,
  ): ByteArray {
    val params = UseKeyParams(runtime, remoteMmid, reason)
    val encryptKey = encryptKey(params)
    return encryptKey.encryptData(params, sourceData)
  }

  /**
   * 数据解密
   */
  suspend fun decryptData(
    encryptedBytes: ByteArray,
    remoteMmid: MMID,
    reason: UseKeyParams.UseKeyReason,
  ): ByteArray {
    val params = UseKeyParams(runtime, remoteMmid, reason)
    val encryptKey = encryptKey(params)
    return encryptKey.decryptData(params, encryptedBytes)
  }

  //  init {
//    globalDefaultScope.launch {
//      getItem("keychain.sys.dweb", "test-crypto")?.also { encryptBytes ->
//        println("QAQ test-crypto/start: ${encryptBytes.base64String}")
//        runCatching {
//          println("QAQ test-crypto/load: ${decryptData(runtime.mmid, encryptBytes).utf8String}")
//        }.getOrElse { err ->
//          println(err.stackTraceToString().split('\n')
//            .joinToString("\n") { "QAQ test-crypto/load-error: $it" })
//        }
//      }
//      val data = "Time: ${now()}"
//      println("QAQ test-crypto/save: $data")
//      runCatching {
//        val encryptBytes = encryptData(runtime.mmid, data.utf8Binary);
//        setItem("keychain.sys.dweb", "test-crypto", encryptBytes)
//        println("QAQ test-crypto/done: ${encryptBytes.base64String}")
//      }.getOrElse { err ->
//        println(err.stackTraceToString().split('\n')
//          .joinToString("\n") { "QAQ test-crypto/save-error: $it" })
//      }
//    }
//  }
  private val storeManager = AndroidDeviceKeyValueStoreManager()
  private val keysManager = AndroidKeysManager(storeManager)

  @Throws(Exception::class)
  actual suspend fun getItem(remoteMmid: MMID, key: String): ByteArray? {
    val store = storeManager.getStore(remoteMmid)
    return store.getItem(key)?.let {
      decryptData(
        it, remoteMmid, buildUseKeyReason(
          remoteMmid = remoteMmid,
          title = KeychainI18nResource.keychain_get_title.text,
          description = KeychainI18nResource.keychain_get_description.text(key)
        )
      )
    }
  }

  actual suspend fun setItem(remoteMmid: MMID, key: String, value: ByteArray): Boolean {
    val store = storeManager.getStore(remoteMmid)
    return runCatching {
      store.setItem(
        key, encryptData(
          value, remoteMmid, buildUseKeyReason(
            remoteMmid = remoteMmid,
            title = KeychainI18nResource.keychain_set_title.text,
            description = KeychainI18nResource.keychain_set_description.text(key)
          )
        )
      )
      keysManager.addKey(remoteMmid, key)
      true
    }.getOrDefault(false)
  }

  actual suspend fun hasItem(remoteMmid: MMID, key: String): Boolean {
    val store = storeManager.getStore(remoteMmid)
    return store.hasItem(key)
  }

  actual suspend fun deleteItem(remoteMmid: MMID, key: String): Boolean {
    val store = storeManager.getStore(remoteMmid)
    if (!hasItem(remoteMmid, key)) {
      return false
    }
    /// 这里只是要获得用户的授权
    runCatching {
      EncryptKey.getRootKey(
        buildUseKeyParams(remoteMmid = remoteMmid,
          title = KeychainI18nResource.keychain_delete_title.text,
          description = KeychainI18nResource.keychain_delete_description.text(key)),
      )
    }.getOrElse { return false }
    return store.deleteItem(key).trueAlso {
      keysManager.removeKey(remoteMmid, key)
    }
  }

  private suspend fun buildUseKeyParams(remoteMmid: MMID, title: String, description: String) =
    UseKeyParams(runtime, remoteMmid, buildUseKeyReason(remoteMmid, title, description))

  private suspend fun buildUseKeyReason(remoteMmid: MMID, title: String, description: String) =
    UseKeyParams.UseKeyReason(
      title = title,
      subtitle = "${runtime.bootstrapContext.dns.query(remoteMmid)?.name} ($remoteMmid)",
      description = description,
    )

  actual suspend fun keys(remoteMmid: MMID): List<String> {
    return keysManager.getKeys(remoteMmid).toList()
  }

  actual suspend fun mmids(): List<MMID> {
    return keysManager.getMmids()
  }
}
