package org.dweb_browser.core.http.router

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.core.ipc.helper.ReadableStreamOut
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.pure.http.PureBinary
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody

interface HttpHandlerToolkit {

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
      val job = ipc.scope.launch {
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
        ipc.launchJobs += ipc.scope.launch(start = CoroutineStart.UNDISPATCHED) {
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
      val job = ipc.scope.launch {
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
        ipc.launchJobs += ipc.scope.launch(start = CoroutineStart.UNDISPATCHED) {
          doClose()
        }
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


  fun getContextHttpRouter(): HttpRouter? {
    return null
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
      // 自动绑定上下文
      it.ctx = getContextHttpRouter()
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