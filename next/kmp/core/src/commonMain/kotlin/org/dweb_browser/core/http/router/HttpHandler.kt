package org.dweb_browser.core.http.router

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.http.IPureChannel
import org.dweb_browser.core.http.PureChannelContext
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.ipc.Ipc

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
typealias MiddlewareHttpHandler = suspend HandlerContext.(next: HttpHandler) -> PureResponse
typealias HttpHandler = suspend HandlerContext.() -> PureResponse
typealias TypedHttpHandler<T> = suspend HandlerContext.() -> T

fun HttpHandler.toChain() = HttpHandlerChain(this)
class HttpHandlerChain(val handler: HttpHandler) {
  private var middlewares: MutableList<MiddlewareHttpHandler>? = null
  fun use(middleware: MiddlewareHttpHandler, force: Boolean = false): Boolean {
    if (!force && hasUse(middleware)) {
      return false
    }
    return (middlewares ?: mutableListOf<MiddlewareHttpHandler>().also { middlewares = it }).add(
      middleware
    )
  }

  fun hasUse(after: MiddlewareHttpHandler) = middlewares?.contains(after) ?: false

  private suspend fun HandlerContext.doHandler(
    ite: Iterator<MiddlewareHttpHandler>, handler: HttpHandler
  ): PureResponse = if (ite.hasNext()) {
    val middleware = ite.next()
    middleware {
      doHandler(ite, handler)
    }
  } else handler()

  suspend operator fun invoke(ctx: HandlerContext): PureResponse {
    return try {
      when (val ms = middlewares) {
        null -> handler(ctx)
        else -> ctx.doHandler(
          /// 拷贝一份再做迭代器，避免出竞争问题
          ms.toList().iterator(), handler
        )
      }
    } catch (ex: Exception) {
      debugRoute("Error", ctx.request.href, ex)
      PureResponse(
        HttpStatusCode.InternalServerError, body = PureStringBody(
          """
            <p>${ctx.request.href}</p>
            <pre>${ex.message ?: "Unknown Error"}</pre>
          """.trimIndent()
        )
      )
    }
  }
}


interface IChannelHandlerContext : IHandlerContext, IPureChannel {
  val pureChannelContext: PureChannelContext
}

open class ChannelHandlerContext(
  context: IHandlerContext,
  pureChannel: IPureChannel,
  override val pureChannelContext: PureChannelContext
) :
  IChannelHandlerContext, IHandlerContext by context, IPureChannel by pureChannel {
}