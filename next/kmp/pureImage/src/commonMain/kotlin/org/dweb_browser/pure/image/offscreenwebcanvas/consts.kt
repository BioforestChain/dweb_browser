package org.dweb_browser.pure.image.offscreenwebcanvas

import kotlinx.serialization.Serializable

@Serializable
internal data class RunCommandReq(
  val rid: Int, val returnType: ReturnType, val runCode: String,
)

@Serializable
enum class ReturnType {
  void, json, string, binary;
}

internal class ChannelMessage(val text: String? = null, val binary: ByteArray? = null)