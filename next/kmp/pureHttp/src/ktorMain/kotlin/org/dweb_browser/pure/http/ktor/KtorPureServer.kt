package org.dweb_browser.pure.http.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ServerConfigBuilder
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.ApplicationEnvironmentBuilder
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketUpgrade
import io.ktor.server.websocket.WebSockets
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.commonAsyncExceptionHandler
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.HttpPureServerOnRequest
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.WS_BAD_GATEWAY
import org.dweb_browser.pure.http.debugHttpPureServer
import kotlin.coroutines.CoroutineContext

open class KtorPureServer<out TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration>(
  private val serverEngineFactory: ApplicationEngineFactory<TEngine, TConfiguration>,
  val onRequest: HttpPureServerOnRequest,
) {
  protected var serverEngine: EmbeddedServer<@UnsafeVariance TEngine, TConfiguration>? = null
  protected val serverLock = Mutex()
  protected open fun createServer(
    config: TConfiguration.() -> Unit = {},
    envBuilder: ApplicationEnvironmentBuilder.() -> Unit = {},
    serverConfigBuilder: ServerConfigBuilder.() -> Unit = {},
  ): EmbeddedServer<@UnsafeVariance TEngine, TConfiguration> {
    val applicationModule: Application.() -> Unit = {
      install(WebSockets)
      install(createApplicationPlugin("dweb-gateway") {
        onCall { call ->
          /// 将 ktor的request 构建成 pureRequest
          call.request.asPureRequest().also { rawRequest ->
            val url = rawRequest.href
            var pureRequest = rawRequest
            val pureChannelDeferred = if (pureRequest.isWebSocket) {
              CompletableDeferred<PureChannel>().also {
                pureRequest = pureRequest.copy(channel = it)
              }
            } else null
            val response = onRequest(pureRequest) ?: when {
              pureRequest.hasChannel -> PureResponse(HttpStatusCode.WS_BAD_GATEWAY)
              else -> PureResponse(HttpStatusCode.BadRequest)
            }

            if (pureChannelDeferred != null) {
              /// 如果是101响应头，那么使用WebSocket来作为双工的通讯标准进行传输，这里使用PureChannel来承担这层抽象
              when (response.status.value) {
                /// 如果是200响应头，说明是传统的响应模式，这时候只处理输出，websocket的incoming数据完全忽视
                200 -> {
                  val res = WebSocketUpgrade(call, null) {
                    val ws = this;
                    pureChannelDeferred.cancel(CancellationException("no need channel"))
                    val streamReader = response.stream().getReader("Http1Server websocket")
                    /// 将从服务端收到的数据，转成 200 的标准传输到 websocket 的 frame 中
                    launch {
                      streamReader.consumeEachArrayRange { byteArray, _ ->
                        ws.send(Frame.Binary(true, byteArray))
                      }
                      ws.close()
                    }
                    /// 将从客户端收到的数据，但是忽视
                    @Suppress("ControlFlowWithEmptyBody") for (frame in ws.incoming) {// 注意，这里ws.incoming要立刻进行，不能在launch中异步执行，否则ws将无法完成连接建立
                    }
                    /// 等到双工关闭，同时也关闭读取层
                    streamReader.cancel(null)

                  }
                  call.respond(res)
                }
                /// 如果是100响应头，说明走的是标准的PureDuplex模型，那么使用WebSocket来作为双工的通讯标准进行传输
                101 -> {
                  val res = WebSocketUpgrade(call, null) {
                    val ws = this;
                    val income = Channel<PureFrame>()
                    val outgoing = Channel<PureFrame>()
                    val pureChannel = PureChannel(income, outgoing, ws)
                    pureChannelDeferred.complete(pureChannel)

                    pipeToPureChannel(ws, url, income, outgoing, pureChannel)
                  }
                  call.respond(res)
                }

                else -> call.response.fromPureResponse(response).also {
                  debugHttpPureServer("WS-ERROR", response.body.toPureString())
                }
              }
            } else {
              call.response.fromPureResponse(response)
            }
          }
        }
      })
    }
    return embeddedServer(
      factory = serverEngineFactory,
      rootConfig = serverConfig(
        environment = applicationEnvironment {
          log = KtorSimpleLogger("pure-server")
          envBuilder()
        },
      ) {
        this.parentCoroutineContext = ioAsyncExceptionHandler + getCoroutineExceptionHandler()
        watchPaths = emptyList()
        serverConfigBuilder()
        // 处理请求
        module(applicationModule)
      },
      // configuration script for the engine
      configure = config,
    )
  }

  protected open fun getCoroutineExceptionHandler(): CoroutineContext = commonAsyncExceptionHandler

  protected suspend inline fun startServer(
    onStarted: EmbeddedServer<@UnsafeVariance TEngine, TConfiguration>.() -> Unit = {},
    createServer: () -> EmbeddedServer<@UnsafeVariance TEngine, TConfiguration>,
  ) =
    serverLock.withLock {
      val engine = when (val engine = serverEngine) {
        null -> {
          createServer().also { newEngine ->
            newEngine.start(wait = false)
            newEngine.onStarted()
            this.serverEngine = newEngine
            newEngine.addShutdownHook {
              debugHttpPureServer("startServer/addShutdownHook", "shutdown")
              globalDefaultScope.launch(start = CoroutineStart.UNDISPATCHED) {
                close()
              }
            }
          }
        }

        else -> engine
      }
      serverStateFlow.value ?: engine.application.engine.getPort().also {
        debugHttpPureServer("serverStateFlow/emit") {
          "emit($it) in ${this@KtorPureServer}"
        }
        serverStateFlow.emit(it)
      }
    }

  open suspend fun start(port: UShort) = startServer(createServer = {
    createServer(config = {
      connector {
        this.port = port.toInt()
        this.host = host
      }
    })
  })

  protected suspend fun getPort() = serverEngine?.run { application.engine.getPort() }

  suspend fun close() = serverLock.withLock {
    serverEngine?.also { engine ->
      serverEngine = null
      engine.stop()
      serverStateFlow.emit(null)
    }
    Unit
  }

  suspend fun getDebugInfo(): String {
    return serverEngine?.run {
      engineConfig.connectors.joinToString(", ") {
        "${it.type.name}://${it.host}:${it.port}".lowercase()
      }
    } ?: "No Start Yet."
  }

  protected val serverStateFlow = MutableStateFlow<UShort?>(null)
  val stateFlow = serverStateFlow as StateFlow<UShort?>
}


suspend fun ApplicationEngine.getPort(): UShort {
  val connectors = resolvedConnectors()
  debugHttpPureServer("getPort") { connectors.joinToString(",") { "${it.type.name.lowercase()}://${it.host}:${it.port}" } }
  return connectors.first().port.toUShort()
}