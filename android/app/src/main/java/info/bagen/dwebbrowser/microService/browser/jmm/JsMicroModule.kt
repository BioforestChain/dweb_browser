package info.bagen.dwebbrowser.microService.browser.jmm

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.dwebview.ipcWeb.Native2JsIpc
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.toBase64Url
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.ConnectResult
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.core.connectAdapterManager
import org.dweb_browser.microservice.help.boolean
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.help.int
import org.dweb_browser.microservice.help.stream
import org.dweb_browser.microservice.help.types.CommonAppManifest
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
import org.dweb_browser.microservice.help.types.IpcSupportProtocols
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.help.types.MicroModuleManifest
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.ipc.helper.IpcMessageArgs
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import java.util.Random

fun debugJsMM(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("JsMM", tag, msg, err)

open class JsMicroModule(val metadata: JmmAppInstallManifest) : MicroModule(
  MicroModuleManifest().apply {
    assign(metadata)
    categories += MICRO_MODULE_CATEGORY.Application
    icons.ifEmpty {
      icons += ImageResource(src = metadata.logo)
    }
    mmid = metadata.id
    ipc_support_protocols = IpcSupportProtocols(
      cbor = true, protobuf = false, raw = true
    )
  }
) {

  companion object {
    init {
      val nativeToWhiteList = listOf<MMID>("js.browser.dweb")

      data class JsMM(val jmm: JsMicroModule, val rmm: MicroModule)
      connectAdapterManager.append(-1) { fromMM, toMM, reason ->

        val jsMM = if (nativeToWhiteList.contains(toMM.mmid)) null
        else if (toMM is JsMicroModule) JsMM(toMM, fromMM)
        else if (fromMM is JsMicroModule) JsMM(fromMM, toMM)
        else null

        debugJsMM(
          "JsMM/connectAdapter", "fromMM:${fromMM.mmid} => toMM:${toMM.mmid} ==> jsMM:$jsMM"
        )
        jsMM?.let {
          /**
           * 与 NMM 相比，这里会比较难理解：
           * 因为这里是直接创建一个 Native2JsIpc 作为 ipcForFromMM，
           * 而实际上的 ipcForToMM ，是在 js-context 里头去创建的，因此在这里是 一个假的存在
           *
           * 也就是说。如果是 jsMM 内部自己去执行一个 connect，那么这里返回的 ipcForFromMM，其实还是通往 js-context 的， 而不是通往 toMM的。
           * 也就是说，能跟 toMM 通讯的只有 js-context，这里无法通讯。
           */
          val originIpc = jsMM.jmm.ipcBridge(jsMM.rmm.mmid)
          fromMM.beConnect(originIpc, reason)
          toMM.beConnect(originIpc, reason)
          return@append ConnectResult(ipcForFromMM = originIpc, ipcForToMM = originIpc)
        }
      }
    }
  }

  /**
   * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
   * 所以不会和其它程序所使用的 pid 冲突
   */
  private var processId: String? = null
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  val pid = ByteArray(8).also { Random().nextBytes(it) }.toBase64Url()
  private suspend fun createNativeStream(): ReadableStreamIpc {
    debugJsMM("createNativeStream", "pid=$pid, root=${metadata.server}")
    processId = pid
    val streamIpc = ReadableStreamIpc(this, "code-server")
    streamIpc.onRequest { (request, ipc) ->
      debugJsMM("streamIpc.onRequest", "path=${request.uri.path}")
      val response = if (request.uri.path.endsWith("/")) {
        Response(Status.FORBIDDEN)
      } else {
        // 正则含义是将两个或以上的 / 斜杆直接转为单斜杆
        nativeFetch(
          "file://" + (metadata.server.root + request.uri.path).replace(Regex("/{2,}"), "/")
        )
      }
      ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
    }
    streamIpc.bindIncomeStream(
      nativeFetch(
        Request(
          Method.POST,
          Uri.of("file://js.browser.dweb/create-process").query("entry", metadata.server.entry)
            .query("process_id", pid)
        ).body(streamIpc.stream)
      ).stream()
    )
    this.addToIpcSet(streamIpc)
    return streamIpc
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    debugJsMM("bootstrap...", "$mmid/$metadata")

    createNativeStream()
    /**
     * 拿到与js.browser.dweb模块的直连通道，它会将 Worker 中的数据带出来
     */
    val (jsIpc) = bootstrapContext.dns.connect("js.browser.dweb")

    // 监听关闭事件
    jsIpc.onClose {
      if (running) {
        shutdown()
      }
    }

    /**
     * 这里 jmm 的对于 request 的默认处理方式是将这些请求直接代理转发出去
     * TODO 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["camera.sys.dweb"]
     */
    jsIpc.onRequest { (ipcRequest, ipc) ->
      /// WARN 这里不再受理 file://<domain>/ 的请求，只处理 http[s]:// | file:/// 这些原生的请求
      val scheme = ipcRequest.uri.scheme
      val host = ipcRequest.uri.host
      debugJsMM("onProxyRequest", "start ${ipcRequest.uri}")
      if (scheme == "file" && host.endsWith(".dweb")) {
        val jsWebIpc = connect(host)
        jsWebIpc.emitMessage(IpcMessageArgs(ipcRequest, jsWebIpc))
      } else {
        runBlockingCatching(ioAsyncExceptionHandler) {
          /// 在js-worker一侧：与其它模块的通讯，统一使用 connect 之后再发送 request 来实现。
          // 转发请求
          val request = ipcRequest.toRequest()
          val response = nativeFetch(request)
          debugJsMM("onProxyRequest", "end ${ipcRequest.uri}")
          val ipcResponse = IpcResponse.fromResponse(ipcRequest.req_id, response, ipc)
          debugJsMM("onProxyRequest", "send ${ipcRequest.uri}")
          ipc.postMessage(ipcResponse)
          debugJsMM("onProxyRequest", "send ${ipcRequest.uri}")
        }.onFailure {
          debugJsMM("onProxyRequest", "fail ${ipcRequest.uri} ${it}")
          ipc.postMessage(
            IpcResponse.fromText(
              ipcRequest.req_id, 500, text = it.message ?: "", ipc = ipc
            )
          )
        }
      }

    }

    /**
     * 收到 Worker 的事件，如果是指令，执行一些特定的操作
     */
    jsIpc.onEvent { (ipcEvent) ->
      /**
       * 收到要与其它模块进行ipc连接的指令
       */
      if (ipcEvent.name == "dns/connect") {
        ioAsyncScope.launch {
          data class DnsConnectEvent(val mmid: MMID)

          val event = gson.fromJson(ipcEvent.text, DnsConnectEvent::class.java)
          try {
            /**
             * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
             * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
             *
             * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
             */
            /**
             * 向目标模块发起连接，注意，这里是很特殊的，因为我们自定义了 JMM 的连接适配器 connectAdapterManager，
             * 所以 JsMicroModule 这里作为一个中间模块，是没法直接跟其它模块通讯的。
             *
             * TODO 如果有必要，未来需要让 connect 函数支持 force 操作，支持多次连接。
             */
            val (targetIpc) = bootstrapContext.dns.connect(event.mmid)
            /// 只要不是我们自己创建的直接连接的通道，就需要我们去 创造直连并进行桥接
            if (targetIpc.remote.mmid != mmid) {
              ipcBridge(event.mmid, targetIpc)
            }
          } catch (e: Exception) {
            ipcConnectFail(mmid, e);
          }
        }
      }
      if (ipcEvent.name == "restart") {
        // 调用重启
        bootstrapContext.dns.restart(mmid)
      }
      null
    }
  }


  private val fromMMIDOriginIpcWM = mutableMapOf<MMID, PromiseOut<Ipc>>();

  class JmmIpc(port_id: Int, remote: IMicroModuleManifest) : Native2JsIpc(port_id, remote) {}

  /**
   * 桥接ipc到js内部：
   * 使用 create-ipc 指令来创建一个代理的 WebMessagePortIpc ，然后我们进行中转
   */
  private fun _ipcBridge(fromMMID: MMID, targetIpc: Ipc?) = fromMMIDOriginIpcWM.getOrPut(fromMMID) {
    PromiseOut<Ipc>().also { po ->
      ioAsyncScope.launch {
        try {

          debugJsMM("ipcBridge", "fromMmid:$fromMMID targetIpc:$targetIpc")
          /**
           * 向js模块发起连接
           */
          val portId = nativeFetch(
            Uri.of("file://js.browser.dweb/create-ipc").query("process_id", pid)
              .query("mmid", fromMMID)
          ).int()
          val originIpc = JmmIpc(portId, this@JsMicroModule)

          /// 如果传入了 targetIpc，那么启动桥接模式，我们会中转所有的消息给 targetIpc，包括关闭，那么这个 targetIpc 理论上就可以作为 originIpc 的代理
          if (targetIpc != null) {
            /**
             * 将两个消息通道间接互联
             */
            originIpc.onMessage { (ipcMessage) ->
              targetIpc.postMessage(ipcMessage)
            }
            targetIpc.onMessage { (ipcMessage) ->
              originIpc.postMessage(ipcMessage)
            }
            /**
             * 监听关闭事件
             */
            originIpc.onClose {
              fromMMIDOriginIpcWM.remove(targetIpc.remote.mmid)
              targetIpc.close()
            }
            targetIpc.onClose {
              fromMMIDOriginIpcWM.remove(originIpc.remote.mmid)
              originIpc.close()
            }
          } else {
            originIpc.onClose {
              fromMMIDOriginIpcWM.remove(originIpc.remote.mmid)
            }
          }
          po.resolve(originIpc);
        } catch (e: Exception) {
          debugJsMM("_ipcBridge Error", e)
          po.reject(e)
        }
      }
    }
  }

  private suspend fun ipcBridge(fromMMID: MMID, targetIpc: Ipc? = null) =
    _ipcBridge(fromMMID, targetIpc).waitPromise();

  private suspend fun ipcConnectFail(mmid: MMID, reason: Any): Boolean {
    val errMessage = if (reason is Exception) {
      reason.message + "\n" + (reason.message ?: "")
    } else {
      reason.toString()
    }
    val url = "file://js.browser.dweb/create-ipc-fail?process_id=$pid&mmid=$mmid&reason=$errMessage"
    return nativeFetch(url).boolean()
  }

  override suspend fun _shutdown() {
    debugJsMM("closeJsProcessSignal emit", "$mmid/$metadata")
    fromMMIDOriginIpcWM.forEach { map ->
      val ipc = map.value.waitPromise()
      ipc.close()
    }
    fromMMIDOriginIpcWM.clear()
    processId = null
    ioAsyncScope.cancel()
  }

  override fun toManifest(): CommonAppManifest {
    return this.metadata.toCommonAppManifest()
  }
}