package org.dweb_browser.helper

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytes
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRangeMake
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyRef
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmRSAEncryptionPKCS1

object RSA {
  @OptIn(ExperimentalForeignApi::class)
  fun generatePublicKeyAndPrivateKey(): Pair<SecKeyRef?, SecKeyRef?> = memScoped {
    val keySizeAllocation = alloc<IntVar>().apply {
      value = 2048
    }

    val attributes = CFDictionaryCreateMutable(
      kCFAllocatorDefault,
      1,
      kCFTypeDictionaryKeyCallBacks.ptr,
      kCFTypeDictionaryValueCallBacks.ptr
    ) ?: throw Exception("Failed to create mutable dictionary for key attributes.")
    CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeRSA)
    CFDictionaryAddValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPrivate)
    CFDictionaryAddValue(
      attributes,
      kSecAttrKeySizeInBits,
      CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, keySizeAllocation.ptr)
    )

    val error = alloc<CFErrorRefVar>()
    val privateKey = SecKeyCreateRandomKey(attributes, error.ptr)
      ?: throw Exception("Failed to create RSA private key.")
    val publicKey = SecKeyCopyPublicKey(privateKey)
      ?: throw Exception("Failed to derive public key from the given private key.")

    attributes.autorelease(this@memScoped)

    return Pair(publicKey, privateKey)
  }

  @OptIn(ExperimentalForeignApi::class)
  fun encryptData(publicKey: SecKeyRef, plainText: String) = memScoped {
    val data = plainText.encodeToByteArray()
    val error = alloc<CFErrorRefVar>()
    val encryptedData = SecKeyCreateEncryptedData(
      publicKey,
      kSecKeyAlgorithmRSAEncryptionPKCS1,
      data.bridgingRetain().autorelease(this@memScoped),
      error.ptr
    ) ?: throw Exception("SecKeyCreateEncryptedData returned null.")

    encryptedData.autorelease(this@memScoped)
    encryptedData.toByteArray()
  }

  @OptIn(ExperimentalForeignApi::class)
  fun decryptData(privateKey: SecKeyRef, encryptedData: ByteArray) = memScoped {
    privateKey.autorelease(this@memScoped)
    val error = alloc<CFErrorRefVar>()
    val decryptedData = SecKeyCreateDecryptedData(
      privateKey,
      kSecKeyAlgorithmRSAEncryptionPKCS1,
      encryptedData.bridgingRetain().autorelease(this@memScoped),
      error.ptr
    ) ?: throw Exception("SecKeyCreateDecryptedData returned null.")

    decryptedData.autorelease(this@memScoped)
    decryptedData.toByteArray().decodeToString()
  }

  @OptIn(ExperimentalUnsignedTypes::class, ExperimentalForeignApi::class)
  private fun ByteArray.bridgingRetain() = asUByteArray().let {
    CFDataCreate(null, it.refTo(0), it.size.convert()) as CFDataRef
  }

  @OptIn(ExperimentalForeignApi::class)
  fun <T : CPointer<U>, U> MemScope.autorelease(value: T): T {
    defer { CFRelease(value as CFTypeRef) }
    return value
  }

  @OptIn(ExperimentalForeignApi::class)
  fun <T : CPointer<U>, U> T.autorelease(scope: MemScope): T = scope.autorelease(this)

  @OptIn(ExperimentalUnsignedTypes::class, ExperimentalForeignApi::class)
  private fun CFDataRef.toByteArray() = ByteArray(CFDataGetLength(this).convert())
    .also { CFDataGetBytes(this, CFRangeMake(0, it.size.convert()), it.asUByteArray().refTo(0)) }
}
