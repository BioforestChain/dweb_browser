package org.dweb_browser.core.module

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.ReadableStreamOut
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.http.PureBinary
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody

val debugNMM = Debugger("NMM")

abstract class NativeMicroModule(manifest: MicroModuleManifest) : MicroModule(manifest) {
  constructor(mmid: MMID, name: String) : this(MicroModuleManifest().apply {
    this.mmid = mmid
    this.name = name
  })

  companion object {
    private var ipc_acc by SafeInt(1)

    init {
      connectAdapterManager.append { fromMM, toMM, reason ->
        if (toMM is NativeMicroModule) {
          debugNMM("NMM/connectAdapter", "fromMM: ${fromMM.mmid} => toMM: ${toMM.mmid}")
          val channel = NativeMessageChannel(kotlinIpcPool.scope, fromMM.id, toMM.id)
          val acc = ipc_acc++
          val fromNativeIpc = kotlinIpcPool.create(
            "from-native-${fromMM.id}-$acc",
            toMM,
            channel.port1
          )
          val toNativeIpc = kotlinIpcPool.create(
            "to-native-${toMM.id}-$acc",
            fromMM,
            channel.port2
          )
          fromMM.beConnect(fromNativeIpc, reason) // 通知发起连接者作为Client
          toMM.beConnect(toNativeIpc, reason) // 通知接收者作为Server
          return@append ConnectResult(fromNativeIpc, toNativeIpc) // 返回发起者的ipc
        } else null
      }
    }
  }

  override suspend fun getSafeDwebPermissionProviders() = this.dweb_permissions.mapNotNull {
    PermissionProvider.from(this, it)
  }

  private val protocolRouters = mutableMapOf<DWEB_PROTOCOL, MutableList<HttpRouter>>()
  private fun getProtocolRouters(protocol: DWEB_PROTOCOL) =
    protocolRouters.getOrPut(protocol) { mutableListOf() }

  suspend fun routes(vararg list: RouteHandler) = HttpRouter(this, this.mmid).also {
    it.addRoutes(*list)
  }.also { addRouter(it) }

  fun addRouter(router: HttpRouter) {
    getProtocolRouters("*") += router
  }

  fun removeRouter(router: HttpRouter) {
    getProtocolRouters("*") -= router
  }

  class ProtocolBuilderContext(val mm: MicroModule, val host: String) {
    internal val router = HttpRouter(mm, host)
    suspend fun routes(vararg list: RouteHandler) = router.apply { addRoutes(*list) }

    val onConnect = mm.onConnect
  }

  suspend fun protocol(
    protocol: DWEB_PROTOCOL, buildProtocol: suspend ProtocolBuilderContext.() -> Unit
  ) {
    val context = ProtocolBuilderContext(this, protocol)
    context.buildProtocol()
    getProtocolRouters(protocol) += context.router
  }

  override suspend fun afterShutdown() {
    super.afterShutdown()
    protocolRouters.clear()
  }

  /**
   * 实现一整套简易的路由响应规则
   */
  override suspend fun beforeBootstrap(bootstrapContext: BootstrapContext) =
    super.beforeBootstrap(bootstrapContext).trueAlso {
      onConnect { (clientIpc) ->
        clientIpc.requestFlow.onEach { (ipcRequest) ->
          debugNMM("NMM/Handler", ipcRequest.url)
          /// 根据host找到对应的路由模块
          val routers = protocolRouters[ipcRequest.uri.host] ?: protocolRouters["*"]
          var response: PureResponse? = null
          if (routers != null) for (router in routers) {
            val pureRequest = ipcRequest.toPure()
            val res = router.withFilter(pureRequest)?.invoke(HandlerContext(pureRequest, clientIpc))
            if (res != null) {
              response = res
              break
            }
          }

          clientIpc.postMessage(
            IpcResponse.fromResponse(
              ipcRequest.reqId, response ?: PureResponse(HttpStatusCode.BadGateway), clientIpc
            )
          )
        }.launchIn(ioAsyncScope)
      }
    }

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

  fun defineJsonLineResponse(
    middlewareHttpHandler: MiddlewareHttpHandler? = null,
    handler: suspend JsonLineHandlerContext.() -> Unit,
  ) = wrapHandler(middlewareHttpHandler) {
    JsonLineHandlerContext(this).run {
      // 执行分发器
      val job = ioAsyncScope.launch {
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
      val closeListen = ioAsyncScope.launch {
        ipc.closeDeferred.await()
        doClose()
      }
      // 监听 job 完成，释放相关的监听
      job.invokeOnCompletion { closeListen.cancel() }
      // 返回响应流
      PureResponse.build { body(responseReadableStream.stream.stream) }
    }
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

  fun defineCborPackageResponse(
    middlewareHttpHandler: MiddlewareHttpHandler? = null,
    handler: suspend CborPacketHandlerContext.() -> Unit,
  ) = wrapHandler(middlewareHttpHandler) {
    CborPacketHandlerContext(this).run {
      // 执行分发器
      val job = ioAsyncScope.launch {
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
      val closeListen = ioAsyncScope.launch {
        ipc.closeDeferred.await()
        doClose()
      }
      // 监听 job 完成，释放相关的监听
      job.invokeOnCompletion { closeListen.cancel() }
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

/**
 * 创建一个 channel 通信
 */
suspend fun NativeMicroModule.createChannel(
  urlPath: String, resolve: suspend PureChannelContext.() -> Unit
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