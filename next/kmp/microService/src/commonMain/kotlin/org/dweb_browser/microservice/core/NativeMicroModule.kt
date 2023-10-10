package org.dweb_browser.microservice.core

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.help.types.MicroModuleManifest
import org.dweb_browser.microservice.http.HttpRouter
import org.dweb_browser.microservice.http.PureBinary
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStream
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.http.RoutingHttpHandler
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.NativeIpc
import org.dweb_browser.microservice.ipc.NativeMessageChannel
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStreamOut

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

  protected val router = HttpRouter()
  fun routes(vararg list: RoutingHttpHandler) = router.apply { addRoutes(*list) }

  /**
   * 实现一整套简易的路由响应规则
   */
  init {
    onConnect { (clientIpc) ->
      clientIpc.onRequest { (ipcRequest) ->
        debugNMM("NMM/Handler", ipcRequest.url)
        val routingHandler = router.withFilter(ipcRequest);
        val response = routingHandler?.let {
          it(HandlerContext(ipcRequest.toRequest(), clientIpc))
        } ?: PureResponse(HttpStatusCode.BadGateway)

        clientIpc.postMessage(
          IpcResponse.fromResponse(
            ipcRequest.req_id, response, clientIpc
          )
        )
      }
    }
  }

  interface IHandlerContext {
    val request: PureRequest
    val ipc: Ipc
    fun throwException(
      code: HttpStatusCode = HttpStatusCode.InternalServerError,
      message: String = code.description,
      cause: Throwable? = null
    ): Nothing = throw ResponseException(code, message, cause)
  }

  open class HandlerContext(override val request: PureRequest, override val ipc: Ipc) :
    IHandlerContext

  protected fun defineEmptyResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<Unit>,
  ) = wrapHandler(beforeResponse) {
    handler()
    PureResponse(HttpStatusCode.OK)
  }

  protected fun defineStringResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<String>,
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).body(
      handler()
    )
  }

  protected fun defineNumberResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<Number>,
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).jsonBody(
      handler()
    )
  }

  protected fun defineBooleanResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<Boolean>,
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).jsonBody(
      try {
        handler()
      } catch (e: Throwable) {
        e.printStackTrace()
        false
      }
    )
  }

  protected fun defineJsonResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<JsonElement>,
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).jsonBody(
      handler()
    )
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

  protected fun defineJsonLineResponse(
    beforeResponse: BeforeResponse? = null,
    handler: suspend JsonLineHandlerContext.() -> Unit,
  ) = wrapHandler(beforeResponse) {
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
      PureResponse(HttpStatusCode.OK).body(responseReadableStream.stream.stream)
    }
  }

  fun PureResponse.body(body: JsonElement) = jsonBody(body)

  protected fun definePureResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<PureResponse>,
  ) = wrapHandler(beforeResponse) {
    handler()
  }

  protected fun definePureBinaryHandler(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<PureBinary>,
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).body(
      handler()
    )
  }

  protected fun definePureStreamHandler(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<PureStream>,
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).body(handler())
  }

  private fun wrapHandler(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<PureResponse?>,
  ): suspend (HandlerContext) -> PureResponse = { context: HandlerContext ->
    try {
      handler(context)?.let { response ->
        beforeResponse?.invoke(response) ?: response
      } ?: PureResponse(HttpStatusCode.NotImplemented)
    } catch (ex: Exception) {
      debugNMM("NMM/Error", context.request.href, ex)
      PureResponse(
        HttpStatusCode.InternalServerError, body = PureStringBody(
          """
          <p>${context.request.href}</p>
          <pre>${ex.message ?: "Unknown Error"}</pre>
        """.trimIndent()
        )
      )
    }
  }

  class ResponseException(
    val code: HttpStatusCode = HttpStatusCode.InternalServerError,
    message: String = code.description,
    cause: Throwable? = null
  ) : Exception(message, cause)
}

typealias BeforeResponse = suspend (PureResponse) -> PureResponse?
typealias RequestHandler<T> = suspend NativeMicroModule.HandlerContext.() -> T
typealias HttpHandler = suspend (NativeMicroModule.HandlerContext) -> PureResponse

@Serializable
data class DwebResult(val success: Boolean, val message: String = "")
