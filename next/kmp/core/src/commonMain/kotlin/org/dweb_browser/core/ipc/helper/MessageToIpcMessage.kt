package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.CborLoose
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.pure.http.PureHeaders

object IpcMessageConst {
  @OptIn(ExperimentalSerializationApi::class)
  val closeCborByteArray = CborLoose.encodeToByteArray("close")
  val closeByteArray = "close".encodeToByteArray()

  @OptIn(ExperimentalSerializationApi::class)
  val pingCborByteArray = CborLoose.encodeToByteArray("ping")
  val pingByteArray = "ping".encodeToByteArray()

  @OptIn(ExperimentalSerializationApi::class)
  val pongCborByteArray = CborLoose.encodeToByteArray("pong")
  val pongByteArray = "pong".encodeToByteArray()
}

/***
 * 用于cbor解析type属于何种类型
 * 原因：无法使用IpcMessage，因为IpcMessage是密封类，无法直接构造
 */
private class IpcMessageType(val type: IPC_MESSAGE_TYPE)

fun unByteSpecial(data: ByteArray): ByteArray? {
  // 判断特殊的字节
  if (data.contentEquals(IpcMessageConst.closeCborByteArray) ||
    data.contentEquals(IpcMessageConst.pingCborByteArray) ||
    data.contentEquals(IpcMessageConst.pongCborByteArray)
  ) {
    return data
  }
  return null
}

@OptIn(ExperimentalSerializationApi::class)
fun cborToIpcMessage(data: ByteArray, ipc: Ipc): IpcMessage {
  try {
    // 解析出对象
    val ipcMessage = CborLoose.decodeFromByteArray<IpcMessageType>(data)
    return when (ipcMessage.type) {
      IPC_MESSAGE_TYPE.REQUEST -> Cbor.decodeFromByteArray<IpcReqMessage>(data).let {
        // 这里是指接收到数据而反序列化出 IpcRequest，所以不是发起者，而是响应者，因此是 IpcServerRequest
        IpcServerRequest(
          it.reqId,
          it.url,
          it.method,
          PureHeaders(it.headers),
          IpcBodyReceiver.from(it.metaBody, ipc),
          ipc
        )
      }

      IPC_MESSAGE_TYPE.RESPONSE -> Cbor.decodeFromByteArray<IpcResMessage>(data).let {
        IpcResponse(
          it.reqId,
          it.statusCode,
          PureHeaders(it.headers),
          IpcBodyReceiver.from(it.metaBody, ipc),
          ipc
        )
      }

      IPC_MESSAGE_TYPE.EVENT -> Cbor.decodeFromByteArray<IpcEvent>(data)
      IPC_MESSAGE_TYPE.STREAM_DATA -> Cbor.decodeFromByteArray<IpcStreamData>(data)
      IPC_MESSAGE_TYPE.STREAM_PULL -> Cbor.decodeFromByteArray<IpcStreamPulling>(data)
      IPC_MESSAGE_TYPE.STREAM_PAUSED -> Cbor.decodeFromByteArray<IpcStreamPaused>(data)
      IPC_MESSAGE_TYPE.STREAM_END -> Cbor.decodeFromByteArray<IpcStreamEnd>(data)
      IPC_MESSAGE_TYPE.STREAM_ABORT -> Cbor.decodeFromByteArray<IpcStreamAbort>(data)
      IPC_MESSAGE_TYPE.LIFE_CYCLE -> Cbor.decodeFromByteArray<IpcLifeCycle>(data)
      IPC_MESSAGE_TYPE.ERROR -> Cbor.decodeFromByteArray<IpcError>(data)
    }
  } catch (e: Exception) {
    e.printStackTrace()
    return IpcError(400, e.message)
  }
}

@OptIn(ExperimentalSerializationApi::class)
fun cborToIpcPoolPack(pack: ByteArray) = run { Cbor.decodeFromByteArray<PackIpcMessage>(pack) }

@OptIn(ExperimentalSerializationApi::class)
fun ipcPoolPackToCbor(pack: PackIpcMessage) = run { Cbor.encodeToByteArray<PackIpcMessage>(pack) }

