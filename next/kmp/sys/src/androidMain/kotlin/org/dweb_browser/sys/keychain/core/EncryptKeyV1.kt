package org.dweb_browser.sys.keychain.core

import android.security.keystore.KeyProperties
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.helper.base64UrlString
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.sys.keychain.debugKeychain
import org.dweb_browser.sys.keychain.render.keychainMetadataStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Serializable
@SerialName(EncryptKeyV1.VERSION)
class EncryptKeyV1(
  val encryptEncoded: ByteArray,
  var encoded: ByteArray? = null,
  /**
   * 一个访问名单表
   */
  val expiredTimeMap: MutableMap<String, Long> = mutableMapOf(),
  /**
   * 一个用户自定义的过期时间表
   * TODO 实现在界面上提供给用户 “10min内始终给予授权” 的选项
   */
  val userAllowKeepDurationMap: MutableMap<String, Long> = mutableMapOf(),
) : AesEncryptKey(EncryptKeyV1.TRANSFORMATION) {
  companion object {
    const val VERSION = "v1"
    private const val alias = "keychainstore-key-v1"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_HMAC_SHA256// KEY_ALGORITHM_AES
    private const val BLOCK_MODES = KeyProperties.BLOCK_MODE_CBC  // 目前支持的模式里，算是最安全的；它的缺点是不能并发处理
    private const val ENCRYPTION_PADDINGS = KeyProperties.ENCRYPTION_PADDING_PKCS7

    // private const val ENCRYPTION_PADDINGS = KeyProperties.ENCRYPTION_PADDING_NONE
    // private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val TRANSFORMATION = "AES/$BLOCK_MODES/$ENCRYPTION_PADDINGS"

    @OptIn(ExperimentalSerializationApi::class)
    internal val recoveryKey: RecoveryKey = { params ->
      runCatching {
        keychainMetadataStore.getItem(alias)?.let {
          EncryptCbor.decodeFromByteArray<EncryptKeyV1>(it)
        }
      }.getOrElse {
        debugKeychain("EncryptKeyV1.recoveryKey", "fail to recoveryKey", it)
        null
      }
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal val generateKey: GenerateKey = { params ->
      // 生成一个 数据密钥
      val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
      val secretKey = keyGenerator.generateKey()
      // 向用户获取 根密钥
      val rootKey = EncryptKey.getRootKey(params)
      // 使用根密钥来加密随机密钥
      val encryptKey = EncryptKeyV1(
        rootKey.encryptData(
          params.copy(
            reason = UseKeyParams.UseKeyReason(title = "生成新的密钥，需要用户的授权"),
          ), secretKey.encoded
        )
      )

      // 使用根密钥来加密随机密钥
      keychainMetadataStore.setItem(
        alias, EncryptCbor.encodeToByteArray<EncryptKeyV1>(encryptKey)
      )
      encryptKey
    }
  }

  override suspend fun readCryptKey(params: UseKeyParams): SecretKey {
    // 如果缓存还没过期，那么可以继续使用，否则向用户获取数据
    val key = encoded?.let {
      if (expiredTimeMap.getOrDefault(params.remoteMmid, 0) > datetimeNow()) it else null
    } ?: run {
      // 向用户获取 根密钥
      val rootKey = EncryptKey.getRootKey(params)
      // 使用根密钥，解密获得 数据密钥，
      rootKey.decryptData(params, encryptEncoded).also {
        encoded = it
        userAllowKeepDurationMap[params.remoteMmid]?.also { duration ->
          /// 在用户授权的时间内，这个密钥的使用不用再询问用户
          expiredTimeMap[params.remoteMmid] = datetimeNow() + duration
        }
      }
    }
    println("QAQ readCryptKey=${key.base64UrlString}")

    return SecretKeySpec(key, ALGORITHM)
  }

}