package org.dweb_browser.microservice.core

import org.dweb_browser.helper.*
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.IpcSupportProtocols
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.NativeIpc
import org.dweb_browser.microservice.ipc.NativeMessageChannel
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.http4k.core.*
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextKey
import org.http4k.routing.RoutingHttpHandler
import java.io.InputStream

fun debugNMM(tag: String, msg: Any = "", err: Throwable? = null) =
  printdebugln("DNS", tag, msg, err)

abstract class NativeMicroModule(override val mmid: MMID, override val name: String) :
  MicroModule() {
  override val ipc_support_protocols =
    IpcSupportProtocols(cbor = true, protobuf = true, raw = true)
  override val categories: MutableList<MICRO_MODULE_CATEGORY> = mutableListOf()
  override val dweb_deeplinks: MutableList<DWEB_DEEPLINK> = mutableListOf()
  override val dir: String? = null
  override val lang: String? = null
  override val short_name: String = ""
  override val description: String? = null
  override val icons: List<ImageResource> = emptyList()
  override val display: DisplayMode? = null
  override val orientation: String? = null
  override val screenshots: List<ImageResource>? = null
  override val shortcuts: List<ShortcutItem>  = emptyList()
  override val theme_color: String? = null
  override val background_color: String = "#ffffff"


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

  var apiRouting: RoutingHttpHandler? = null


  private val requestContexts = RequestContexts()
  private val requestContextKey_ipc = RequestContextKey.required<Ipc>(requestContexts)
  private val ipcApiFilter = ServerFilters.InitialiseRequestContext(requestContexts)

  /**
   * 实现一整套简易的路由响应规则
   */
  init {
    onConnect { (clientIpc) ->
      clientIpc.onRequest { (ipcRequest) ->
        val routes = apiRouting ?: return@onRequest null;
        val routesWithContext = routes.withFilter(ipcApiFilter.then(Filter { next ->
          { next(it.with(requestContextKey_ipc of clientIpc)) }
        }));
        debugNMM("NMM/Handler", ipcRequest.url)
        val request = ipcRequest.toRequest()
        val response = routesWithContext(request)
        clientIpc.postMessage(
          IpcResponse.fromResponse(
            ipcRequest.req_id, response, clientIpc
          )
        )
      }
    }
  }

  class ResponseRegistry {
    companion object {
      private val regMap = mutableMapOf<Class<*>, (item: Any) -> Response>()

      fun <T : Any> registryResponse(type: Class<T>, handler: (item: T) -> Response) {
        regMap[type] = handler as (item: Any) -> Response
      }

      init {
        registryResponse(ByteArray::class.java) {
          Response(Status.OK).body(MemoryBody(it))
        }
        registryResponse(InputStream::class.java) {
          Response(Status.OK).body(it)
        }
      }

      fun <T : Any> registryJsonAble(type: Class<T>, handler: (item: T) -> Any) {
        registryResponse(type) {
          asJson(handler(it))
        }
      }

      fun handle(result: Any): Response {
        val javaClass = result.javaClass
        return when (val handler = regMap[javaClass]) {
          null -> {
            var superJavaClass = javaClass.superclass
            while (superJavaClass != null) {
              // 尝试寻找继承关系
              when (val handler = regMap[superJavaClass]) {
                null -> superJavaClass = superJavaClass.superclass
                else -> return handler(result)
              }
            }
            // 否则默认当成JSON来返回
            return asJson(result)
          }
          // 如果有注册处理函数，那么交给处理函数进行处理
          else -> handler(result)
        }
      }

      fun asJson(result: Any) = Response(Status.OK).body(gson.toJson(result))
        .header("Content-Type", "application/json")

    }
  }

  protected fun defineHandler(handler: suspend (request: Request) -> Any?) = { request: Request ->
    runBlockingCatching {
      when (val result = handler(request)) {
        null, Unit -> {
          Response(Status.OK)
        }

        is Response -> result
        is ByteArray -> Response(Status.OK).body(MemoryBody(result))
        is InputStream -> Response(Status.OK).body(result)
        else -> {
          // 如果有注册处理函数，那么交给处理函数进行处理
          ResponseRegistry.handle(result)
        }
      }
    }.getOrElse { ex ->
      debugNMM("NMM/Error", request.uri, ex)
      Response(Status.INTERNAL_SERVER_ERROR).body(
        """
          <p>${request.uri}</p>
          <pre>${ex.message ?: "Unknown Error"}</pre>
        """.trimIndent()
      )
    }
  }

  protected fun defineHandler(handler: suspend (request: Request, ipc: Ipc) -> Any?) =
    defineHandler { request ->
      handler(request, requestContextKey_ipc(request))
    }
}

