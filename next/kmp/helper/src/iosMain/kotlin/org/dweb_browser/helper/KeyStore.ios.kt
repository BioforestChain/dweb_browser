package org.dweb_browser.helper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSBundle
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemCopyMatching
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyRef
import platform.Security.kSecAccessControlBiometryCurrentSet
import platform.Security.kSecAccessControlPrivateKeyUsage
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrTokenID
import platform.Security.kSecAttrTokenIDSecureEnclave
import platform.Security.kSecClass
import platform.Security.kSecClassKey
import platform.Security.kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecReturnRef

@OptIn(ExperimentalForeignApi::class)
object KeyStore {
  private val LABEL_ID = NSBundle.mainBundle.bundleIdentifier!!

  init {
    generatePrivateKey()
  }

  @OptIn(ExperimentalForeignApi::class)
  fun dictionary() = memScoped {
    val dictionary = CFDictionaryCreateMutable(
      kCFAllocatorDefault,
      1,
      kCFTypeDictionaryKeyCallBacks.ptr,
      kCFTypeDictionaryValueCallBacks.ptr,
    )!!
    CFDictionarySetValue(dictionary, kSecClass, kSecClassKey)

    dictionary
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun generatePrivateKey() = memScoped {
    val attributes = dictionary().autorelease(this)
    CFDictionarySetValue(attributes, kSecClass, kSecClassKey)
    val bitsRef = 256.bridgingRetain().autorelease(this)
    CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
    CFDictionarySetValue(attributes, kSecAttrKeySizeInBits, bitsRef)
    CFDictionarySetValue(attributes, kSecAttrTokenID, kSecAttrTokenIDSecureEnclave)

    val privateKeyAttributes = CFDictionaryCreateMutable(
      kCFAllocatorDefault,
      1,
      kCFTypeDictionaryKeyCallBacks.ptr,
      kCFTypeDictionaryValueCallBacks.ptr
    )?.autorelease(this)

    val accessControl = SecAccessControlCreateWithFlags(
      kCFAllocatorDefault,
      kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
      kSecAccessControlPrivateKeyUsage or kSecAccessControlBiometryCurrentSet,
      null
    )?.autorelease(this)

    CFDictionarySetValue(
      privateKeyAttributes,
      kSecAttrAccessControl,
      accessControl
    )
    CFDictionarySetValue(privateKeyAttributes, kSecAttrIsPermanent, kCFBooleanTrue)
    val labelRef = LABEL_ID.encodeToByteArray().bridgingRetain().autorelease(this)
    CFDictionarySetValue(privateKeyAttributes, kSecAttrApplicationTag, labelRef)
    CFDictionarySetValue(attributes, kSecPrivateKeyAttrs, privateKeyAttributes)

    val error = alloc<CFErrorRefVar>()
    val privateKey = SecKeyCreateRandomKey(attributes, error.ptr)

    privateKey?.autorelease(this)
  }

  @OptIn(ExperimentalForeignApi::class)
  @Suppress("UNCHECKED_CAST")
  fun getPrivateKey(): SecKeyRef = memScoped {
    val attributes = dictionary()
    CFDictionarySetValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPrivate)
    CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
    val bitsRef = 256.bridgingRetain().autorelease(this)
    CFDictionarySetValue(attributes, kSecAttrKeySizeInBits, bitsRef)
    val labelRef = LABEL_ID.encodeToByteArray().bridgingRetain().autorelease(this)
    CFDictionarySetValue(attributes, kSecAttrApplicationTag, labelRef)
    CFDictionarySetValue(attributes, kSecReturnRef, kCFBooleanTrue)

    val result = alloc<CFTypeRefVar>()
    val code = SecItemCopyMatching(attributes, result.ptr)

    return result.value as SecKeyRef? ?: throw Exception("Error fetching private key $code")
  }

  @OptIn(ExperimentalForeignApi::class)
  fun encrypt(input: ByteArray): ByteArray = memScoped {
    val privateKey = getPrivateKey()
    val publicKey = SecKeyCopyPublicKey(privateKey)
    val error = alloc<CFErrorRefVar>()
    val encryptedData = SecKeyCreateEncryptedData(
      publicKey,
      kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM,
      input.bridgingRetain().autorelease(this),
      error.ptr
    )!!
    privateKey.autorelease(this)
    encryptedData.autorelease(this)
    encryptedData.toByteArray()
  }

  @OptIn(ExperimentalForeignApi::class)
  fun decrypt(encryptedData: ByteArray): ByteArray = memScoped {
    val privateKey = getPrivateKey()
    val error = alloc<CFErrorRefVar>()
    val decryptedData = SecKeyCreateDecryptedData(
      privateKey,
      kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM,
      encryptedData.bridgingRetain().autorelease(this),
      error.ptr
    )!!
    privateKey.autorelease(this)
    decryptedData.autorelease(this)
    decryptedData.toByteArray()
  }
}