package org.dweb_browser.sys.keychain

import org.dweb_browser.core.module.MicroModule

interface EncryptKey {
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
typealias GetOrRecoveryKey = suspend (MicroModule.Runtime) -> EncryptKey?
typealias GenerateKey = suspend (MicroModule.Runtime) -> EncryptKey
