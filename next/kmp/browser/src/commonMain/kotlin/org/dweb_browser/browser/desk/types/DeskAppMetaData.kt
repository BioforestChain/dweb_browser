package org.dweb_browser.browser.desk.types

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.helper.PropMetas
import org.dweb_browser.helper.PropMetasSerializer
import org.dweb_browser.sys.window.core.WindowState


//object DeskAppMetaDataSerializer:KSerializer<DeskAppMetaData>{
//  override val descriptor = buildClassSerialDescriptor("DeskAppMetaData"){
//
//  }
//  override fun deserialize(decoder: Decoder): DeskAppMetaData {
//
//  }
//  override fun serialize(encoder: Encoder, value: DeskAppMetaData) {
//
//  }
//}

object DeskAppMetaDataSerializer : PropMetasSerializer<DeskAppMetaData>(DeskAppMetaData.P)

@Serializable(DeskAppMetaDataSerializer::class)
class DeskAppMetaData private constructor(
  p: PropMetas.PropValues,
  private val data: MicroModuleManifest,
) : PropMetas.Constructor<DeskAppMetaData>(p, P), IDeskAppMetaData, IMicroModuleManifest by data {
  constructor(p: PropMetas.PropValues = P.buildValues()) : this(p = p, MicroModuleManifest(p))

  companion object {
    val P = PropMetas("DeskAppMetaData") { DeskAppMetaData(it) }.extends(MicroModuleManifest.P)
    val P_running = P.required<Boolean>("running", false) // 是否正在运行
    val P_winStates = P.list<WindowState>("winStates") // 当前进程所拥有的窗口的状态
  }

  override var running by P_running(p)
  override var winStates by P_winStates(p)
}

interface IDeskAppMetaData : IMicroModuleManifest {
  var running: Boolean
  var winStates: List<WindowState>
}

