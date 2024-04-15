package org.dweb_browser.core.module

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.core.help.types.DWEB_PROTOCOL
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.http.router.HandlerContext
import org.dweb_browser.core.http.router.HttpHandler
import org.dweb_browser.core.http.router.HttpHandlerChain
import org.dweb_browser.core.http.router.HttpRouter
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.MiddlewareHttpHandler
import org.dweb_browser.core.http.router.RouteHandler
import org.dweb_browser.core.http.router.TypedHttpHandler
import org.dweb_browser.core.http.router.toChain
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.ReadableStreamOut
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.core.std.permission.ext.requestPermissions
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.listen
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.pure.http.PureBinary
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.buildRequestX


abstract class NativeMicroModule(manifest: MicroModuleManifest) : MicroModule(manifest) {
  override fun toString(): String {
    return "NMM($mmid)"
  }

  constructor(mmid: MMID, name: String) : this(MicroModuleManifest().apply {
    this.mmid = mmid
    this.name = name
  })

  companion object {
    init {
      connectAdapterManager.append { fromMM, toMM, reason ->
        if (toMM is NativeMicroModule.NativeRuntime) {
          fromMM.debugMM("NMM/connectAdapter", "fromMM: ${fromMM.mmid} => toMM: ${toMM.mmid}")
          val channel = NativeMessageChannel(kotlinIpcPool.scope, fromMM.id, toMM.id)
          val pid = 0
          val fromNativeIpc = kotlinIpcPool.createIpc(channel.port1, pid, fromMM, toMM)
          val toNativeIpc = kotlinIpcPool.createIpc(channel.port2, pid, toMM, fromMM)
          // fromMM.beConnect(fromNativeIpc, reason) // 通知发起连接者作为Client
          toMM.beConnect(toNativeIpc, reason) // 通知接收者作为Server
          fromNativeIpc
        } else null
      }

      /**
       * 对全局的自定义路由提供适配器
       * 对 nativeFetch 定义 file://xxx.dweb的解析
       */
      nativeFetchAdaptersManager.append { fromMM, request ->
        if (request.url.protocol.name == "file" && request.url.host.endsWith(".dweb")) {
          val mpid = request.url.host
          fromMM.debugMM("fetch ipc", "$fromMM => ${request.href}")
          val url = request.href
          val reasonRequest = buildRequestX(url, request.method, request.headers, request.body);
          val fromIpc = runCatching {
            fromMM.connect(mpid, reasonRequest)
          }.getOrElse {
            return@append PureResponse(HttpStatusCode.BadGateway, body = PureStringBody(url))
          }
          var response = fromIpc.request(request)
          if (response.status == HttpStatusCode.Unauthorized) {
            val permissions = response.body.toPureString()
            /// 尝试进行授权请求
            if (fromMM is NativeMicroModule.NativeRuntime && fromMM.requestPermissions(permissions)
                .all { it.value == AuthorizationStatus.GRANTED }
            ) {
              /// 如果授权完全成功，那么重新进行请求
              response = fromIpc.request(request)
            }
          }
          response
        } else null
      }
    }
  }

  override suspend fun getSafeDwebPermissionProviders() = dweb_permissions.mapNotNull {
    PermissionProvider.from(this, it)
  }


  abstract inner class NativeRuntime : Runtime() {
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

    /**
     * 实现一整套简易的路由响应规则
     */
    init {
      debugMM("onConnect", "start")
      onConnect.listen { connectEvent ->
        val (clientIpc) = connectEvent.consume()
        debugMM("onConnect", clientIpc)
        clientIpc.onRequest("file-dweb-router").collectIn(mmScope) { event ->
          val ipcRequest = event.consumeFilter {
            when (it.uri.protocol.name) {
              "file", "dweb", "https" -> true
              else -> false
            }
          } ?: return@collectIn
          debugMM("NMM/Handler", ipcRequest.url)
          /// 根据host找到对应的路由模块
          val routers = protocolRouters[ipcRequest.uri.host] ?: protocolRouters["*"]
          var response: PureResponse? = null
//          println("QAQ routes=$routers")
          if (routers != null) for (router in routers) {
//            println("QAQ ipcRequest=$ipcRequest")
            val pureRequest = ipcRequest.toPure()
//            println("QAQ pureRequest=$pureRequest")
            val res =
              router.withFilter(pureRequest)?.invoke(HandlerContext(pureRequest, clientIpc))
//            println("QAQ response=$response")
            if (res != null) {
              response = res
              break
            }
          }

          clientIpc.postResponse(
            ipcRequest.reqId, response ?: PureResponse(HttpStatusCode.BadGateway)
          )
//          println("QAQ postResponse=${ipcRequest.reqId}")
        }

        /// 在 NMM 这里，只要绑定好了，就可以开始握手通讯
        clientIpc.start(await = false, reason = "on-connect")
      }
    }

    fun IHandlerContext.getRemoteRuntime() = (bootstrapContext.dns.query(ipc.remote.mmid)
      ?: throw IllegalArgumentException("no found microModule ${ipc.remote.mmid}")).runtime

    fun defineEmptyResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<Unit>,
    ) = wrapHandler(middlewareHttpHandler) {
      handler()
      PureResponse(HttpStatusCode.OK)
    }

