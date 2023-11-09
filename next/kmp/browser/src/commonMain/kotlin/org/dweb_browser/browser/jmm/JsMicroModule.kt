package org.dweb_browser.browser.jmm

import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.fullPath
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.help.types.IpcSupportProtocols
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.ReadableStreamIpc
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.ConnectResult
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.connectAdapterManager
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.printError
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.toBase64Url
import kotlin.random.Random

fun debugJsMM(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("JsMM", tag, msg, err)

open class JsMicroModule(val metadata: JmmAppInstallManifest) :
  MicroModule(MicroModuleManifest().apply {
    assign(metadata)
    categories += MICRO_MODULE_CATEGORY.Application
    icons.ifEmpty {
      icons = listOf(ImageResource(src = metadata.logo))
    }
    mmid = metadata.id
    ipc_support_protocols = IpcSupportProtocols(
      cbor = true, protobuf = false, raw = true
    )
  }) {

  companion object {
    /**
     * 当前JsMicroModule的版本
     */
    const val VERSION = 1
    const val PATCH = 0

    init {
      val nativeToWhiteList = listOf<MMID>("js.browser.dweb")

      data class JsMM(val jmm: JsMicroModule, val rmm: MicroModule)
      connectAdapterManager.append(1) { fromMM, toMM, reason ->

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
          val originIpc = ipcBridge(jsMM.rmm.mmid, jsMM.jmm, jsMM.jmm.pid, jsMM.jmm.fromMMIDOriginIpcWM, null)
          fromMM.beConnect(originIpc, reason)
          toMM.beConnect(originIpc, reason)
          return@append ConnectResult(ipcForFromMM = originIpc, ipcForToMM = originIpc)
        }
      }
    }
  }

  override suspend fun getSafeDwebPermissionProviders() =
    this.dweb_permissions.mapNotNull { PermissionProvider.from(this, it, metadata.bundle_url) }

  /**
   * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
   * 所以不会和其它程序所使用的 pid 冲突
   */
  private var processId: String? = null

  val pid = ByteArray(8).also { Random.nextBytes(it) }.toBase64Url()
  private suspend fun createNativeStream(): ReadableStreamIpc =
    withContext(ioAsyncScope.coroutineContext) {
      debugJsMM("createNativeStream", "pid=$pid, root=${metadata.server}")
      processId = pid
      val streamIpc = ReadableStreamIpc(this@JsMicroModule, "code-server")
      streamIpc.onRequest { (request, ipc) ->
        debugJsMM("streamIpc.onRequest", "path=${request.uri.fullPath}")
        val response = if (request.uri.fullPath.endsWith("/")) {
          PureResponse(HttpStatusCode.Forbidden)
        } else {
          // 正则含义是将两个或以上的 / 斜杆直接转为单斜杆
          nativeFetch(
            "file://" + (metadata.server.root + request.uri.fullPath).replace(Regex("/{2,}"), "/")
          )
        }
        ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
      }
      streamIpc.bindIncomeStream(
        nativeFetch(
          PureRequest(URLBuilder("file://js.browser.dweb/create-process").apply {
            parameters["entry"] = metadata.server.entry
            parameters["process_id"] = pid
          }.buildUnsafeString(), IpcMethod.POST, body = PureStreamBody(streamIpc.input.stream))
        ).stream()
      )
      this@JsMicroModule.addToIpcSet(streamIpc)
      streamIpc
    }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    debugJsMM(
      "bootstrap...", "$mmid/ minTarget:${metadata.minTarget} maxTarget:${metadata.maxTarget}"
    )
    metadata.canSupportTarget(VERSION, disMatchMinTarget = {
      throw RuntimeException(
        "应用($mmid)与容器版本不匹配，当前版本:${VERSION}，应用最低要求:${metadata.minTarget}",
        Exception("$short_name 无法启动"),
      )
    }, disMatchMaxTarget = {
      throw RuntimeException(
        "应用($mmid)与容器版本不匹配，当前版本:${VERSION}，应用最高兼容到:${metadata.maxTarget}",
        Exception("$short_name 无法启动"),
      )
    })

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
      val scheme = ipcRequest.uri.protocol.name
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
          val ipcResponse = IpcResponse.fromResponse(ipcRequest.req_id, response, ipc)
          ipc.postMessage(ipcResponse)
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
        @Serializable
        data class DnsConnectEvent(val mmid: MMID, val sub_protocols: List<String> = listOf())

        val event = JsonLoose.decodeFromString<DnsConnectEvent>(ipcEvent.text)
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
            ipcBridge(event.mmid, this@JsMicroModule, pid, fromMMIDOriginIpcWM, targetIpc)
          }
        } catch (e: Exception) {
          ipcConnectFail(mmid, e);
          printError("dns/connect", e)
        }
      }
      if (ipcEvent.name == "restart") {
        // 调用重启
        bootstrapContext.dns.restart(mmid)
      }
      null
    }
  }


  val fromMMIDOriginIpcWM = mutableMapOf<MMID, PromiseOut<Ipc>>();


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
  }

  override fun toManifest(): CommonAppManifest {
    return this.metadata.toCommonAppManifest()
  }
}
