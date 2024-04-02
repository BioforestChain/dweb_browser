package org.dweb_browser.browser.jmm

import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.fullPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.IpcSupportProtocols
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.WebMessageEndpoint
import org.dweb_browser.core.ipc.helper.IpcError
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.connectAdapterManager
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.dwebview.ipcWeb.native2JsEndpoint
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.alsoLaunchIn
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.listen
import org.dweb_browser.helper.printError
import org.dweb_browser.helper.toBase64Url
import org.dweb_browser.helper.withScope
import org.dweb_browser.pure.http.PureResponse
import kotlin.random.Random

val debugJsMM = Debugger("JsMM")

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
    const val VERSION = 2
    const val PATCH = 0

    init {
      val nativeToWhiteList = listOf<MMID>("js.browser.dweb")

      data class MmDirection(
        val endJmm: JsMicroModule.JmmRuntime,
        val startMm: IMicroModuleManifest
      )
      // jsMM对外创建ipc的适配器，给DnsNMM的connectMicroModules使用
      connectAdapterManager.append(1) { fromMM, toMM, reason ->

        val jsMM = if (nativeToWhiteList.contains(toMM.mmid)) null
        /// 这里优先判断 toMM 是否是 endJmm
        else if (toMM is JsMicroModule.JmmRuntime) MmDirection(toMM, fromMM)
        else if (fromMM is JsMicroModule.JmmRuntime) MmDirection(fromMM, toMM)
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
          val toJmmIpc = jsMM.endJmm.ipcBridge(jsMM.startMm.mmid) //(tip:创建到worker内部的桥接)
//          fromMM.beConnect(toJmmIpc, reason)
          toMM.beConnect(toJmmIpc, reason)
          toJmmIpc
//          val forwardIpc = toJmmIpc.toForwardIpc()
//          return@append if (jsMM.startMm.mmid == fromMM.mmid) {
//            ConnectResult(
//              ipcForFromMM = toJmmIpc,
//              ipcForToMM = forwardIpc,
//            )
//          } else {
//            ConnectResult(
//              ipcForFromMM = forwardIpc,
//              ipcForToMM = toJmmIpc,
//            )
//          }
        }
      }
    }
  }

  override suspend fun getSafeDwebPermissionProviders() =
    this.dweb_permissions.mapNotNull { PermissionProvider.from(this, it, metadata.bundle_url) }

  inner class JmmRuntime(override val bootstrapContext: BootstrapContext) : Runtime() {

    /**
     * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
     * 所以不会和其它程序所使用的 pid 冲突
     */
    private val pid = ByteArray(8).also { Random.nextBytes(it) }.toBase64Url()

    /**
     * 该对象有两个主要用途：
     * 1. 代理提供 dns
     *
     * > 这个会在模块关闭后释放，在模块启动后创建
     */
    private var fetchIpc: Ipc? = null

    /**创建js文件流*/
    private suspend fun createNativeStream(): Ipc {
      debugJsMM("createNativeStream", "pid=$pid, root=${metadata.server}")
      val processIpc = connect("js.browser.dweb").fork()
      // 让这个新的 ipc 作为 js-process 的通讯通道
      processIpc.request("file://js.browser.dweb/create-process")
      processIpc.onRequest.collectIn(mmScope) { request ->
        debugJsMM("processIpc.onRequest", "path=${request.uri.fullPath}")
        val response = if (request.uri.fullPath.endsWith("/")) {
          PureResponse(HttpStatusCode.Forbidden)
        } else {
          // 正则含义是将两个或以上的 / 斜杆直接转为单斜杆
          nativeFetch(
            "file://" + (metadata.server.root + request.uri.fullPath).replace(Regex("/{2,}"), "/")
          )
        }
        processIpc.postMessage(IpcResponse.fromResponse(request.reqId, response, processIpc))
      }
      this.linkIpc(processIpc)
      return processIpc
    }

    override suspend fun _bootstrap() {
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
      // 创建worker线程环境
      createNativeStream()
      /**
       * 拿到与js.browser.dweb模块的直连通道，它会将 Worker 中的数据带出来
       */
      val jsIpc = connect("js.browser.dweb")
      this.fetchIpc = jsIpc

      // 监听关闭事件
      mmScope.launch {
        if (isRunning) {
          jsIpc.awaitClosed()
          shutdown()
        }
      }

      /**
       * 这里 jmm 的对于 request 的默认处理方式是将这些请求直接代理转发出去
       * TODO 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["media-capture.sys.dweb"]
       */
      jsIpc.onRequest.onEach { ipcRequest ->
        /// WARN 这里不再受理 file://<domain>/ 的请求，只处理 http[s]:// | file:/// 这些原生的请求
        val scheme = ipcRequest.uri.protocol.name
        val host = ipcRequest.uri.host
        debugJsMM("onProxyRequest", "start ${ipcRequest.uri}")
        if (scheme == "file" && host.endsWith(".dweb")) {
          val jsWebIpc = connect(host)
          jsWebIpc.dispatchMessage(ipcRequest)
        } else {
          runCatching {
            withContext(ioAsyncExceptionHandler) {
              /// 在js-worker一侧：与其它模块的通讯，统一使用 connect 之后再发送 request 来实现。
              // 转发请求
              val request = ipcRequest.toPure().toClient()
              val response = nativeFetch(request)
              val ipcResponse = IpcResponse.fromResponse(ipcRequest.reqId, response, jsIpc)
              jsIpc.postMessage(ipcResponse)
            }
          }.onFailure {
            debugJsMM("onProxyRequest", "fail ${ipcRequest.uri} ${it}")
            jsIpc.postMessage(
              IpcResponse.fromText(
                ipcRequest.reqId, 500, text = it.message ?: "", ipc = jsIpc
              )
            )
          }
        }
      }.launchIn(mmScope)

      /**
       * 收到 Worker 的事件，如果是指令，执行一些特定的操作
       */
      jsIpc.onEvent.onEach { ipcEvent ->
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
            val targetIpc = connect(event.mmid) // 由上面的适配器产生
            /// 只要不是我们自己创建的直接连接的通道，就需要我们去 创造直连并进行桥接
            if (targetIpc.endpoint is WebMessageEndpoint) {
              ipcBridge(targetIpc.remote.mmid, targetIpc)
            }
            // 如果是jsMM互联，那么直接把port分配给两个人
            // ipcBridgeJsMM(mmid, targetIpc.remote.mmid)

            /**
             * 连接成功，正式告知它数据返回。注意，create-ipc虽然也会resolve任务，但是我们还是需要一个明确的done事件，来确保逻辑闭环
             * 否则如果遇到ipc重用，create-ipc是不会触发的
             */
            @Serializable
            data class DnsConnectDone(val connect: MMID, val result: MMID)

            /// event.mmid 可能是自协议，所以result提供真正的mmid
            val done = DnsConnectDone(
              connect = event.mmid,
              result = targetIpc.remote.mmid
            )
            jsIpc.postMessage(IpcEvent.fromUtf8("dns/connect/done", Json.encodeToString(done)))
          } catch (e: Exception) {
            jsIpc.postMessage(IpcError(503, e.message))
            printError("dns/connect", e)
          }
        }
        if (ipcEvent.name == "restart") {
          // 调用重启
          bootstrapContext.dns.restart(mmid)
        }
      }.launchIn(mmScope)
    }


    private val fromMMIDOriginIpcWM = mutableMapOf<MMID, CompletableDeferred<Ipc>>();


    /**
     * 桥接ipc到js内部：
     * 使用 create-ipc 指令来创建一个代理的 WebMessagePortIpc ，然后我们进行中转
     */
    private suspend fun ipcBridgeSelf(fromMMID: MMID): Ipc {
      /**
       * 向js模块发起连接
       */
      val portId = nativeFetch(
        URLBuilder("file://js.browser.dweb/create-ipc").apply {
          parameters["process_id"] = pid
          parameters["mmid"] = fromMMID
        }.buildUnsafeString()
      ).int()
      return kotlinIpcPool.createIpc(
        endpoint = native2JsEndpoint(portId),
        remote = this,
        locale = MicroModuleManifest().apply { mmid = fromMMID },
//      // 默认不自动开始
//      autoStart = false,
      )
//    // 跟自己代理的js-worker 建立 ipc，也就是说，对这个ipc做通信能发到worker内部
//    val toJmmIpc = JmmIpc(
//      portId,
//      this@JsMicroModule,
//      fromMMID,
//      fetchIpc ?: throw CancellationException("ipcBridge abort"),
//      "native-createIpc-${fromMMID}"
//    )
//    return toJmmIpc
    }

    /**桥接两个JsMM*/
    private suspend fun ipcBridgeJsMM(fromMMID: MMID, toMMID: MMID): Boolean {
      return nativeFetch(
        URLBuilder("file://js.browser.dweb/create-ipc").apply {
          parameters["process_id"] = pid
          parameters["from_mmid"] = fromMMID
          parameters["to_mmid"] = toMMID
        }.buildUnsafeString()
      ).boolean()
    }

    private fun ipcBridgeFactory(fromMMID: MMID, targetIpc: Ipc?) =
      fromMMIDOriginIpcWM.getOrPut(fromMMID) {
        return@getOrPut CompletableDeferred<Ipc>().alsoLaunchIn(mmScope) {
          debugJsMM("ipcBridge", "fromMmid:$fromMMID targetIpc:$targetIpc")

          val toJmmIpc = ipcBridgeSelf(fromMMID)
          toJmmIpc.onBeforeClose.listen {
            fromMMIDOriginIpcWM.remove(fromMMID)
          }
          // 如果是jsMM相互连接，直接把port丢过去

          /// 如果传入了 targetIpc，并且当互联当不是jsMM才会启动桥接
          /// 那么启动桥接模式，我们会中转所有的消息给 targetIpc，包括关闭
          /// 那么这个 targetIpc 理论上就可以作为 toJmmIpc 的代理
          if (targetIpc != null) {
            /**
             * 将两个消息通道间接互联，这里targetIpc明确为NativeModule
             */
            toJmmIpc.onMessage.collectIn(mmScope) { ipcMessage ->
              targetIpc.postMessage(ipcMessage)
            }
            targetIpc.onMessage.collectIn(mmScope) { ipcMessage ->
              toJmmIpc.postMessage(ipcMessage)
            }
            /**
             * 监听关闭事件
             */
            mmScope.launch {
              toJmmIpc.awaitClosed()
              fromMMIDOriginIpcWM.remove(targetIpc.remote.mmid)
              targetIpc.close()
            }
            mmScope.launch {
              targetIpc.awaitClosed()
              fromMMIDOriginIpcWM.remove(toJmmIpc.remote.mmid)
              toJmmIpc.close()
            }
          }
          toJmmIpc
        }
      }


    internal suspend fun ipcBridge(fromMMID: MMID, targetIpc: Ipc? = null) = withScope(mmScope) {
      ipcBridgeFactory(fromMMID, targetIpc).await()
    }


    override suspend fun _shutdown() {
      debugJsMM(
        "jsMM_shutdown",
        "$mmid/${this.fetchIpc?.debugId} ipcNumber=>${fromMMIDOriginIpcWM.size}"
      )
      fromMMIDOriginIpcWM.forEach { map ->
        val ipc = map.value.await()
        ipc.close()
      }
      fromMMIDOriginIpcWM.clear()
      fetchIpc?.close()
      fetchIpc = null
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = JmmRuntime(bootstrapContext)

  override val runtime get() = super.runtime as JmmRuntime

  override fun toManifest(): CommonAppManifest {
    return this.metadata.toCommonAppManifest()
  }
}