package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.OrderBy

/**
 * Ipc生命周期控制
 */
@Serializable
sealed class EndpointLifecycle(val state: LIFECYCLE_STATE, override val order: Int = 0) :
  EndpointMessage(ENDPOINT_MESSAGE_TYPE.LIFECYCLE), OrderBy {
  // TODO 测试能否 equals？
  @Serializable
  class Init() : EndpointLifecycle(LIFECYCLE_STATE.INIT)

  @Serializable
  data class Opening(val subProtocols: Set<EndpointProtocol> = setOf()) :
    EndpointLifecycle(LIFECYCLE_STATE.OPENING)

  @Serializable
  data class Opened(val subProtocols: Set<EndpointProtocol> = setOf()) :
    EndpointLifecycle(LIFECYCLE_STATE.OPENED)

  @Serializable
  data class Closing(val reason: String? = null) : EndpointLifecycle(LIFECYCLE_STATE.CLOSING)

  @Serializable
  data class Closed(val reason: String? = null) : EndpointLifecycle(LIFECYCLE_STATE.CLOSED)
}


enum class EndpointProtocol {
  Json, Cbor,
//  Protobuf,
}