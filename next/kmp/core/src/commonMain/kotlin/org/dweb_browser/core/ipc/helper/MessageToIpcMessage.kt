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
    subclass(IpcLifecycle::class)
    subclass(IpcReqMessage::class)
    subclass(IpcResMessage::class)
    subclass(IpcEvent::class)
    subclass(IpcStreamData::class)
    subclass(IpcStreamPulling::class)
    subclass(IpcStreamPaused::class)
    subclass(IpcStreamEnd::class)
    subclass(IpcStreamAbort::class)
    subclass(IpcError::class)
    subclass(IpcFork::class)
  }
  polymorphic(IpcLifecycleState::class) {
    subclass(IpcLifecycleInit::class)
    subclass(IpcLifecycleOpening::class)
    subclass(IpcLifecycleOpened::class)
    subclass(IpcLifecycleClosing::class)
    subclass(IpcLifecycleClosed::class)
  }
  polymorphic(EndpointLifecycleState::class) {
    subclass(EndpointLifecycleInit::class)
    subclass(EndpointLifecycleOpening::class)
    subclass(EndpointLifecycleOpened::class)
    subclass(EndpointLifecycleClosing::class)
    subclass(EndpointLifecycleClosed::class)
  }
  polymorphic(EndpointMessage::class) {
    subclass(EndpointIpcMessage::class)
    subclass(EndpointLifecycle::class)
  }
}

val JsonIpc = Json {
  encodeDefaults = true
  ignoreUnknownKeys = true
  serializersModule = moduleIpc
}

@OptIn(ExperimentalSerializationApi::class)
val CborIpc = Cbor {
  encodeDefaults = true
  ignoreUnknownKeys = true
  serializersModule = moduleIpc
}

@OptIn(ExperimentalSerializationApi::class)
inline fun cborToEndpointMessage(data: ByteArray) =
  CborIpc.decodeFromByteArray<EndpointMessage>(data)

inline fun jsonToEndpointMessage(data: String) = JsonIpc.decodeFromString<EndpointMessage>(data)

@OptIn(ExperimentalSerializationApi::class)
fun endpointMessageToCbor(message: EndpointMessage) =
  CborIpc.encodeToByteArray<EndpointMessage>(serializableEndpointMessage(message, false))

fun endpointMessageToJson(message: EndpointMessage) =
  JsonIpc.encodeToString<EndpointMessage>(serializableEndpointMessage(message, true))

fun normalizeIpcMessage(ipcMessage: IpcMessage, ipc: Ipc): IpcMessage = when (ipcMessage) {
  is IpcReqMessage -> IpcServerRequest(
    ipcMessage.reqId,
    ipcMessage.url,
    ipcMessage.method,
    PureHeaders(ipcMessage.headers),
    IpcBodyReceiver.from(ipcMessage.metaBody, ipc),
    ipc
  )

  is IpcResMessage -> IpcResponse(
    ipcMessage.reqId,
    ipcMessage.statusCode,
    PureHeaders(ipcMessage.headers),
    IpcBodyReceiver.from(ipcMessage.metaBody, ipc),
    ipc
  )

  else -> ipcMessage
}


interface JsonAble {
  val jsonAble: Any
}

@Suppress("UNCHECKED_CAST")
private fun <T : EndpointMessage> serializableEndpointMessage(
  ipcPoolPack: T,
  jsonAble: Boolean,
): T =
  when (ipcPoolPack) {
    is EndpointIpcMessage -> when (val ipcMessage = ipcPoolPack.ipcMessage) {
      is IpcRequest -> ipcPoolPack.copy(ipcMessage = ipcMessage.ipcReqMessage) as T
      is IpcResponse -> ipcPoolPack.copy(ipcMessage = ipcMessage.ipcResMessage) as T
      is JsonAble -> if (jsonAble) ipcPoolPack.copy(ipcMessage = ipcMessage.jsonAble as IpcMessage) as T else ipcPoolPack
      else -> ipcPoolPack
    }

    else -> ipcPoolPack
  }


//fun ipcMessageToJson(data: IpcMessage) = JsonIpc.encodeToString(data)