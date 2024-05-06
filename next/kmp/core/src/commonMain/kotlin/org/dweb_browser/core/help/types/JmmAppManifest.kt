package org.dweb_browser.core.help.types

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PropMetas
import org.dweb_browser.helper.PropMetasSerializer

object JmmAppManifestSerializer : PropMetasSerializer<JmmAppManifest>(JmmAppManifest.P)

@Serializable(with = JmmAppManifestSerializer::class)
class JmmAppManifest private constructor(
  p: PropMetas.PropValues,
  private val data: CommonAppManifest,
) : PropMetas.Constructor<JmmAppManifest>(p, P), IJmmAppManifest, ICommonAppManifest by data {
  constructor(p: PropMetas.PropValues = P.buildValues()) : this(p, CommonAppManifest(p))

  companion object {
    internal val P =
      PropMetas("JmmAppManifest") { JmmAppManifest(it) }.extends(CommonAppManifest.P)
    private val P_baseURI = P.optional<String>("baseURI")
    private val P_server = P.required("server", MainServer("/sys", "/server/plaoc.server.js"))

    /**
     * 要求的目标平台(JsMicroModule这个平台)的最小版本号
     * 未来还会加入对一些附属模块的最低版本号要求，但这里主要是针对平台这个不可变环境的要求
     */
    private val P_minTarget = P.required<Int>("minTarget", 1)
    private val P_maxTarget = P.optional<Int>("maxTarget")
  }

  override var baseURI by P_baseURI(p)
  override var server by P_server(p)
  override var minTarget by P_minTarget(p)
  override var maxTarget by P_maxTarget(p)

  override fun toCommonAppManifest() = data

  override fun canSupportTarget(
    version: Int,
    disMatchMinTarget: (minTarget: Int) -> String?,
    disMatchMaxTarget: (maxTarget: Int) -> String?,
  ): String? {
    if (minTarget > version) {
      return disMatchMinTarget(minTarget)
    }
    if (maxTarget != null && maxTarget!! < version) {
      return disMatchMaxTarget(maxTarget!!)
    }
    return null
  }
}


/** Js模块应用 元数据 */
internal interface IJmmAppManifest : ICommonAppManifest {
  var baseURI: String?
  var server: MainServer
  var minTarget: Int
  var maxTarget: Int?
  fun toCommonAppManifest(): CommonAppManifest
  fun canSupportTarget(
    version: Int,
    disMatchMinTarget: (minTarget: Int) -> String?,
    disMatchMaxTarget: (maxTarget: Int) -> String?
  ): String?

  fun canSupportTarget(version: Int) = canSupportTarget(version, { null }, { null }) == null
}
