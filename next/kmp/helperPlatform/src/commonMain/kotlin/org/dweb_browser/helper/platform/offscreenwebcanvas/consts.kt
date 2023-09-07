package org.dweb_browser.helper.platform.offscreenwebcanvas

import kotlinx.serialization.Serializable

@Serializable
internal data class RunCommandReq(
  val rid: Int, val resultVoid: Boolean, val resultJsonIfy: Boolean, val runCode: String
)

@Serializable
internal data class RunCommandResult(
  val rid: Int,
  val error: String? = null,
  val success: String? = null,
)
internal data class ChannelMessage(val data: String)