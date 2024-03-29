package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.pure.http.PureHeaders

val moduleIpc = SerializersModule {
  polymorphic(IpcMessage::class) {
    subclass(IpcReqMessage::class)
    subclass(IpcResMessage::class)
    subclass(IpcEvent::class)
    subclass(IpcStreamData::class)
    subclass(IpcStreamPulling::class)
    subclass(IpcStreamPaused::class)
    subclass(IpcStreamEnd::class)
    subclass(IpcStreamAbort::class)
    subclass(IpcError::class)
  }
}

val JsonIpc = Json {
  ignoreUnknownKeys = true
  serializersModule = moduleIpc
}

@OptIn(ExperimentalSerializationApi::class)
val CborIpc = Cbor {
  ignoreUnknownKeys = true
  serializersModule = moduleIpc
}

object IpcMessageConst {
  @OptIn(ExperimentalSerializationApi::class)
  val closeCborByteArray = CborIpc.encodeToByteArray("close")
  val closeByteArray = "close".encodeToByteArray()

  @OptIn(ExperimentalSerializationApi::class)
  val pingCborByteArray = CborIpc.encodeToByteArray("ping")
  val pingByteArray = "ping".encodeToByteArray()

  @OptIn(ExperimentalSerializationApi::class)
  val pongCborByteArray = CborIpc.encodeToByteArray("pong")
  val pongByteArray = "pong".encodeToByteArray()
}

/***
 * 用于cbor解析type属于何种类型
 * 原因：无法使用IpcMessage，因为IpcMessage是密封类，无法直接构造
 */
private class IpcMessageType(val type: IPC_MESSAGE_TYPE)

fun unByteSpecial(data: ByteArray): ByteArray? {
  // 判断特殊的字节
  if (data.contentEquals(IpcMessageConst.closeCborByteArray) || data.contentEquals(IpcMessageConst.pingCborByteArray) || data.contentEquals(
      IpcMessageConst.pongCborByteArray
    )
  ) {
    return data
  }
  return null
}

@OptIn(ExperimentalSerializationApi::class)
inline fun cborToIpcPoolPack(data: ByteArray) = CborIpc.decodeFromByteArray<EndpointMessage>(data)
inline fun jsonToIpcPoolPack(data: String) = JsonIpc.decodeFromString<EndpointMessage>(data)

@OptIn(ExperimentalSerializationApi::class)
fun ipcPoolPackToCbor(message: EndpointMessage) =
  CborIpc.encodeToByteArray<EndpointMessage>(serializableIpcMessage(message))

fun ipcPoolPackToJson(message: EndpointMessage) =
  JsonIpc.encodeToString<EndpointMessage>(serializableIpcMessage(message))

fun normalizeIpcMessage(ipcPoolPack: EndpointMessage, ipc: Ipc) =
  when (val ipcMessage = ipcPoolPack.ipcMessage) {
    is IpcReqMessage -> IpcServerRequest(
      ipcMessage.reqId,
      ipcMessage.url,
      ipcMessage.method,
      PureHeaders(ipcMessage.headers),
      IpcBodyReceiver.from(ipcMessage.metaBody, ipc),
      ipc
    ).let { ipcPoolPack.copy(ipcMessage = it) }

    is IpcResMessage -> IpcResponse(
      ipcMessage.reqId,
      ipcMessage.statusCode,
      PureHeaders(ipcMessage.headers),
      IpcBodyReceiver.from(ipcMessage.metaBody, ipc),
      ipc
    ).let { ipcPoolPack.copy(ipcMessage = it) }

    else -> ipcPoolPack
  }

fun serializableIpcMessage(ipcPoolPack: EndpointMessage) =
  when (val ipcMessage = ipcPoolPack.ipcMessage) {
    is IpcRequest -> ipcPoolPack.copy(ipcMessage = ipcMessage.ipcReqMessage)
    is IpcResponse -> ipcPoolPack.copy(ipcMessage = ipcMessage.ipcResMessage)
    else -> ipcPoolPack
  }


fun ipcMessageToJson(data: IpcMessage) = JsonIpc.encodeToString(data)