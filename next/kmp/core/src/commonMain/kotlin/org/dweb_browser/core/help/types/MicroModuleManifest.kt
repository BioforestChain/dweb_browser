package org.dweb_browser.core.help.types

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
        cbor = true, protobuf = false, json = true
      )
    )

    private val P_targetType = P.optional<String>("targetType")
    private val P_minTarget = P.optional<Int>("minTarget")
    private val P_maxTarget = P.optional<Int>("maxTarget")
  }


  override var mmid by P_mmid(p) {
    p.set("id", value)
    afterWrite = {
      p.set("id", it)
    }
  }
  override var ipc_support_protocols by P_ipc_support_protocols(p)
  override var targetType by P_targetType(p)
  override var minTarget by P_minTarget(p)
  override var maxTarget by P_maxTarget(p)

  override var id by P.getRequired<String>("id")(p) {
    p.set("mmid", value)
    afterWrite = {
      p.set("mmid", it)
    }
  }

  override fun toCommonAppManifest() = data
}

interface IMicroModuleManifest : ICommonAppManifest {
  var mmid: MMID
  var ipc_support_protocols: IpcSupportProtocols
  fun toCommonAppManifest(): CommonAppManifest

  fun getMmptList() = listOf(mmid, *dweb_protocols.toTypedArray())
  var targetType: String?
  var minTarget: Int?
  var maxTarget: Int?
}