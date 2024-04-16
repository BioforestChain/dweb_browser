package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.IntEnumSerializer
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.toBase64ByteArray


const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024


//# endregion

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
  SERVER("server"), CLIENT("client"),
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.role }
  }
}


fun dataToBinary(
  data: Any, /*String or ByteArray*/ encoding: IPC_DATA_ENCODING,
) = when (encoding) {
  IPC_DATA_ENCODING.BINARY -> data as ByteArray
  IPC_DATA_ENCODING.BASE64 -> (data as String).toBase64ByteArray()
  IPC_DATA_ENCODING.UTF8 -> (data as String).encodeToByteArray()
}


fun dataToText(
  data: Any, /*String or ByteArray*/ encoding: IPC_DATA_ENCODING,
) = when (encoding) {
  IPC_DATA_ENCODING.BINARY -> (data as ByteArray).decodeToString()
  IPC_DATA_ENCODING.BASE64 -> (data as String).toBase64ByteArray().decodeToString()
  IPC_DATA_ENCODING.UTF8 -> data as String
}
