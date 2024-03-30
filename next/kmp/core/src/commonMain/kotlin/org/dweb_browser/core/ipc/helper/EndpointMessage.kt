package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ByteEnumSerializer

/**
 * 总的消息类型抽象
 */
@Serializable
sealed class EndpointMessage(val type: ENDPOINT_MESSAGE_TYPE)
object ENDPOINT_MESSAGE_TYPE_Serializer :
  ByteEnumSerializer<ENDPOINT_MESSAGE_TYPE>(
    "ENDPOINT_MESSAGE_TYPE",
    ENDPOINT_MESSAGE_TYPE.ALL_VALUES,
    { type })

@Serializable(ENDPOINT_MESSAGE_TYPE_Serializer::class)
enum class ENDPOINT_MESSAGE_TYPE(val type: Byte) {
  LIFECYCLE(0),
  IPC(1),
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.type }
  }
}