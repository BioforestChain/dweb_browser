package org.dweb_browser.microservice.help.types

import kotlinx.serialization.Serializable

@Serializable
data class IpcSupportProtocols(
  val cbor: Boolean,
  val protobuf: Boolean,
  val raw: Boolean,
)