    fun defineStringResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<String>,
    ) = wrapHandler(middlewareHttpHandler) {
      PureResponse.build { body(handler()) }
    }

    fun defineNumberResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<Number>,
    ) = wrapHandler(middlewareHttpHandler) {
      PureResponse.build {
        jsonBody(handler())
      }
    }

    fun defineBooleanResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<Boolean>,
    ) = wrapHandler(middlewareHttpHandler) {
      PureResponse.build {
        jsonBody(
          try {
            handler()
          } catch (e: Throwable) {
            e.printStackTrace()
            false
          }
        )
      }
    }

    fun defineJsonResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<JsonElement>,
    ) = wrapHandler(middlewareHttpHandler) {
      PureResponse.build {
        jsonBody(handler())
      }
    }


    fun defineJsonLineResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: suspend JsonLineHandlerContext.() -> Unit,
    ) = wrapHandler(middlewareHttpHandler) {
      JsonLineHandlerContext(this).run {
        // 执行分发器
        val job = scopeLaunch(cancelable = true) {
          try {
            handler()
          } catch (e: Throwable) {
            e.printStackTrace()
            end(reason = e)
          }
        }
        val doClose = suspend {
          if (job.isActive) {
            job.cancel(CancellationException("ipc closed"))
            end()
          }
        }
        // 监听 response 流关闭，这可能发生在网页刷新
        responseReadableStream.controller.awaitClose {
          onDisposeSignal.emit()
          doClose()
        }
        // 监听 ipc 关闭，这可能由程序自己控制
        ipc.onClosed {
          scopeLaunch(cancelable = false) {
            doClose()
          }
        }
        // 返回响应流
        PureResponse.build { body(responseReadableStream.stream.stream) }
      }
    }


    fun defineCborPackageResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: suspend CborPacketHandlerContext.() -> Unit,
    ) = wrapHandler(middlewareHttpHandler) {
      CborPacketHandlerContext(this).run {
        // 执行分发器
        val job = scopeLaunch(cancelable = false) {
          try {
            handler()
          } catch (e: Throwable) {
            e.printStackTrace()
            end(reason = e)
          }
        }

        val doClose = {
          if (job.isActive) {
            job.cancel(CancellationException("ipc closed"))
            end()
          }
        }
        // 监听 response 流关闭，这可能发生在网页刷新
        responseReadableStream.controller.awaitClose {
          onDisposeSignal.emit()
          doClose()
        }
        // 监听 ipc 关闭，这可能由程序自己控制
        ipc.onClosed {
          doClose()
        }
        // 返回响应流
        PureResponse.build { body(responseReadableStream.stream.stream) }
      }
    }

    fun definePureResponse(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<PureResponse>,
    ) = wrapHandler(middlewareHttpHandler) {
      handler()
    }

    fun definePureBinaryHandler(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<PureBinary>,
    ) = wrapHandler(middlewareHttpHandler) {
      PureResponse(body = PureBinaryBody(handler()))
    }

    fun definePureStreamHandler(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<PureStream>,
    ) = wrapHandler(middlewareHttpHandler) {
      PureResponse(body = PureStreamBody(handler()))
    }

    private fun wrapHandler(
      middlewareHttpHandler: MiddlewareHttpHandler? = null,
      handler: TypedHttpHandler<PureResponse?>,
    ): HttpHandlerChain {

      val httpHandler: HttpHandler = {
        handler() ?: PureResponse(HttpStatusCode.NotImplemented)
      }
      return httpHandler.toChain().also {
        if (middlewareHttpHandler != null) {
          it.use(middlewareHttpHandler)
        }
      }
    }
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

    fun end(reason: Throwable? = null) {
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
suspend fun NativeMicroModule.NativeRuntime.createChannel(
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