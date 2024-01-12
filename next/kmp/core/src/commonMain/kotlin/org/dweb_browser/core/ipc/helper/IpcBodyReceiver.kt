package org.dweb_browser.core.ipc.helper

import kotlinx.atomicfu.atomic
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.toBase64ByteArray

/**
 * metaBody 可能会被多次转发，
 * 但只有第一次得到这个 metaBody 的 ipc 才是它真正意义上的 Receiver
 */
class IpcBodyReceiver(
  override val metaBody: MetaBody,
  ipc: Ipc,
) : IpcBody() {


  /// 因为是 abstract，所以得用 lazy 来延迟得到这些属性
  override val raw by lazy {
    if (metaBody.type.isStream) {
      val rawIpc = CACHE.metaId_receiverIpc_Map[metaBody.metaId]
        ?: throw Exception("no found ipc by metaId:${metaBody.metaId}")
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

  init {
    /// 将第一次得到这个metaBody的 ipc 保存起来，这个ipc将用于接收
    if (metaBody.type.isStream) {
      CACHE.metaId_receiverIpc_Map.getOrPut(metaBody.metaId) {
        ipc.onClose {
          CACHE.metaId_receiverIpc_Map.remove(metaBody.metaId)
        }
        metaBody.receiverUid = ipc.uid
        ipc
      }
    }
  }

  companion object {

    fun from(metaBody: MetaBody, ipc: Ipc): IpcBody {
      return CACHE.metaId_ipcBodySender_Map[metaBody.metaId] ?: IpcBodyReceiver(metaBody, ipc)
    }


    /**
     * @return {String | ByteArray | InputStream}
     */
    fun metaToStream(metaBody: MetaBody, ipc: Ipc): PureStream {
      /// metaToStream
      val stream_id = metaBody.streamId!!;
      /**
       * 默认是暂停状态
       */
      val paused = atomic(true);
      val readableStream = ReadableStream(cid = "receiver=${stream_id}", onStart = { controller ->
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

        ipc.onStream { (message) ->
          when (message) {
            is IpcStreamData -> if (message.stream_id == stream_id) {
              debugIpcBody(
                "receiver/StreamData/$ipc/${controller.stream}", message
              )
              controller.enqueue(message.binary)
            }

            is IpcStreamEnd -> if (message.stream_id == stream_id) {
              debugIpcBody(
                "receiver/StreamEnd/$ipc/${controller.stream}", message
              )
              controller.closeWrite()
              offListener()
            }

            else -> {}
          }
        }
      }, onOpenReader = { controller ->
        debugIpcBody(
          "receiver/postPullMessage/$ipc/${controller.stream}", stream_id
        )
        if (paused.getAndSet(false)) {
          ipc.postMessage(IpcStreamPulling(stream_id))
        }
      }, onClose = {
        ipc.postMessage(IpcStreamAbort(stream_id))
      });

      debugIpcBody("receiver/$ipc/$readableStream", "start by stream-id:${stream_id}")

      return readableStream.stream
    }
  }
}