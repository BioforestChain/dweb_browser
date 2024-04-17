package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.dweb_browser.helper.OrderBy

/**
 * Ipc生命周期控制
 */
@Serializable
@SerialName(ENDPOINT_MESSAGE_TYPE_LIFECYCLE)
data class EndpointLifecycle(
  @Polymorphic val state: EndpointLifecycleState,
  override val order: Int = DEFAULT_ORDER,
) :
  EndpointMessage, OrderBy {
  companion object {
    const val DEFAULT_ORDER = -1
  }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Polymorphic
@JsonClassDiscriminator("state")
sealed interface EndpointLifecycleState

const val ENDPOINT_LIFECYCLE_STATE_INIT = "init"
const val ENDPOINT_LIFECYCLE_STATE_OPENING = "opening"
const val ENDPOINT_LIFECYCLE_STATE_OPENED = "opened"
const val ENDPOINT_LIFECYCLE_STATE_CLOSING = "closing"
const val ENDPOINT_LIFECYCLE_STATE_CLOSED = "closed"

enum class EndpointProtocol {
  JSON, CBOR,
//  Protobuf,
}

@Serializable
@SerialName(ENDPOINT_LIFECYCLE_STATE_INIT)
data object EndpointLifecycleInit : EndpointLifecycleState

@Serializable
@SerialName(ENDPOINT_LIFECYCLE_STATE_OPENING)
data class EndpointLifecycleOpening(val subProtocols: Set<EndpointProtocol> = setOf()) :
  EndpointLifecycleState

@Serializable
@SerialName(ENDPOINT_LIFECYCLE_STATE_OPENED)
data class EndpointLifecycleOpened(val subProtocols: Set<EndpointProtocol> = setOf()) :
  EndpointLifecycleState

@Serializable
@SerialName(ENDPOINT_LIFECYCLE_STATE_CLOSING)
data class EndpointLifecycleClosing(val reason: String? = null) : EndpointLifecycleState

@Serializable
@SerialName(ENDPOINT_LIFECYCLE_STATE_CLOSED)
data class EndpointLifecycleClosed(val reason: String? = null) : EndpointLifecycleState