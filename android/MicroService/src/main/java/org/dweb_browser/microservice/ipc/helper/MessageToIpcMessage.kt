package org.dweb_browser.microservice.ipc.helper

import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.Ipc

fun jsonToIpcMessage(data: String, ipc: Ipc): Any? {
  if (data == "close" || data == "ping" || data == "pong") {
    return data
  }

  return runBlockingCatching {
    when (gson.fromJson(data, IpcMessage::class.java).type) {
      IPC_MESSAGE_TYPE.REQUEST -> gson.fromJson(data, IpcReqMessage::class.java).let {
        IpcRequest(
          it.req_id,
          it.url,
          it.method,
          IpcHeaders(it.headers),
          IpcBodyReceiver.from(it.metaBody, ipc),
          ipc
        )
      }

      IPC_MESSAGE_TYPE.RESPONSE -> gson.fromJson(data, IpcResMessage::class.java).let {
        IpcResponse(
          it.req_id,
          it.statusCode,
          IpcHeaders(it.headers),
          IpcBodyReceiver.from(it.metaBody, ipc),
          ipc
        )
      }

      IPC_MESSAGE_TYPE.EVENT -> gson.fromJson(data, IpcEvent::class.java)
      IPC_MESSAGE_TYPE.STREAM_DATA -> gson.fromJson(data, IpcStreamData::class.java)
      IPC_MESSAGE_TYPE.STREAM_PULL -> gson.fromJson(data, IpcStreamPulling::class.java)
      IPC_MESSAGE_TYPE.STREAM_PAUSED -> gson.fromJson(data, IpcStreamPaused::class.java)
      IPC_MESSAGE_TYPE.STREAM_END -> gson.fromJson(data, IpcStreamEnd::class.java)
      IPC_MESSAGE_TYPE.STREAM_ABORT -> gson.fromJson(data, IpcStreamAbort::class.java)
    }
  }.getOrDefault(data)

}
