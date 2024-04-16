package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ByteEnumSerializer

/**
 * 总的消息类型抽象
 */
@Serializable
sealed interface EndpointMessage {}

const val ENDPOINT_MESSAGE_TYPE_LIFECYCLE = "life"
const val ENDPOINT_MESSAGE_TYPE_IPC = "ipc"