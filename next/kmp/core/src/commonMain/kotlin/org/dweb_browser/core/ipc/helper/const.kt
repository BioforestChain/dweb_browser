package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.ByteEnumSerializer
import org.dweb_browser.helper.IntEnumSerializer
import org.dweb_browser.helper.SignalCallback
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.toBase64ByteArray


const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024

/**IpcPool*/
data class IpcPoolMessageArgs(val message: EndpointIpcMessage, val ipc: Ipc)
typealias OnIpcPoolMessage = SignalCallback<IpcPoolMessageArgs>

@Serializable(IPC_MESSAGE_TYPE_Serializer::class)
enum class IPC_MESSAGE_TYPE(val type: Byte) {
  LIFECYCLE(0),

  /** 类型：请求 */
  REQUEST(1),

  /** 类型：相应 */
  RESPONSE(2),

  /** 类型：流数据，发送方 */
  STREAM_DATA(3),

  /** 类型：流拉取，请求方 */
  STREAM_PULL(4),

  /** 类型：推送流，发送方
   * 对方可能没有发送PULL过来，或者发送了被去重了，所以我们需要主动发送PUSH指令，对方收到后，如果状态允许，则会发送PULL指令过来拉取数据
   */
  STREAM_PAUSED(5),

  /** 类型：流关闭，发送方
   * 可能是发送完成了，也有可能是被中断了
   */
  STREAM_END(6),

  /** 类型：流中断，请求方 */
  STREAM_ABORT(7),

  /** 类型：事件 */
  EVENT(8),

  /**类型：错误*/
  ERROR(9),
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.type }
  }
}

object IPC_MESSAGE_TYPE_Serializer :
  ByteEnumSerializer<IPC_MESSAGE_TYPE>("IPC_MESSAGE_TYPE", IPC_MESSAGE_TYPE.ALL_VALUES, { type })


object ENDPOINT_STATE_Serializer :
  IntEnumSerializer<LIFECYCLE_STATE>("ENDPOINT_STATE", LIFECYCLE_STATE.ALL_VALUES, { state })

@Serializable(ENDPOINT_STATE_Serializer::class)
enum class LIFECYCLE_STATE(val state: Int) {
  INIT(0),
  OPENING(1),
  OPENED(2),
  CLOSING(3),
  CLOSED(4), ;

  companion object {
    val ALL_VALUES = LIFECYCLE_STATE.entries.associateBy { it.state }
  }
}

/**
 * 可预读取的流
 */
interface PreReadableInputStream {
  /**
   * 对标 InputStream.available 函数
   * 返回可预读的数据
   */
  val preReadableSize: Int
}


object IPC_DATA_ENCODING_Serializer : IntEnumSerializer<IPC_DATA_ENCODING>(
  "IPC_DATA_ENCODING",
  IPC_DATA_ENCODING.ALL_VALUES,
  { encoding })

@Serializable(IPC_DATA_ENCODING_Serializer::class)
enum class IPC_DATA_ENCODING(val encoding: Int) {
  /** UTF8编码的字符串，本质上是 BINARY */
  UTF8(1 shl 1),

  /** BASE64编码的字符串，本质上是 BINARY */
  BASE64(1 shl 2),

  /** 二进制, 与 UTF8/BASE64 是对等关系*/
  BINARY(1 shl 3),

//    /** BINARY-CBOR 编码的二进制，本质上是 OBJECT */
//    CBOR(1 shl 4),
//
//    /** UTF8-JSON 编码的字符串，本质上是 OBJECT */
//    JSON(1 shl 5),
//
//    /** 结构化对象，与 JSON、Cbor 是对等关系 */
//    OBJECT(1 shl 6),
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.encoding }
  }
}

object IPC_ROLE_Serializer :
  StringEnumSerializer<IPC_ROLE>("IPC_ROLE", IPC_ROLE.ALL_VALUES, { role })

@Serializable(IPC_ROLE_Serializer::class)
enum class IPC_ROLE(val role: String) {
  SERVER("server"), CLIENT("client"), ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.role }
  }
}


fun dataToBinary(
  data: Any /*String or ByteArray*/, encoding: IPC_DATA_ENCODING
) = when (encoding) {
  IPC_DATA_ENCODING.BINARY -> data as ByteArray
  IPC_DATA_ENCODING.BASE64 -> (data as String).toBase64ByteArray()
  IPC_DATA_ENCODING.UTF8 -> (data as String).encodeToByteArray()
}


fun dataToText(
  data: Any /*String or ByteArray*/, encoding: IPC_DATA_ENCODING
) = when (encoding) {
  IPC_DATA_ENCODING.BINARY -> (data as ByteArray).decodeToString()
  IPC_DATA_ENCODING.BASE64 -> (data as String).toBase64ByteArray().decodeToString()
  IPC_DATA_ENCODING.UTF8 -> data as String
}