@OptIn(ExperimentalSerializationApi::class)
fun ipcMessageToCbor(data: IpcMessage) = when (data) {
  is IpcRequest -> Cbor.encodeToByteArray<IpcReqMessage>(data.ipcReqMessage)
  is IpcResponse -> Cbor.encodeToByteArray<IpcResMessage>(data.ipcResMessage)
  is IpcStreamData -> Cbor.encodeToByteArray<IpcStreamData>(data)
  is IpcEvent -> Cbor.encodeToByteArray<IpcEvent>(data)
  is IpcEventJsonAble -> Cbor.encodeToByteArray<IpcEventJsonAble>(data)
  is IpcReqMessage -> Cbor.encodeToByteArray<IpcReqMessage>(data)
  is IpcResMessage -> Cbor.encodeToByteArray<IpcResMessage>(data)
  is IpcStreamAbort -> Cbor.encodeToByteArray<IpcStreamAbort>(data)
  is IpcStreamDataJsonAble -> Cbor.encodeToByteArray<IpcStreamDataJsonAble>(data)
  is IpcStreamEnd -> Cbor.encodeToByteArray<IpcStreamEnd>(data)
  is IpcStreamPaused -> Cbor.encodeToByteArray<IpcStreamPaused>(data)
  is IpcStreamPulling -> Cbor.encodeToByteArray<IpcStreamPulling>(data)
  is IpcLifeCycle -> Cbor.encodeToByteArray<IpcLifeCycle>(data)
  is IpcError -> Cbor.encodeToByteArray<IpcError>(data)
}

fun jsonToIpcPack(data: String): IpcPoolPackString {
  return Json.decodeFromString<IpcPoolPackString>(data)
}

fun jsonToIpcPoolPack(data: String, ipc: Ipc): IpcMessage {
  try {
    val typeInfo = Regex(""""type"\s*:\s*(\d+)""").find(data)
      ?: throw Exception("jsonToIpcMessage: parsing failed packet=>$data")
    return when (JsonLoose.decodeFromString<IPC_MESSAGE_TYPE>(typeInfo.groupValues[1])) {
      IPC_MESSAGE_TYPE.REQUEST -> Json.decodeFromString<IpcReqMessage>(data)
        .let {
          // 这里是指接收到数据而反序列化出 IpcRequest，所以不是发起者，而是响应者，因此是 IpcServerRequest
          IpcServerRequest(
            it.reqId,
            it.url,
            it.method,
            PureHeaders(it.headers),
            IpcBodyReceiver.from(it.metaBody, ipc),
            ipc
          )
        }

      IPC_MESSAGE_TYPE.RESPONSE -> Json.decodeFromString<IpcResMessage>(data)
        .let {
          IpcResponse(
            it.reqId,
            it.statusCode,
            PureHeaders(it.headers),
            IpcBodyReceiver.from(it.metaBody, ipc),
            ipc
          )
        }

      IPC_MESSAGE_TYPE.EVENT -> Json.decodeFromString<IpcEvent>(data)
      IPC_MESSAGE_TYPE.STREAM_DATA -> Json.decodeFromString<IpcStreamData>(data)
      IPC_MESSAGE_TYPE.STREAM_PULL -> Json.decodeFromString<IpcStreamPulling>(data)
      IPC_MESSAGE_TYPE.STREAM_PAUSED -> Json.decodeFromString<IpcStreamPaused>(data)
      IPC_MESSAGE_TYPE.STREAM_END -> Json.decodeFromString<IpcStreamEnd>(data)
      IPC_MESSAGE_TYPE.STREAM_ABORT -> Json.decodeFromString<IpcStreamAbort>(data)
      IPC_MESSAGE_TYPE.LIFE_CYCLE -> Json.decodeFromString<IpcLifeCycle>(data)
      IPC_MESSAGE_TYPE.ERROR -> Json.decodeFromString<IpcError>(data)
    }
  } catch (e: Exception) {
    e.printStackTrace()
    return IpcError(500, e.message)
  }
}

fun ipcPoolPackToJson(pack: IpcPoolPack): String {
  val ipcMessageString = ipcMessageToJson(pack.ipcMessage)
  return Json.encodeToString(IpcPoolPackString(pack.pid, ipcMessageString))
}

fun ipcMessageToJson(data: IpcMessage) = when (data) {
  is IpcRequest -> Json.encodeToString(data.ipcReqMessage)
  is IpcResponse -> Json.encodeToString(data.ipcResMessage)
  is IpcStreamData -> Json.encodeToString(data.jsonAble)
  is IpcEvent -> Json.encodeToString(data.jsonAble)
  is IpcEventJsonAble -> Json.encodeToString(data)
  is IpcReqMessage -> Json.encodeToString(data)
  is IpcResMessage -> Json.encodeToString(data)
  is IpcStreamAbort -> Json.encodeToString(data)
  is IpcStreamDataJsonAble -> Json.encodeToString(data)
  is IpcStreamEnd -> Json.encodeToString(data)
  is IpcStreamPaused -> Json.encodeToString(data)
  is IpcStreamPulling -> Json.encodeToString(data)
  is IpcLifeCycle -> Json.encodeToString(data)
  is IpcError -> Json.encodeToString(data)
}

