package org.dweb_browser.sys.keychain.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.sys.keychain.KeychainNMM
import org.dweb_browser.sys.keychain.render.KeychainActivity
import org.dweb_browser.sys.keychain.render.KeychainAuthentication.Companion.ROOT_KEY_VERSION
import org.dweb_browser.sys.keychain.render.keychainMetadataStore
import org.dweb_browser.sys.toast.ext.showToast

abstract class EncryptKey {
  companion object {

    /**
     * 获取根密钥
     */
    suspend fun getRootKey(
      params: UseKeyParams,
    ): EncryptKey {
      val secretKeyRawBytes = KeychainActivity.create(params.runtime).start(
        runtime = params.runtime,
        title = params.reason.title,
        subtitle = params.reason.subtitle,
        description = params.reason.description,
      )
      val version = keychainMetadataStore.getItem(ROOT_KEY_VERSION)?.utf8String ?: run {
        keychainMetadataStore.setItem(ROOT_KEY_VERSION, RootKeyV1.VERSION.utf8Binary)
      }
      return when (version) {
        RootKeyV1.VERSION -> RootKeyV1(secretKeyRawBytes)
        else -> {
          WARNING("invalid $ROOT_KEY_VERSION: $version")
          params.runtime.showToast("您的钥匙串数据可能已经遭到损坏")
          RootKeyV1(secretKeyRawBytes)
        }
      }
    }
  }

  abstract suspend fun encryptData(
    params: UseKeyParams,
    sourceData: ByteArray,
  ): ByteArray

  abstract suspend fun decryptData(
    params: UseKeyParams,
    encryptedBytes: ByteArray,
  ): ByteArray
}

data class UseKeyParams(
  val runtime: KeychainNMM.KeyChainRuntime,
  val remoteMmid: String,
  val reason: UseKeyParams.UseKeyReason = UseKeyParams.UseKeyReason(),
) {
  data class UseKeyReason(
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
  )
}
typealias RecoveryKey = suspend (params: UseKeyParams) -> EncryptKey?
typealias GenerateKey = suspend (params: UseKeyParams) -> EncryptKey

@OptIn(ExperimentalSerializationApi::class)
val EncryptCbor = Cbor {
  serializersModule = SerializersModule {
    polymorphic(EncryptKey::class) {
      subclass(EncryptKeyV1::class)
    }
  }
}
