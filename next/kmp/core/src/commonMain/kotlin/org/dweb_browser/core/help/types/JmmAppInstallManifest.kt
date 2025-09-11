package org.dweb_browser.core.help.types

import dev.whyoleg.cryptography.CryptographyAlgorithmId
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.Digest
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.algorithms.SHA384
import dev.whyoleg.cryptography.algorithms.SHA512
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.helper.PropMetas
import org.dweb_browser.helper.PropMetasSerializer
import org.dweb_browser.helper.base64Binary
import org.dweb_browser.helper.toWebUrl


object JmmAppInstallManifestSerializer :
  PropMetasSerializer<JmmAppInstallManifest>(JmmAppInstallManifest.P)

@Serializable(with = JmmAppInstallManifestSerializer::class)
class JmmAppInstallManifest private constructor(
  internal val p: PropMetas.PropValues,
  private val data: JmmAppManifest,
) : PropMetas.Constructor<JmmAppInstallManifest>(p, P), IJmmAppInstallManifest,
  IJmmAppManifest by data {

  constructor(p: PropMetas.PropValues = P.buildValues()) : this(p, JmmAppManifest(p))

  companion object {

    internal val P =
      PropMetas("JmmAppInstallManifest") { JmmAppInstallManifest(it) }.extends(JmmAppManifest.P)

    /** 安装是展示用的 icon */
    private val P_logo = P.required<String>("logo", "")

    /** 安装时展示用的截图 */
    private val P_images = P.list<String>("images")
    private val P_bundle_url = P.required<String>("bundle_url", "")
    private val P_bundle_hash = P.required<String>("bundle_hash", "")
    private val P_bundle_size = P.required<Long>("bundle_size", 0L)

    /**格式为 `hex:{signature}` */
    private val P_bundle_signature = P.required<String>("bundle_signature", "")
    private val P_signature = P.optional<String>("\$signature")

    /**该链接必须使用和app-id同域名的网站链接，
     * 请求回来是一个“算法+公钥地址”的格式 "{algorithm}:hex;{publicKey}"，
     * 比如说 `rsa-sha256:hex;2...1` */
    private val P_public_key_url = P.required<String>("public_key_url", "")

    /** 安装时展示的作者信息 */
    private val P_author = P.list<String>("author")

    /** 修改日志 */
    private val P_change_log = P.required<String>("change_log", "")

    /** 安装时展示的发布日期 */
    private val P_release_date = P.required<String>("release_date", "")

    /**
     * @deprecated 安装时显示的权限信息
     */
    private val P_permissions = P.list<String>("permissions")

    /**
     * @deprecated 安装时显示的依赖模块
     */
    private val P_plugins = P.list<String>("plugins")

    /**
     * 描述应用支持的语言，格式：http://www.lingoes.net/zh/translator/langcode.htm
     * en 英文，zh 中文
     */
    private val P_languages = P.list<String>("languages")
  }

  override var logo by P_logo(p);
  override var images by P_images(p);
  override var bundle_url by P_bundle_url(p);
  override var bundle_hash by P_bundle_hash(p);
  override var bundle_size by P_bundle_size(p);
  override var bundle_signature by P_bundle_signature(p);
  override var public_key_url by P_public_key_url(p);
  override var author by P_author(p);
  override var change_log by P_change_log(p);
  override var release_date by P_release_date(p);
  override var permissions by P_permissions(p);
  override var plugins by P_plugins(p);
  override var languages by P_languages(p);
  override var signature by P_signature(p);
  override fun toJmmAppManifest() = data
}

/** Js模块应用安装使用的元数据 */
internal interface IJmmAppInstallManifest : IJmmAppManifest {
  var logo: String
  var images: List<String>
  var bundle_url: String
  var bundle_hash: String
  var bundle_size: Long
  var bundle_signature: String
  var public_key_url: String
  var author: List<String>
  var change_log: String
  var release_date: String
  var permissions: List<String>
  var plugins: List<String>
  var languages: List<String>

