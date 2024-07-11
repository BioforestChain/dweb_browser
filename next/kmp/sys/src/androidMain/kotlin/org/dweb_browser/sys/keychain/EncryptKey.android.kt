package org.dweb_browser.sys.keychain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.dweb_browser.core.module.MicroModule

sealed interface EncryptKey {
  suspend fun encryptData(
    runtime: MicroModule.Runtime,
    remoteMmid: String,
    sourceData: ByteArray,
  ): ByteArray

  suspend fun decryptData(
    runtime: MicroModule.Runtime,
    remoteMmid: String,
    encryptedBytes: ByteArray,
  ): ByteArray
}
typealias RecoveryKey = suspend (MicroModule.Runtime) -> EncryptKey?
typealias GenerateKey = suspend (MicroModule.Runtime) -> EncryptKey

@OptIn(ExperimentalSerializationApi::class)
val EncryptCbor = Cbor {
  serializersModule = SerializersModule {
    polymorphic(EncryptKey::class) {
      subclass(EncryptKeyV1::class)
    }
  }
}
