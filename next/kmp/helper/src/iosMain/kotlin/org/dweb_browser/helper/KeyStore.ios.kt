package org.dweb_browser.helper

import cnames.structs.__CFDictionary
import kotlinx.cinterop.CPointer
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
import platform.Security.errSecSuccess
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
public object KeyStore {
  private val LABEL_ID = NSBundle.mainBundle.bundleIdentifier!!

  @OptIn(ExperimentalForeignApi::class)
  public fun dictionary(): CPointer<__CFDictionary> = memScoped {
    val dictionary = CFDictionaryCreateMutable(
      kCFAllocatorDefault,
      1,
      kCFTypeDictionaryKeyCallBacks.ptr,
      kCFTypeDictionaryValueCallBacks.ptr,
    )!!
    CFDictionarySetValue(dictionary, kSecClass, kSecClassKey)

    dictionary
  }

  /**
   * 当前iOS硬件级别的加密主要由 Secure Enclave 来提供，而想要使用 kSecAttrTokenIDSecureEnclave 创建密钥，
   * 当前只支持 kSecAttrKeyTypeECSECPrimeRandom 椭圆曲线密钥，因此iOS改用椭圆曲线算法进行密钥生成，
   * 支持Face ID来获取密钥。之后的加解密数据采用kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM算法进行加解密；
   * */
  @OptIn(ExperimentalForeignApi::class)
  public fun generatePrivateKey(): Unit = memScoped {
    if (findPrivateKeyStatus().first == errSecSuccess) {
      return
    }
    val attributes = dictionary().autorelease(this)
    CFDictionarySetValue(attributes, kSecClass, kSecClassKey)
    val bitsRef = 256.bridgingRetain().autorelease(this)
    CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
    CFDictionarySetValue(attributes, kSecAttrKeySizeInBits, bitsRef)
    CFDictionarySetValue(attributes, kSecAttrTokenID, kSecAttrTokenIDSecureEnclave)

    val privateKeyAttributes = CFDictionaryCreateMutable(
      kCFAllocatorDefault, 1, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr
    )?.autorelease(this)

    val accessControl = SecAccessControlCreateWithFlags(
      kCFAllocatorDefault,
      kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
      kSecAccessControlPrivateKeyUsage or kSecAccessControlBiometryCurrentSet,
      null
    )?.autorelease(this)

    CFDictionarySetValue(
      privateKeyAttributes, kSecAttrAccessControl, accessControl
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
  public fun findPrivateKeyStatus(): Pair<Int, SecKeyRef?> = memScoped {
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

    return Pair(code, result.value as? SecKeyRef)
  }

  @OptIn(ExperimentalForeignApi::class)
  public fun getPrivateKey(): SecKeyRef = memScoped {
    val (code, privateKey) = findPrivateKeyStatus()
    return privateKey ?: throw Exception("Error fetching private key $code")
  }

  @OptIn(ExperimentalForeignApi::class)
  public fun encrypt(input: ByteArray): ByteArray = memScoped {
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
  public fun decrypt(encryptedData: ByteArray): ByteArray = memScoped {
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