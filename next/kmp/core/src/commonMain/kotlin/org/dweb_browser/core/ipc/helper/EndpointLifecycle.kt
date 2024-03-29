package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**
 * Ipc生命周期控制
 */
@Serializable
sealed class EndpointLifecycle(
  val state: ENDPOINT_STATE,
) {
  class Opening(
    val encoding: List<IPC_DATA_ENCODING> = listOf()
  ) : EndpointLifecycle(ENDPOINT_STATE.OPENING)

  class Opened() : EndpointLifecycle(ENDPOINT_STATE.OPENED)
  class Closing() : EndpointLifecycle(ENDPOINT_STATE.CLOSING)
  class Closed() : EndpointLifecycle(ENDPOINT_STATE.CLOSED)
}



enum class EndpointProtocol {
  Json, Cbor,
//  Protobuf,
}