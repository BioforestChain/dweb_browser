package org.dweb_browser.core.http.router

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.WARNING
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.IPureChannel
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.PureStringBody

interface IHandlerContext {
  val request: PureServerRequest
  val ipc: Ipc
  fun throwException(
    code: HttpStatusCode = HttpStatusCode.InternalServerError,
    message: String? = null,
    cause: Throwable? = null,
  ): Nothing = throw ResponseException(code, message ?: code.description, cause)
}

open class HandlerContext(override val request: PureServerRequest, override val ipc: Ipc) :
  IHandlerContext
typealias MiddlewareHttpHandler = suspend HandlerContext.(next: HttpHandler) -> PureResponse
typealias HttpHandler = suspend HandlerContext.() -> PureResponse
typealias TypedHttpHandler<T> = suspend HandlerContext.() -> T

fun HttpHandler.toChain() = HttpHandlerChain(this)
class HttpHandlerChain(val handler: HttpHandler) {
  /**
   * 上下文，如果绑定，那么在RouteHandler创建的时候，会发生自动注册路由
   */
  var ctx: HttpRouter? = null

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
    ite: Iterator<MiddlewareHttpHandler>, handler: HttpHandler,
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
    } catch (e: ResponseException) {
      WARNING("HttpHandlerChain=> $e")
      return PureResponse(e.code, body = IPureBody.from(e.message))
    } catch (ex: Exception) {
      debugRoute("HttpHandlerChain-Error", ctx.request.href, ex)
      return PureResponse(
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
  override val pureChannelContext: PureChannelContext,
) :
  IChannelHandlerContext, IHandlerContext by context, IPureChannel by pureChannel {
}
