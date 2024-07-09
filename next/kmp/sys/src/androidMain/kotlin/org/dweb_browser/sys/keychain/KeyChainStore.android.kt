package org.dweb_browser.sys.keychain

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import org.dweb_browser.helper.now
import org.dweb_browser.helper.platform.DeviceKeyValueStore
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec


private const val ANDROID_KEY_STORE = "AndroidKeyStore"

private const val TRANSFORMATION = "AES/GCM/NoPadding"

class CryptoManager {

  private val alias = "keychainstore-key"


  private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
    load(null)
  }

  private val cryptKey =
    (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: createKey()

  private val keyInfo = runCatching {
    SecretKeyFactory.getInstance(cryptKey.algorithm, ANDROID_KEY_STORE)
      .getKeySpec(cryptKey, KeyInfo::class.java) as KeyInfo
  }.getOrNull()

  init {
    keyInfo?.also { keyInfo ->
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        println("QAQ keyInfo.securityLevel=${keyInfo.securityLevel}")
      } else {
        println("QAQ keyInfo.isInsideSecureHardware=${keyInfo.isInsideSecureHardware}")
      }
    }
  }

  private fun createKey(): SecretKey {
    val keyGenerator = KeyGenerator
      // AES 对称加密 ； EC 非对称加密
      .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

    keyGenerator.init(
      KeyGenParameterSpec.Builder(
        alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM) // 目前支持的模式里，算是最先进的，确保安全性的同时性能还不错；它的缺点是“实现相对复杂，需要额外的硬件或优化软件支持”。
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setIsStrongBoxBacked(true) // 需要存储到硬件中
        .setUserPresenceRequired(true)
        .setUserAuthenticationRequired(true)
        .build()
    )

    return keyGenerator.generateKey()
  }


  lateinit var iv: ByteArray

  /**
   * 数据加密
   */
  fun encryptData(sourceData: ByteArray): ByteArray {
    val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, cryptKey)

    iv = cipher.getIV();
    val byteArray = cipher.doFinal(sourceData)
    return byteArray
  }

  /**
   * 数据解密
   */
  fun decryptData(encryptedBytes: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.DECRYPT_MODE, cryptKey, spec)

    return cipher.doFinal(encryptedBytes)
  }
}

actual class KeyChainStore {
  private val cryptoManager = CryptoManager()

  init {

    getItem("keychain.sys.dweb", "test-crypto")?.also {
      println("QAQ test-crypto/load: ${cryptoManager.decryptData(it).utf8String}")
    }
    val data = "Time: ${now()}"
    println("QAQ QAQ test-crypto/save: $data")
    setItem("keychain.sys.dweb", "test-crypto", cryptoManager.encryptData(data.utf8Binary))
  }

  actual fun getItem(scope: String, key: String): ByteArray? {
    val store = DeviceKeyValueStore(scope)
    return store.getRawItem(key.utf8Binary)
  }

  actual fun setItem(
    scope: String,
    key: String,
    value: ByteArray,
  ): Boolean {
    val store = DeviceKeyValueStore(scope)
    return runCatching { store.setRawItem(key.utf8Binary, value);true }.getOrDefault(false)
  }

  actual fun hasItem(scope: String, key: String): Boolean {
    val store = DeviceKeyValueStore(scope)
    return store.hasKey(key)
  }

  actual fun deleteItem(scope: String, key: String): Boolean {
    val store = DeviceKeyValueStore(scope)
    return store.removeKey(key)
  }

  actual fun supportEnumKeys(): Boolean {
    return true
  }

  actual fun keys(scope: String): List<String> {
    val store = DeviceKeyValueStore(scope)
    return store.getKeys()
  }
}