package org.dweb_browser.core.module

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.core.help.types.DWEB_PROTOCOL
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.http.PureBinary
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.http.router.HandlerContext
import org.dweb_browser.core.http.router.HttpHandler
import org.dweb_browser.core.http.router.HttpHandlerChain
import org.dweb_browser.core.http.router.HttpRouter
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.MiddlewareHttpHandler
import org.dweb_browser.core.http.router.RoutingHttpHandler
import org.dweb_browser.core.http.router.TypedHttpHandler
import org.dweb_browser.core.http.router.toChain
import org.dweb_browser.core.ipc.NativeIpc
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.ReadableStreamOut
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.toLittleEndianByteArray

val debugNMM = Debugger("NMM")

abstract class NativeMicroModule(manifest: MicroModuleManifest) : MicroModule(manifest) {
  constructor(mmid: MMID, name: String) : this(MicroModuleManifest().apply {
    this.mmid = mmid
    this.name = name
  })

  companion object {
    init {
      connectAdapterManager.append { fromMM, toMM, reason ->
        if (toMM is NativeMicroModule) {
          debugNMM("NMM/connectAdapter", "fromMM: ${fromMM.mmid} => toMM: ${toMM.mmid}")
          val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
          val toNativeIpc = NativeIpc(channel.port1, fromMM, IPC_ROLE.SERVER);
          val fromNativeIpc = NativeIpc(channel.port2, toMM, IPC_ROLE.CLIENT);
          fromMM.beConnect(fromNativeIpc, reason) // 通知发起连接者作为Client
          toMM.beConnect(toNativeIpc, reason) // 通知接收者作为Server
          return@append ConnectResult(fromNativeIpc, toNativeIpc) // 返回发起者的ipc
        } else null
      }
    }
  }

  private val protocolRouters = mutableMapOf<DWEB_PROTOCOL, MutableList<HttpRouter>>()
  private fun getProtocolRouters(protocol: DWEB_PROTOCOL) =
    protocolRouters.getOrPut(protocol) { mutableListOf() }

  fun routes(vararg list: RoutingHttpHandler) = HttpRouter(this).also { it ->
    it.addRoutes(*list)
    getProtocolRouters("*") += it
  }

  fun removeRouter(router: HttpRouter) {
    getProtocolRouters("*") -= router
  }

  class ProtocolBuilderContext(mm: MicroModule) {
    internal val router = HttpRouter(mm)
    fun routes(vararg list: RoutingHttpHandler) = router.apply { addRoutes(*list) }
  }

  suspend fun protocol(
    protocol: DWEB_PROTOCOL, buildProtocol: suspend ProtocolBuilderContext.() -> Unit
  ) {
    val context = ProtocolBuilderContext(this)
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
  override suspend fun beforeBootstrap(bootstrapContext: BootstrapContext) {
    super.beforeBootstrap(bootstrapContext)
    onConnect { (clientIpc) ->
      clientIpc.onRequest { (ipcRequest) ->
        debugNMM("NMM/Handler", ipcRequest.url)
        /// 根据host找到对应的路由模块
        val routers = protocolRouters[ipcRequest.uri.host] ?: protocolRouters["*"]
        var response: PureResponse? = null
        if (routers != null) for (router in routers) {
          val res = router.withFilter(ipcRequest)
            ?.invoke(HandlerContext(ipcRequest.toRequest(), clientIpc));
          if (res != null) {
            response = res
            break
          }
        }

        clientIpc.postMessage(
          IpcResponse.fromResponse(
            ipcRequest.req_id, response ?: PureResponse(HttpStatusCode.BadGateway), clientIpc
          )
        )
      }
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
    internal val responseReadableStream = ReadableStreamOut()
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
      val off = ipc.onClose { doClose() }
      // 监听 job 完成，释放相关的监听
      job.invokeOnCompletion { off() }
      // 返回响应流
      PureResponse.build { body(responseReadableStream.stream.stream) }
    }
  }


  class CborPacketHandlerContext constructor(context: HandlerContext) : IHandlerContext by context {
    internal val responseReadableStream = ReadableStreamOut()
    suspend fun emit(data: ByteArray) {
      responseReadableStream.controller.enqueue(data.size.toLittleEndianByteArray(), data)
    }

    @kotlinx.serialization.ExperimentalSerializationApi
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
      val off = ipc.onClose { doClose() }
      // 监听 job 完成，释放相关的监听
      job.invokeOnCompletion { off() }
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
    PureResponse.build {
      body(
        handler()
      )
    }
  }

  fun definePureStreamHandler(
    middlewareHttpHandler: MiddlewareHttpHandler? = null,
    handler: TypedHttpHandler<PureStream>,
  ) = wrapHandler(middlewareHttpHandler) {
    PureResponse.build {
      body(handler())
    }
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
    };
  }

}


@Serializable
data class DwebResult(val success: Boolean, val message: String = "")
