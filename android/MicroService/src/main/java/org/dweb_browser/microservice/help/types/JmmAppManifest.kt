package org.dweb_browser.microservice.help.types

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
      PropMetas("JmmAppManifest", { JmmAppManifest(it) }).extends(CommonAppManifest.P)
    private val P_baseURI = P.optional<String>("baseURI")
    private val P_server = P.required("server", MainServer("/sys", "/server/plaoc.server.js"))
  }

  override var baseURI by P_baseURI(p)
  override var server by P_server(p)

  override fun toCommonAppManifest() = data
}


/** Js模块应用 元数据 */
internal interface IJmmAppManifest : ICommonAppManifest {
  var baseURI: String?
  var server: MainServer
  fun toCommonAppManifest(): CommonAppManifest
}
