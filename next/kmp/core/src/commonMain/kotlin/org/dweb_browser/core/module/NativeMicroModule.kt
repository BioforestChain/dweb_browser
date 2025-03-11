package org.dweb_browser.core.module

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.core.help.types.DWEB_PROTOCOL
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.http.router.HandlerContext
import org.dweb_browser.core.http.router.HttpHandlerToolkit
import org.dweb_browser.core.http.router.HttpRouter
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.RouteHandler
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IpcServerRequest
import org.dweb_browser.core.ipc.helper.ReadableStreamOut
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.core.std.permission.ext.doRequestWithPermissions
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.listen
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.buildRequestX


abstract class NativeMicroModule(manifest: MicroModuleManifest) : MicroModule(manifest) {
  override fun toString(): String {
    return "NMM($mmid)"
  }

  constructor(mmid: MMID, name: String) : this(MicroModuleManifest().apply {
    this.mmid = mmid
    this.name = name
    targetType = "nmm"
  })

  companion object {

    init {
      connectAdapterManager.append { fromMM, toMM, reason ->
        if (toMM is NativeMicroModule.NativeRuntime) {
          fromMM.debugMM("NMM/connectAdapter", "fromMM: ${fromMM.mmid} => toMM: ${toMM.mmid}")
          val channel = NativeMessageChannel(kotlinIpcPool.scope, fromMM.id, toMM.id)
          val fromNativeIpc = kotlinIpcPool.createIpc(
            endpoint = channel.port1,
            pid = 0,
            locale = fromMM.manifest,
            remote = toMM.microModule.manifest,
            startReason = "NMM-connectAdapter"
          )
          val toNativeIpc = kotlinIpcPool.createIpc(
            endpoint = channel.port2,
            pid = 0,
            locale = toMM.microModule.manifest,
            remote = fromMM.manifest,
            startReason = "NMM-connectAdapter"
          )
          // fromMM.beConnect(fromNativeIpc, reason) // 通知发起连接者作为Client
          toMM.beConnect(toNativeIpc, reason) // 通知接收者作为Server
          fromNativeIpc
        } else null
      }

      /**
       * 对全局的自定义路由提供适配器
       * 对 nativeFetch 定义 file://xxx.dweb的解析
       */
      nativeFetchAdaptersManager.append(order = 1) { fromMM, request ->
        if (request.url.protocol.name == "file" && request.url.host.endsWith(".dweb")) {
          val mpid = request.url.host
          fromMM.debugMM("fetch ipc") { "${fromMM.mmid} => ${request.href}" }
          val url = request.href
          val reasonRequest = buildRequestX(url, request.method, request.headers, request.body);
          val fromIpc = runCatching {
            fromMM.connect(mpid, reasonRequest)
          }.getOrElse {
            return@append PureResponse(HttpStatusCode.BadGateway, body = PureStringBody(url))
          }
          fromMM.doRequestWithPermissions { fromIpc.request(request) }
        } else null
      }
    }
  }

  override suspend fun getSafeDwebPermissionProviders() = dweb_permissions.mapNotNull {
    PermissionProvider.from(this, it)
  }


