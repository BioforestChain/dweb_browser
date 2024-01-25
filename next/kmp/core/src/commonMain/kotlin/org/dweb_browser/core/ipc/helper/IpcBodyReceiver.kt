package org.dweb_browser.core.ipc.helper

import kotlinx.atomicfu.atomic
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureStream

val debugIpcBodyReceiver = Debugger("ipc-body-receiver")


class IpcBodyReceiver(
  override val metaBody: MetaBody,
  ipc: Ipc,
) : IpcBody() {

  init {
    /// 将第一次得到这个metaBody的 ipc 保存起来，这个ipc将用于接收
    if (metaBody.type.isStream && metaBody.streamId != null) {
      CACHE.streamId_receiverIpc_Map.getOrPut(metaBody.streamId) {
        ipc.onClose {
          CACHE.streamId_receiverIpc_Map.remove(metaBody.streamId)
        }
        metaBody.receiverPoolId = ipc.endpoint.poolId
        ipc
      }
    }
  }

  /// 因为是 abstract，所以得用 lazy 来延迟得到这些属性
  override val raw by lazy {
    // 处理流
    if (metaBody.type.isStream && metaBody.streamId != null) {
      val rawIpc = CACHE.streamId_receiverIpc_Map[metaBody.streamId]
        ?: throw Exception("no found ipc by metaId:${metaBody.streamId}")
      IPureBody.from(metaToStream(metaBody, rawIpc))
    } else when (metaBody.type.encoding) {
      /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
      IPC_DATA_ENCODING.UTF8 -> IPureBody.from(metaBody.data as String)
      IPC_DATA_ENCODING.BINARY -> IPureBody.from(metaBody.data as ByteArray)
      IPC_DATA_ENCODING.BASE64 -> IPureBody.from(
        metaBody.data as String,
        IPureBody.Companion.PureStringEncoding.Base64
      )

      else -> throw Exception("invalid metaBody type: ${metaBody.type}")
    }
  }

  companion object {
    // 支持快速从缓存里快速拿到IpcBody
    fun from(metaBody: MetaBody, ipc: Ipc): IpcBody {
      return CACHE.streamId_ipcBodySender_Map[metaBody.streamId] ?: IpcBodyReceiver(metaBody, ipc)
    }

    /**
     * 绑定Body到ReadableSteam输出PureStream
     * @return {String | ByteArray | InputStream}
     */
    fun metaToStream(metaBody: MetaBody, ipc: Ipc): PureStream {
      val streamId = metaBody.streamId!!

      /**
       * 默认是暂停状态
       */
      val paused = atomic(true)
      val readableStream = ReadableStream(cid = "receiver=${streamId}", onStart = { controller ->
        // 注册关闭事件
        ipc.onClose {
          controller.closeWrite()
        }
        /// 如果有初始帧，直接存起来
        when (metaBody.type.encoding) {
          IPC_DATA_ENCODING.UTF8 -> (metaBody.data as String).encodeToByteArray()
          IPC_DATA_ENCODING.BINARY -> metaBody.data as ByteArray
          IPC_DATA_ENCODING.BASE64 -> (metaBody.data as String).toBase64ByteArray()
          else -> null
        }?.let { firstData -> controller.enqueueBackground(firstData) }

        ipc.onPulling(streamId) { message, close ->
          when (message) {
            is IpcStreamData -> {
              controller.enqueue(message.binary)
            }

            is IpcStreamEnd -> {
              // 关闭消息监听，关闭流监听
              close()
              controller.closeWrite()
            }

            else -> {
              debugIpcBodyReceiver("receiver", "Unknown message $message")
            }
          }
        }
      }, onOpenReader = { controller ->
        debugIpcBodyReceiver(
          "postPullMessage/$ipc/${controller.stream}", streamId
        )
        // 跟对面讲，我需要开始拉取数据了
        if (paused.getAndSet(false)) {
          ipc.postMessage(IpcStreamPulling(streamId))
        }
      }, onClose = {
        // 跟对面讲，我关闭了,不再接受消息了，可以丢弃这个ByteChannel的内容了
        ipc.postMessage(IpcStreamAbort(streamId))
      });

      debugIpcBodyReceiver("$ipc/$readableStream", "start by stream-id:${streamId}")

      return readableStream.stream
    }
  }
}