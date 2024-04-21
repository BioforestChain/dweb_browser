package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**
 * 总的消息类型抽象
 */
@Serializable
sealed interface EndpointRawMessage {}

sealed interface EndpointMessage {}

const val ENDPOINT_MESSAGE_TYPE_LIFECYCLE = "life"
const val ENDPOINT_MESSAGE_TYPE_IPC = "ipc"