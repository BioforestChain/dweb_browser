package org.dweb_browser.pure.http

import io.ktor.client.engine.darwin.KtorNSURLSessionDelegate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.platform.NSDataHelper.toByteArray
import platform.CoreFoundation.CFDictionaryGetValue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.create
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyKey
import platform.Security.SecCertificateRef
import platform.Security.SecKeyCopyAttributes
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecTrustGetCertificateAtIndex
import platform.Security.SecTrustGetCertificateCount
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrKeyTypeRSA
import platform.darwin.NSObject


private val rsa1024Asn1Header: IntArray = intArrayOf(
  0x30, 0x81, 0x9F, 0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86, 0xF7,
  0x0D, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x81, 0x8D, 0x00
)

private val rsa2048Asn1Header: IntArray = intArrayOf(
  0x30, 0x82, 0x01, 0x22, 0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86,
  0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0F, 0x00
)

private val rsa3072Asn1Header: IntArray = intArrayOf(
  0x30, 0x82, 0x01, 0xA2, 0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86,
  0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x8F, 0x00
)

private val rsa4096Asn1Header: IntArray = intArrayOf(
  0x30, 0x82, 0x02, 0x22, 0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86,
  0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x02, 0x0F, 0x00
)

private val ecdsaSecp256r1Asn1Header: IntArray = intArrayOf(
  0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02,
  0x01, 0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03,
  0x42, 0x00
)

private val ecdsaSecp384r1Asn1Header: IntArray = intArrayOf(
  0x30, 0x76, 0x30, 0x10, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02,
  0x01, 0x06, 0x05, 0x2b, 0x81, 0x04, 0x00, 0x22, 0x03, 0x62, 0x00
)

/**
 * Certificate headers
 *
 * Sources for values:
 * https://github.com/datatheorem/TrustKit/blob/master/TrustKit/Pinning/TSKSPKIHashCache.m
 * https://github.com/IBM-Swift/BlueRSA/blob/master/Sources/CryptorRSA/CryptorRSAUtilities.swift
 */
internal object CertificatesInfo {
  val rsa = mapOf(
    1024 to rsa1024Asn1Header,
    2048 to rsa2048Asn1Header,
    3072 to rsa3072Asn1Header,
    4096 to rsa4096Asn1Header
  )

  val ecdsa = mapOf(
    256 to ecdsaSecp256r1Asn1Header,
    384 to ecdsaSecp384r1Asn1Header
  )

  internal const val HASH_ALGORITHM_SHA_256 = "sha256/"
  internal const val HASH_ALGORITHM_SHA_1 = "sha1/"
}

@OptIn(ExperimentalForeignApi::class)
internal object NSSessionUtils {

  /**
   * Gets the public key from the SecCertificate
   */

  fun getPublicKeyBytes(certificateRef: SecCertificateRef): ByteArray? {
    val publicKeyRef = SecCertificateCopyKey(certificateRef) ?: return null

    return publicKeyRef.use {
      val publicKeyAttributes = SecKeyCopyAttributes(publicKeyRef)
      val publicKeyTypePointer = CFDictionaryGetValue(publicKeyAttributes, kSecAttrKeyType)
      val publicKeyType = CFBridgingRelease(publicKeyTypePointer) as NSString
      val publicKeySizePointer = CFDictionaryGetValue(publicKeyAttributes, kSecAttrKeySizeInBits)
      val publicKeySize = CFBridgingRelease(publicKeySizePointer) as NSNumber

      CFBridgingRelease(publicKeyAttributes)

      if (!checkValidKeyType(publicKeyType, publicKeySize)) {
        println("CertificatePinner: Public Key not supported type or size")
        return null
      }

      val publicKeyDataRef = SecKeyCopyExternalRepresentation(publicKeyRef, null)
      val publicKeyData = CFBridgingRelease(publicKeyDataRef) as NSData
      val publicKeyBytes = publicKeyData.toByteArray()
      val headerInts = getAsn1HeaderBytes(publicKeyType, publicKeySize)

      val header = headerInts.foldIndexed(ByteArray(headerInts.size)) { i, a, v ->
        a[i] = v.toByte()
        a
      }
      header + publicKeyBytes
    }
  }

  internal inline fun <T : CPointed, R> CPointer<T>.use(block: (CPointer<T>) -> R): R {
    try {
      return block(this)
    } finally {
      CFBridgingRelease(this)
    }
  }

  /**
   * Checks that we support the key type and size
   */

