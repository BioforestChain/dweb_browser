package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.pure.http.PureHeaders

val moduleIpc = SerializersModule {
  polymorphic(IpcRawMessage::class) {
    subclass(IpcLifecycle::class)
    subclass(IpcReqMessage::class)
    subclass(IpcResMessage::class)
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
  polymorphic(EndpointRawMessage::class) {
    subclass(EndpointIpcRawMessage::class)
    subclass(EndpointLifecycle::class)
  }
}

val JsonIpc = Json {
  encodeDefaults = true
  ignoreUnknownKeys = true
  serializersModule = moduleIpc + SerializersModule {
    polymorphic(IpcRawMessage::class) {
      subclass(IpcEventRawString::class)
      subclass(IpcStreamDataRawString::class)
    }
  }
}

@OptIn(ExperimentalSerializationApi::class)
val CborIpc = Cbor {
  encodeDefaults = true
  ignoreUnknownKeys = true
  serializersModule = moduleIpc + SerializersModule {
    polymorphic(IpcRawMessage::class) {
      subclass(IpcEventRawBinary::class)
      subclass(IpcStreamDataRawBinary::class)
    }
  }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun cborToEndpointMessage(data: ByteArray) =
  CborIpc.decodeFromByteArray<EndpointRawMessage>(data)

inline fun jsonToEndpointMessage(data: String) = JsonIpc.decodeFromString<EndpointRawMessage>(data)

@OptIn(ExperimentalSerializationApi::class)
fun endpointMessageToCbor(message: EndpointMessage) =
  CborIpc.encodeToByteArray<EndpointRawMessage>(message.toEndpointRawMessage(true))

fun endpointMessageToJson(message: EndpointMessage) =
  JsonIpc.encodeToString<EndpointRawMessage>(message.toEndpointRawMessage(false))

fun IpcRawMessage.toIpcMessage(ipc: Ipc): IpcMessage {
  return when (val raw = this) {
    is IpcReqMessage -> IpcServerRequest(
      raw.reqId,
      raw.url,
      raw.method,
      PureHeaders(raw.headers),
      IpcBodyReceiver.from(raw.metaBody, ipc),
      ipc
    )

    is IpcResMessage -> IpcResponse(
      raw.reqId,
      raw.statusCode,
      PureHeaders(raw.headers),
      IpcBodyReceiver.from(raw.metaBody, ipc),
      ipc
    )

    is IpcError -> raw
    is IpcEventRawString -> raw.toIpcEvent()
    is IpcEventRawBinary -> raw.toIpcEvent()
    is IpcFork -> raw
    is IpcLifecycle -> raw
    is IpcStreamAbort -> raw
    is IpcStreamDataRawString -> raw.toIpcStreamData()
    is IpcStreamDataRawBinary -> raw.toIpcStreamData()
    is IpcStreamEnd -> raw
    is IpcStreamPaused -> raw
    is IpcStreamPulling -> raw
  }
}

fun IpcMessage.toIpcRawMessage(binary: Boolean): IpcRawMessage {
  return when (val msg = this) {
    is IpcError -> msg
    is IpcEvent -> msg.asRawAble(binary)
    is IpcFork -> msg
    is IpcLifecycle -> msg
    is IpcClientRequest -> msg.asRawAble(binary)
    is IpcServerRequest -> msg.asRawAble(binary)
    is IpcResponse -> msg.asRawAble(binary)
    is IpcStreamAbort -> msg
    is IpcStreamData -> msg.asRawAble(binary)
    is IpcStreamEnd -> msg
    is IpcStreamPaused -> msg
    is IpcStreamPulling -> msg
  }
}

interface RawAble<T : Any> {
  val stringAble: T
  val binaryAble get() = stringAble
  fun asRawAble(binary: Boolean) = when {
    binary -> binaryAble
    else -> stringAble
  }
}

fun EndpointMessage.toEndpointRawMessage(binary: Boolean): EndpointRawMessage =
  when (val msg = this) {
    is EndpointIpcMessage -> msg.asRawAble(binary)
    is EndpointLifecycle -> msg
  }

//fun EndpointRawMessage.toEndpointMessage(ipc: Ipc): EndpointMessage = when (val raw = this) {
//  is EndpointIpcRawMessage -> raw.toEndpointMessage(ipc)
//  is EndpointLifecycle -> raw
//}


//fun ipcMessageToJson(data: IpcMessage) = JsonIpc.encodeToString(data)