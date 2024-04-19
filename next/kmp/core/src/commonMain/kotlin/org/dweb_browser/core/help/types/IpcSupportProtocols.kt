package org.dweb_browser.core.help.types

import kotlinx.serialization.Serializable

@Serializable
data class IpcSupportProtocols(
  val cbor: Boolean,
  val protobuf: Boolean,
  val json: Boolean,
)