  private fun checkValidKeyType(publicKeyType: NSString, publicKeySize: NSNumber): Boolean {
    val keyTypeRSA = CFBridgingRelease(kSecAttrKeyTypeRSA) as NSString
    val keyTypeECSECPrimeRandom = CFBridgingRelease(kSecAttrKeyTypeECSECPrimeRandom) as NSString

    val size: Int = publicKeySize.intValue.toInt()
    val keys = when (publicKeyType) {
      keyTypeRSA -> CertificatesInfo.rsa
      keyTypeECSECPrimeRandom -> CertificatesInfo.ecdsa
      else -> return false
    }

    return keys.containsKey(size)
  }

  /**
   * Get the [IntArray] of Asn1 headers needed to prepend to the public key to create the
   * encoding [ASN1Header](https://docs.oracle.com/middleware/11119/opss/SCRPJ/oracle/security/crypto/asn1/ASN1Header.html)
   */

  private fun getAsn1HeaderBytes(publicKeyType: NSString, publicKeySize: NSNumber): IntArray {
    val keyTypeRSA = CFBridgingRelease(kSecAttrKeyTypeRSA) as NSString
    val keyTypeECSECPrimeRandom = CFBridgingRelease(kSecAttrKeyTypeECSECPrimeRandom) as NSString

    val size: Int = publicKeySize.intValue.toInt()
    val keys = when (publicKeyType) {
      keyTypeRSA -> CertificatesInfo.rsa
      keyTypeECSECPrimeRandom -> CertificatesInfo.ecdsa
      else -> return intArrayOf()
    }

    return keys[size] ?: intArrayOf()
  }


  @OptIn(BetaInteropApi::class)
  fun didReceiveChallenge(
    sslConfig: HttpPureClientConfig.DwebSslConfig,
    session: NSURLSession,
    challenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
  ) {
    val trust = challenge.protectionSpace.serverTrust
    if (trust != null
      //
      && challenge.protectionSpace.host.endsWith(".dweb")
      //
      && challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust
      //
      && SecTrustGetCertificateCount(trust) != 0L
    ) {
      val certHex =
        SecTrustGetCertificateAtIndex(trust, 0)?.let { NSSessionUtils.getPublicKeyBytes(it) }
      if (certHex != null && certHex.contentEquals(sslConfig.publicKey)) {
        NSURLSessionAuthChallengeCancelAuthenticationChallenge
        completionHandler(
          NSURLSessionAuthChallengeUseCredential,
          NSURLCredential.create(trust = challenge.protectionSpace.serverTrust)
        )
        return
      }
    }
    completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
  }
}


@OptIn(ExperimentalForeignApi::class)
internal class KtorNSURLSessionDelegateWrapper(
  private val ktorDelegate: KtorNSURLSessionDelegate,
  private val sslConfig: HttpPureClientConfig.DwebSslConfig,
) : NSObject(), NSURLSessionDataDelegateProtocol, NSURLSessionWebSocketDelegateProtocol {
  override fun URLSession(
    session: NSURLSession,
    dataTask: NSURLSessionDataTask,
    didReceiveData: NSData,
  ) {
    ktorDelegate.URLSession(session = session, dataTask = dataTask, didReceiveData = didReceiveData)
  }

  override fun URLSession(
    session: NSURLSession,
    task: NSURLSessionTask,
    didCompleteWithError: NSError?,
  ) {
    ktorDelegate.URLSession(
      session = session, task = task, didCompleteWithError = didCompleteWithError
    )
  }

  override fun URLSession(
    session: NSURLSession,
    webSocketTask: NSURLSessionWebSocketTask,
    didOpenWithProtocol: String?,
  ) {
    ktorDelegate.URLSession(
      session = session, webSocketTask = webSocketTask, didOpenWithProtocol = didOpenWithProtocol
    )
  }

  override fun URLSession(
    session: NSURLSession,
    webSocketTask: NSURLSessionWebSocketTask,
    didCloseWithCode: NSURLSessionWebSocketCloseCode,
    reason: NSData?,
  ) {
    ktorDelegate.URLSession(
      session = session,
      webSocketTask = webSocketTask,
      didCloseWithCode = didCloseWithCode,
      reason = reason,
    )
  }

  override fun URLSession(
    session: NSURLSession,
    didReceiveChallenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
  ) {
    NSSessionUtils.didReceiveChallenge(sslConfig, session, didReceiveChallenge, completionHandler)
  }
}
