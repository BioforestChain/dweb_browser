package org.dweb_browser.microservice.core

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.helper.printDebug
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

fun debugNMM(tag: String, msg: Any = "", err: Throwable? = null) = printDebug("DNS", tag, msg, err)

abstract class NativeMicroModule(manifest: MicroModuleManifest) : MicroModule(manifest) {
  constructor(mmid: MMID, name: String) : this(
    MicroModuleManifest().apply {
      this.mmid = mmid
      this.name = name
    }
  )

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
        val routesWithContext = router.withFilter(ipcRequest);
        debugNMM("NMM/Handler", ipcRequest.url)
        val response = routesWithContext?.let {
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

  data class HandlerContext(val request: PureRequest, val ipc: Ipc) {
    fun throwException(
      code: HttpStatusCode = HttpStatusCode.InternalServerError,
      message: String = code.description,
      cause: Throwable? = null
    ): Nothing = throw ResponseException(code, message, cause)
  }

  protected fun defineEmptyResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<Unit>,
  ) = wrapHandler(beforeResponse) {
    handler()
    PureResponse(HttpStatusCode.NoContent)
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
    PureResponse(HttpStatusCode.OK).jsonBody(handler())
  }

  protected fun defineJsonResponse(
    beforeResponse: BeforeResponse? = null,
    handler: RequestHandler<JsonElement>,
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).jsonBody(
      handler()
    )
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
