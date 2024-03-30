package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**
 * Ipc生命周期控制
 */
@Serializable
sealed class EndpointLifecycle(
  val state: ENDPOINT_STATE,
) : EndpointMessage(ENDPOINT_MESSAGE_TYPE.LIFECYCLE) {
  class Init() : EndpointLifecycle(ENDPOINT_STATE.INIT)
  data class Opening(val subProtocols: Set<EndpointProtocol> = setOf()) :
    EndpointLifecycle(ENDPOINT_STATE.OPENING)

  data class Opened(val subProtocols: Set<EndpointProtocol> = setOf()) :
    EndpointLifecycle(ENDPOINT_STATE.OPENED)

  data class Closing(val reason: String? = null) : EndpointLifecycle(ENDPOINT_STATE.CLOSING)
  data class Closed(val reason: String? = null) : EndpointLifecycle(ENDPOINT_STATE.CLOSED)

  override fun hashCode(): Int {
    return state.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is EndpointLifecycle) return false

    if (state != other.state) return false

    return true
  }

}


enum class EndpointProtocol {
  Json, Cbor,
//  Protobuf,
}