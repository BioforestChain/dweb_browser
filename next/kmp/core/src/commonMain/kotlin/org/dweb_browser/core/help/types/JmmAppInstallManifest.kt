package org.dweb_browser.core.help.types

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PropMetas
import org.dweb_browser.helper.PropMetasSerializer


object JmmAppInstallManifestSerializer :
  PropMetasSerializer<JmmAppInstallManifest>(JmmAppInstallManifest.P)

@Serializable(with = JmmAppInstallManifestSerializer::class)
class JmmAppInstallManifest private constructor(
  p: PropMetas.PropValues,
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
  fun toJmmAppManifest(): JmmAppManifest
}

