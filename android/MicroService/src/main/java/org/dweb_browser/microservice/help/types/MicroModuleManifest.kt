package org.dweb_browser.microservice.help.types

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PropMetas
import org.dweb_browser.helper.PropMetasSerializer

object MicroModuleManifestSerializer :
  PropMetasSerializer<MicroModuleManifest>(MicroModuleManifest.P)

@Serializable(with = MicroModuleManifestSerializer::class)
class MicroModuleManifest private constructor(
  p: PropMetas.PropValues,
  private val data: CommonAppManifest,
) : PropMetas.Constructor<MicroModuleManifest>(p, P), IMicroModuleManifest,
  ICommonAppManifest by data {
  constructor(p: PropMetas.PropValues = P.buildValues()) : this(p, CommonAppManifest(p))

  companion object {
    val P =
      PropMetas("MicroModuleManifest", { MicroModuleManifest(it) }).extends(CommonAppManifest.P)
    private val P_mmid = P.required<MMID>("mmid", "")
    private val P_ipc_support_protocols = P.required<IpcSupportProtocols>(
      "ipc_support_protocols", IpcSupportProtocols(
        cbor = true, protobuf = true, raw = true
      )
    )
  }


  override var mmid by P_mmid(p)
  override var ipc_support_protocols by P_ipc_support_protocols(p)
  override var id: MMID
    get() = mmid
    set(value) {
      mmid = value
    }

  override fun toCommonAppManifest() = data
}

interface IMicroModuleManifest : ICommonAppManifest {
  var mmid: MMID
  var ipc_support_protocols: IpcSupportProtocols
  fun toCommonAppManifest(): CommonAppManifest
}