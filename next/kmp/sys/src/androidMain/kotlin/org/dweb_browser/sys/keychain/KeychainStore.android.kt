package org.dweb_browser.sys.keychain

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.platform.DeviceKeyValueStore
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.sys.keychain.core.EncryptKey
import org.dweb_browser.sys.keychain.core.EncryptKeyV1
import org.dweb_browser.sys.keychain.core.UseKeyParams
import java.security.KeyStore

internal const val ANDROID_KEY_STORE = "AndroidKeyStore"
internal const val ANDROID_KEY_PROVIDER = "BC"
internal val deviceKeyStore = DeviceKeyValueStore(ANDROID_KEY_STORE)
internal val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }


actual class KeychainStore actual constructor(val runtime: MicroModule.Runtime) {
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

  private val encryptKey = SuspendOnce1 { params: UseKeyParams -> getOrRecoveryOrCreateKey(params) }


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

  actual suspend fun getItem(remoteMmid: MMID, key: String): ByteArray? {
    val store = storeManager.getStore(remoteMmid)
    return store.getRawItem(key.utf8Binary)?.let {
      decryptData(
        it, remoteMmid, buildUseKeyReason(
          remoteMmid = remoteMmid,
          title = "应用想要访问您的钥匙串中保存的密钥",
          description = "读取钥匙: $key"
        )
      )
    }
  }

  actual suspend fun setItem(remoteMmid: MMID, key: String, value: ByteArray): Boolean {
    val store = storeManager.getStore(remoteMmid)
    return runCatching {
      store.setRawItem(
        key.utf8Binary, encryptData(
          value, remoteMmid, buildUseKeyReason(
            remoteMmid = remoteMmid,
            title = "应用想要使用您的钥匙串保存密钥",
            description = "保存钥匙: $key"
          )
        )
      )
      keysManager.addKey(remoteMmid, key)
      true
    }.getOrDefault(false)
  }

  actual suspend fun hasItem(remoteMmid: MMID, key: String): Boolean {
    val store = storeManager.getStore(remoteMmid)
    return store.hasKey(key)
  }

  actual suspend fun deleteItem(remoteMmid: MMID, key: String): Boolean {
    val store = storeManager.getStore(remoteMmid)
    EncryptKey.getRootKey(
      buildUseKeyParams(
        remoteMmid = remoteMmid,
        title = "应用想要删除您的钥匙串保存密钥",
        description = "删除钥匙: $key"
      )
    )
    return store.removeKey(key).trueAlso {
      keysManager.removeKey(remoteMmid, key)
    }
  }

  private fun buildUseKeyParams(remoteMmid: MMID, title: String, description: String) =
    UseKeyParams(runtime, remoteMmid, buildUseKeyReason(remoteMmid, title, description))

  private fun buildUseKeyReason(remoteMmid: MMID, title: String, description: String) =
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
