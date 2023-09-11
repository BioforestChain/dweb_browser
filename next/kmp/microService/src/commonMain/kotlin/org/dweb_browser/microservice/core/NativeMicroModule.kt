package org.dweb_browser.microservice.core

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.ApplicationResponse
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.help.types.MicroModuleManifest
import org.dweb_browser.microservice.http.PureByteArrayBody
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.PureUtf8StringBody
import org.dweb_browser.microservice.http.router
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.NativeIpc
import org.dweb_browser.microservice.ipc.NativeMessageChannel
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import kotlin.reflect.KClass

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

//  var apiRouting: RoutingHttpHandler? = null


//  private val requestContexts = RequestContexts()
//  private val requestContextKey_ipc = RequestContextKey.required<Ipc>(requestContexts)
//  private val ipcApiFilter = ServerFilters.InitialiseRequestContext(requestContexts)

  /**
   * 实现一整套简易的路由响应规则
   */
  init {
    onConnect { (clientIpc) ->
      clientIpc.onRequest { (ipcRequest) ->
        val routesWithContext = router.withFilter(ipcRequest);
        debugNMM("NMM/Handler", ipcRequest.url)
        val request = ipcRequest.toRequest()
        val response = routesWithContext?.let { it(request, clientIpc) }

        if(response != null) {
          clientIpc.postMessage(
            IpcResponse.fromResponse(
              ipcRequest.req_id, response, clientIpc
            )
          )
        }
      }
    }
  }

  class ResponseRegistry {
    companion object {
      private val regMap = mutableMapOf<KClass<*>, (item: Any) -> PureResponse>()

      fun <T : Any> registryResponse(type: KClass<T>, handler: (item: T) -> PureResponse) {
        regMap[type] = handler as (item: Any) -> PureResponse
      }

      init {
        registryResponse(ByteArray::class) {
          PureResponse(HttpStatusCode.OK, body = PureByteArrayBody(it))
        }
        registryResponse(ByteReadPacket::class) {
          PureResponse(HttpStatusCode.OK, body = PureStreamBody(it))
        }
      }

      fun <T : Any> registryJsonAble(type: KClass<T>, handler: (item: T) -> Any) {
        registryResponse(type) {
          asJson(handler(it))
        }
      }

      fun handle(result: Any): PureResponse {
        val kClass = result::class

        return when (val handler = regMap[kClass]) {
          null -> {
//            var superJavaClass = kClass.
//            while (superJavaClass != null) {
//              // 尝试寻找继承关系
//              when (val handler = regMap[superJavaClass]) {
//                null -> superJavaClass = superJavaClass.superclass
//                else -> return handler(result)
//              }
//            }
            // 否则默认当成JSON来返回
            return asJson(result)
          }
          // 如果有注册处理函数，那么交给处理函数进行处理
          else -> handler(result)
        }
      }

      fun asJson(result: Any) =
        PureResponse(
          HttpStatusCode.OK,
          headers = IpcHeaders().also { it.init("Content-Type", "application/json") },
          body = PureUtf8StringBody(Json.encodeToString(result))
        )
    }
  }

  data class HandlerContext(val request: PureRequest, private val getIpc: RequestContextLens<Ipc>) {
    val ipc get() = getIpc(request)
    fun throwException(
      code: HttpStatusCode = HttpStatusCode.InternalServerError,
      message: String = code.description,
      cause: Throwable? = null
    ): Nothing = throw ResponseException(code, message, cause)
  }

  protected fun defineEmptyResponse(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend HandlerContext.(request: PureRequest) -> Unit,
  ) = wrapHandler(beforeResponse) {
    HandlerContext(it, requestContextKey_ipc).handler(it)
    PureResponse(HttpStatusCode.NoContent)
  }

  protected fun defineStringResponse(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend HandlerContext.(request: PureRequest) -> String
  ) = wrapHandler(beforeResponse) { request ->
    PureResponse(HttpStatusCode.OK).body(HandlerContext(request, requestContextKey_ipc).handler(request))
  }

  protected fun defineBooleanResponse(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend HandlerContext.(request: PureRequest) -> Boolean
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).jsonBody(HandlerContext(it, requestContextKey_ipc).handler(it))
  }

  protected fun defineJsonResponse(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend HandlerContext.(request: PureRequest) -> JsonElement
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).jsonBody(
      HandlerContext(
        it, requestContextKey_ipc
      ).handler(it)
    )
  }

  fun PureResponse.body(body: JsonElement) = jsonBody(body)

  protected fun defineResponse(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend HandlerContext.(request: PureRequest) -> PureResponse
  ) = wrapHandler(beforeResponse) {
    HandlerContext(it, requestContextKey_ipc).handler(it)
  }

  protected fun defineByteArrayHandler(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend HandlerContext.(request: PureRequest) -> ByteArray
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).body(MemoryBody(HandlerContext(it, requestContextKey_ipc).handler(it)))
  }

  protected fun defineInputStreamHandler(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend HandlerContext.(request: PureRequest) -> ByteReadPacket
  ) = wrapHandler(beforeResponse) {
    PureResponse(HttpStatusCode.OK).body(HandlerContext(it, requestContextKey_ipc).handler(it))
  }

  private fun wrapHandler(
    beforeResponse: (PureResponse.() -> PureResponse)? = null,
    handler: suspend (request: PureRequest) -> PureResponse?,
  ) = { request: PureRequest ->
    runBlockingCatching {
      handler(request)?.let { response ->
        if (beforeResponse != null) {
          response.beforeResponse()
        } else response
      } ?: PureResponse(HttpStatusCode.NotImplemented)
    }.getOrElse { ex ->
      debugNMM("NMM/Error", request.url, ex)
      PureResponse(HttpStatusCode.InternalServerError, body = PureUtf8StringBody("""
          <p>${request.url}</p>
          <pre>${ex.message ?: "Unknown Error"}</pre>
        """.trimIndent()))
    }
  }


  protected fun defineHandler(handler: suspend (request: PureRequest, ipc: Ipc) -> Any?) = { request: PureRequest ->
    runBlockingCatching {
      when (val result = handler(request)) {
        null, Unit -> {
          PureResponse(HttpStatusCode.OK)
        }

        is PureResponse -> result
        is ByteArray -> PureResponse(HttpStatusCode.OK, body = PureByteArrayBody(result))
        is ByteReadPacket -> PureResponse(HttpStatusCode.OK, body = PureStreamBody(result))
        else -> {
          // 如果有注册处理函数，那么交给处理函数进行处理
          ResponseRegistry.handle(result)
        }
      }
    }.getOrElse { ex ->
      debugNMM("NMM/Error", request.url, ex)
      val content = """
          <p>${request.url}</p>
          <pre>${ex.message ?: "Unknown Error"}</pre>
        """.trimIndent();
      when (ex) {
        is ResponseException -> PureResponse(ex.code, body = PureUtf8StringBody(content))
        else -> PureResponse(HttpStatusCode.InternalServerError, body = PureUtf8StringBody(content))
      }
    }
  }

  protected fun defineHandler(handler: suspend (request: PureRequest, ipc: Ipc) -> Any?) =
    defineHandler { request ->
      handler(request, ipc)
    }

  class ResponseException(
    val code: HttpStatusCode = HttpStatusCode.InternalServerError,
    message: String = code.description,
    cause: Throwable? = null
  ) : Exception(message, cause)
}
