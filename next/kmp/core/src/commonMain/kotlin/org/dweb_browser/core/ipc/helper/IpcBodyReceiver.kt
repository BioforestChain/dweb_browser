package org.dweb_browser.core.ipc.helper

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.base64Binary
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.utf8Binary
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
        ipc.onClosed {
          CACHE.streamId_receiverIpc_Map.remove(metaBody.streamId)
        }
        metaBody.receiverPoolId = ipc.pool.poolId
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

      lateinit var ipcCloseDisposable: DisposableHandle;
      val metaToStreamConsumer = ipc.onStream("metaToStream")
      var isStreamEnd = false

      val readableStream =
        ReadableStream(ipc.scope, cid = "receiver=${streamId}", onStart = { controller ->
          // 注册关闭事件
          ipcCloseDisposable = ipc.onClosed {
            ipc.launchJobs += launch {
              controller.closeWrite()
            }
          }
          /// 如果有初始帧，直接存起来
          when (metaBody.type.encoding) {
            IPC_DATA_ENCODING.UTF8 -> (metaBody.data as String).utf8Binary
            IPC_DATA_ENCODING.BINARY -> metaBody.data as ByteArray
            IPC_DATA_ENCODING.BASE64 -> (metaBody.data as String).base64Binary
            else -> null
          }?.let { firstData -> controller.background { controller.enqueue(firstData) } }

          metaToStreamConsumer.collectIn(ipc.scope) { event ->
            val ipcStream = event.consumeFilter { it.stream_id == streamId } ?: return@collectIn;
            /// 这里在 background 中执行，避免阻塞，因为 metaToStreamConsumer 是互相阻塞的
            controller.background {
              when (ipcStream) {
                is IpcStreamData -> {
                  debugIpcBodyReceiver(
                    "receiver/StreamData/$ipc/${controller.stream}"
                  ) { ipcStream }
                  controller.enqueue(ipcStream.binary)
                }

                is IpcStreamEnd -> {
                  debugIpcBodyReceiver(
                    "receiver/StreamEnd/$ipc/${controller.stream}", ipcStream
                  )
                  isStreamEnd = true
                  controller.background {}
                  controller.closeWrite()
                }

                else -> {}
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
          ipcCloseDisposable.dispose()
          metaToStreamConsumer.close()
          if (!isStreamEnd && !ipc.isClosed) {
            // 跟对面讲，我关闭了,不再接受消息了，可以丢弃这个ByteChannel的内容了
            ipc.postMessage(IpcStreamAbort(streamId))
          }
        });

      debugIpcBodyReceiver("$ipc/$readableStream", "start by stream-id:${streamId}")

      return readableStream.stream
    }
  }
}