  /**
   * 参考标准 https://developer.mozilla.org/zh-CN/docs/Web/Security/Subresource_Integrity
   * 符串包括一个前缀，表示一个特定的哈希算法（目前允许的前缀是 sha256、sha384 和 sha512），后面是一个短横线（-），最后是实际的 base64 编码的哈希。
   * 值可以包含多个由空格分隔的哈希值，只要文件匹配其中任意一个哈希值，就可以通过校验并加载该资源
   */
  var signature: String?
  fun toJmmAppManifest(): JmmAppManifest
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun JmmAppInstallManifest.verifySignature(): Boolean {
  if (signature.isNullOrBlank()) {
    return false
  }
  val signature: String = signature!!
  if (public_key_url.isBlank()) {
    return false
  }
  if (!id.endsWith(".dweb")) {
    return false
  }
  val origin = id.substringBeforeLast(".dweb")
  val publicKeyUrl =
    public_key_url.toWebUrl() ?: this.baseURI?.let { base -> (base + public_key_url).toWebUrl() }
    ?: return false

  if (publicKeyUrl.host != origin) return false

  try {

    val publicKeyRes = httpFetch(publicKeyUrl.toString())
    if (publicKeyRes.json<PublicKeyJsonBase>().version != 1) return false
    val v1 = publicKeyRes.json<PublicKeyV1Info>()
    /// 目前只支持 ECDSA 算法
    if (v1.algorithm != "ECDSA") return false

    val provider = CryptographyProvider.Default
    val ecdsa = provider.get(ECDSA)
    val publicKeyDecoder = ecdsa.publicKeyDecoder(
      when (v1.crv) {
        "P-256" -> EC.Curve.P256
        "P-384" -> EC.Curve.P384
        "P-P521" -> EC.Curve.P521
        else -> EC.Curve.P256
      }
    )

    val decodedPublicKey = publicKeyDecoder.decodeFromByteArray(
      when (v1.format) {
        "RAW" -> EC.PublicKey.Format.RAW
        "DER" -> EC.PublicKey.Format.DER
        "PEM" -> EC.PublicKey.Format.PEM
        "JWK" -> EC.PublicKey.Format.JWK
        else -> EC.PublicKey.Format.DER
      }, v1.publicKey.let {
        when {
          it.startsWith("base64-") -> it.substringAfter("base64-").base64Binary
          it.startsWith("hex-") -> it.substringAfter("hex-").base64Binary
          else -> it.base64Binary
        }
      })

    val signatureDigest: CryptographyAlgorithmId<Digest>
    val signatureBytes: ByteArray
    when {
      signature.startsWith("sha256-") -> {
        signatureDigest = SHA256
        signatureBytes = signature.substringAfter("sha256-").base64Binary
      }

      signature.startsWith("sha384-") -> {
        signatureDigest = SHA384
        signatureBytes = signature.substringAfter("sha384-").base64Binary
      }

      signature.startsWith("sha512-") -> {
        signatureDigest = SHA512
        signatureBytes = signature.substringAfter("sha384-").base64Binary
      }

      else -> return false
    }

    return decodedPublicKey.signatureVerifier(
      digest = signatureDigest,
      format = ECDSA.SignatureFormat.RAW
    ).tryVerifySignature(
      data = Json.encodeToString(this.p.toMap().sortedForJsonStringify()).encodeToByteArray(),
      signature = signatureBytes,
    )

//    val keyPairGenerator = ecdsa.keyPairGenerator(EC.Curve.P521)
//    val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKey()
//
//// generating signature using privateKey
//    val signature: ByteArray =
//      keyPair.privateKey.signatureGenerator(digest = SHA512)
//        .generateSignature("text1".encodeToByteArray())
//
//
//// verifying signature with publicKey, note, digest should be the same
//    val verificationResult: Boolean =
//      keyPair.publicKey.signatureVerifier(digest = SHA512)
//        .verifySignature("text1".encodeToByteArray(), signature)
//
//// will print true
//    println(verificationResult)
//
//// key also can be encoded and decoded
//
//    val encodedPublicKey: ByteArray = keyPair.publicKey.encodeTo(EC.PublicKey.Format.DER)
//// note, the curve should be the same
//    val decodedPublicKey: ECDSA.PublicKey =
//      ecdsa.publicKeyDecoder(EC.Curve.P521).decodeFrom(EC.PublicKey.Format.DER, encodedPublicKey)
//
//    val decodedKeyVerificationResult: Boolean =
//      decodedPublicKey.signatureVerifier(digest = SHA512)
//        .verifySignature("text1".encodeToByteArray(), signature)
//
//// will print true
//    println(decodedKeyVerificationResult)

    return true

  } catch (e: Throwable) {
    return false
  }
}

@Serializable
data class PublicKeyJsonBase(val version: Int)

@Serializable
data class PublicKeyV1Info(
  val algorithm: String,
  val format: String,

  val publicKey: String,
  val crv: String?,
)


private fun Map<*, *>.sortedForJsonStringify(): List<Any?> {
  val keys = this.keys.mapNotNull { it as? String }.filter { !it.startsWith("$") }.sorted()
  val result = mutableListOf<Any?>()
  for (key in keys) {
    val value = this[key]
    result.add(key)
    if (value is Map<*, *>) {
      result.add(value.sortedForJsonStringify())
    } else {
      result.add(value)
    }
  }
  return result
}