  abstract inner class NativeRuntime : Runtime(), HttpHandlerToolkit {
    private val protocolRouters = mutableMapOf<DWEB_PROTOCOL, MutableList<HttpRouter>>()
    private fun getProtocolRouters(protocol: DWEB_PROTOCOL) =
      protocolRouters.getOrPut(protocol) { mutableListOf() }

    suspend fun routes(vararg list: RouteHandler) = HttpRouter(this, mmid).also {
      it.addRoutes(*list)
    }.also { addRouter(it) }

    fun addRouter(router: HttpRouter) {
      getProtocolRouters("*") += router
    }

    fun removeRouter(router: HttpRouter) {
      getProtocolRouters("*") -= router
    }

    inner class ProtocolBuilderContext(val host: String) {
      internal val router = HttpRouter(this@NativeRuntime, host)
      suspend fun routes(vararg list: RouteHandler) = router.apply { addRoutes(*list) }

      val onConnect get() = this@NativeRuntime::onConnect
    }

    suspend fun protocol(
      protocol: DWEB_PROTOCOL, buildProtocol: suspend ProtocolBuilderContext.() -> Unit,
    ) {
      val context = ProtocolBuilderContext(protocol)
      context.buildProtocol()
      getProtocolRouters(protocol) += context.router
    }

    protected var routesCheckAllowHttp = { _: IpcServerRequest -> false }
    protected var routesCheckAllowDweb = { _: IpcServerRequest -> true }
    protected fun HandlerContext.defaultRoutesNotFound(error: Throwable? = null) =
      PureResponse.build {
        if (error != null) {
          status(HttpStatusCode.InternalServerError)
          body(error.stackTraceToString())
        } else {
          status(HttpStatusCode.NotFound)
          body(request.href)
        }
      }

    protected var routesNotFound: suspend HandlerContext.() -> PureResponse = {
      defaultRoutesNotFound(null)
    }

    /**
     * 实现一整套简易的路由响应规则
     */
    init {
      debugMM("onConnect", "start")
      onConnect.listen { connectEvent ->
        val (clientIpc) = connectEvent.consume()
        debugMM("onConnect-start", clientIpc)
        clientIpc.onRequest("routes").collectIn(mmScope) { event ->
          val ipcRequest = event.consumeFilter {
            when (it.uri.protocol.name) {
              "file", "dweb" -> routesCheckAllowDweb(it)
              "http", "https" -> routesCheckAllowHttp(it)
              else -> false
            }
          } ?: return@collectIn
          mmScope.launch(start = CoroutineStart.UNDISPATCHED) {
            debugMM("NMM/Handler-start") { ipcRequest.url }
            /// 根据host找到对应的路由模块
            val routers = protocolRouters[ipcRequest.uri.host] ?: protocolRouters["*"]
            val request = ipcRequest.toPure()
            val ctx = HandlerContext(request, clientIpc)
            var response: PureResponse? = null
            if (!routers.isNullOrEmpty()) {
              for (router in routers) {
                val res = router.withFilter(request)?.invoke(ctx)
                if (res != null) {
                  response = res
                  break
                }
              }
            }

            clientIpc.postResponse(ipcRequest.reqId, response ?: runCatching {
              routesNotFound(ctx)
            }.getOrElse {
              ctx.defaultRoutesNotFound(it)
            })
            debugMM("NMM/Handler-done") { ipcRequest.url }
          }
        }

        /// 在 NMM 这里，只要绑定好了，就可以开始握手通讯
        clientIpc.start(await = false, reason = "on-connect")
        debugMM("onConnect-end", clientIpc)
      }
    }

    suspend fun IHandlerContext.getRemoteRuntime() = (bootstrapContext.dns.query(ipc.remote.mmid)
      ?: throw IllegalArgumentException("no found microModule ${ipc.remote.mmid}")).runtime

  }

  class JsonLineHandlerContext constructor(context: HandlerContext) : IHandlerContext by context {
    internal val responseReadableStream = ReadableStreamOut(context.ipc.scope)
    suspend fun emit(line: JsonElement) {
      responseReadableStream.controller.enqueue((Json.encodeToString(line) + "\n").toByteArray())
    }

    suspend inline fun <reified T> emit(lineData: T) = emit(lineData.toJsonElement())

    suspend fun end(reason: Throwable? = null) {
      if (reason != null) {
        responseReadableStream.controller.closeWrite(reason)
      } else {
        responseReadableStream.controller.closeWrite()
      }
    }

    internal val onDisposeSignal = SimpleSignal()

    val onDispose = onDisposeSignal.toListener()
  }

  class CborPacketHandlerContext(context: HandlerContext) : IHandlerContext by context {
    internal val responseReadableStream = ReadableStreamOut(context.ipc.scope)
    suspend fun emit(data: ByteArray) {
      responseReadableStream.controller.enqueue(data.size.toLittleEndianByteArray(), data)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend inline fun <reified T> emit(lineData: T) = emit(Cbor.encodeToByteArray(lineData))

    suspend fun end(reason: Throwable? = null) {
      if (reason != null) {
        responseReadableStream.controller.closeWrite(reason)
      } else {
        responseReadableStream.controller.closeWrite()
      }
    }

    internal val onDisposeSignal = SimpleSignal()

    val onDispose = onDisposeSignal.toListener()
  }
}

/**
 * 创建一个 channel 通信
 */
suspend fun NativeMicroModule.NativeRuntime.channelRequest(
  urlPath: String, resolve: suspend PureChannelContext.() -> Unit,
): PureResponse {
  val channelDef = CompletableDeferred<PureChannel>()
  val request = PureClientRequest(
    urlPath, PureMethod.GET, channel = channelDef
  )
  val channel = PureChannel(from = request).also { channelDef.complete(it) }
  val res = nativeFetch(request)
  if (res.isOk) {
    channel.start().resolve()
  }
  return res
}