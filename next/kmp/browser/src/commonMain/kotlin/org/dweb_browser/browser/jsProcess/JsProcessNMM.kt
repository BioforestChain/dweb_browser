package org.dweb_browser.browser.jsProcess

import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.browser.kit.GlobalWebMessageEndpoint
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.getDebugTags
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod

val debugJsProcess = Debugger("js-process")

class JsProcessNMM : NativeMicroModule("js.browser.dweb", "Js Process") {
  init {
    name = BrowserI18nResource.JsProcess.short_name.text
    short_name = BrowserI18nResource.JsProcess.short_name.text
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);
    // 和jmm使用同一个图标
    icons = listOf(
      ImageResource(
        src = "file:///sys/browser-icons/jmm.browser.dweb.svg",
        type = "image/svg+xml",
        // purpose = "monochrome"
      )
    )
  }

  inner class JsProcessRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {


    private val JS_PROCESS_WORKER_CODE by lazy {
      mmScope.async {
        nativeFetch("file:///sys/browser-js-process-worker/index.js").binary()
      }
    }

    private val JS_CORS_HEADERS = mapOf(
      Pair("Content-Type", "text/javascript"),
      Pair("Access-Control-Allow-Origin", "*"),
      Pair("Access-Control-Allow-Headers", "*"),// 要支持 X-Dweb-Host
      Pair("Access-Control-Allow-Methods", "*"),
    )

    private val INTERNAL_PATH = "/<internal>".encodeURI()

    override suspend fun _bootstrap() {
      // 依赖 file 模块，所以需要启动它
      bootstrapContext.dns.open("file.std.dweb")
      /// 主页的网页服务
      val mainServer = this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
        // 提供基本的主页服务
        val serverIpc = server.listen()
        serverIpc.onRequest("js-process-gateway").collectIn(mmScope) { event ->
          val request = event.consume()
          // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
          if (request.uri.encodedPath.startsWith(INTERNAL_PATH)) {
            val internalPath = request.uri.encodedPath.substring(INTERNAL_PATH.length)
            if (internalPath == "/bootstrap.js") {
              serverIpc.postMessage(
                IpcResponse.fromBinary(
                  request.reqId,
                  200,
                  PureHeaders(JS_CORS_HEADERS),
                  JS_PROCESS_WORKER_CODE.await(),
                  serverIpc
                )
              )
            } else {
              serverIpc.postMessage(
                IpcResponse.fromText(
                  request.reqId,
                  404,
                  PureHeaders(JS_CORS_HEADERS),
                  "// no found $internalPath",
                  serverIpc
                )
              )
            }
          } else {
            val response = nativeFetch("file:///sys/browser-js-process-main${request.uri.fullPath}")
            serverIpc.postResponse(request.reqId, response)
          }
        }
      }

      val apis = createJsProcessWeb(mainServer, this)
      val bootstrapUrl =
        apis.dWebView.resolveUrl(mainServer.startResult.urlInfo.buildInternalUrl { resolvePath("$INTERNAL_PATH/bootstrap.js") }
          .toString())

      onBeforeShutdown {
        apis.destroy()
      }
      apis.onDestroy {
        shutdown()
      }

      val tokenPidMap = SafeHashMap<String, Int>()
      routes(
        /**
         * 创建 web worker
         * 那么当前的ipc将会用来用作接下来的通讯
         */
        "/create-process" bind PureMethod.GET by defineJsonResponse {
          debugMM("create-process", request)
          val processInfo = createProcessAndRun(
            processName = request.queryOrNull("name") ?: ipc.remote.name,
            remoteCodeIpc = ipc,
            apis = apis,
            bootstrapUrl = bootstrapUrl,
            entry = request.queryOrNull("entry"),
          )
          val processToken = randomUUID()
          tokenPidMap[processToken] = processInfo.processId

          // 创建成功了，注册销毁函数
          ipc.onClosed {
            scopeLaunch(cancelable = false) {
              debugJsProcess("close-all-process", mmid)
              val processMap = tokenPidMap.remove(processToken)
              // 关闭程序
              apis.destroyProcess(processInfo.processId)
            }
          }
          // 返回具柄
          CreateProcessReturn(processToken, portId = processInfo.portId).toJsonElement()
        },
        /// 创建 web 通讯管道
        "/create-ipc-endpoint" bind PureMethod.GET by defineNumberResponse {
          debugMM("create-ipc-endpoint", request)
          val processToken = request.query("token")

          val processId = tokenPidMap[processToken] ?: throw ResponseException(
            code = HttpStatusCode.NotFound,
            message = "ipc:${ipc.remote.mmid}/processId:$processToken invalid"
          )

          val manifestJson = request.query("manifest")
          val ids = NativeMessageChannel.getIds(
            Regex(""""id"\s*:\s*"(.+?)"""").find(manifestJson)?.groups?.get(1)?.value ?: "???.dweb",
            ipc.remote.mmid,
          )
          // 返回 endpoint 的 globalId
          apis.createIpcEndpoint(processId, manifestJson, ids.first).globalId.also {
            debugMM("create-ipc-endpoint-success", "globalId=$it manifest=$manifestJson")
          }
        }, "/create-ipc" bind PureMethod.GET by defineEmptyResponse {
          val processToken = request.query("token")
          val processId = tokenPidMap[processToken] ?: throw ResponseException(
            code = HttpStatusCode.NotFound,
            message = "ipc:${ipc.remote.mmid}/processId:$processToken invalid"
          )

          val remoteGlobalId = request.query("globalId").toInt()
          val manifestJson = request.query("manifest")
          debugMM("/create-ipc") { "remoteGlobalId=$remoteGlobalId,manifestJson=$manifestJson" }
          apis.createJsIpc(
            processId, GlobalWebMessageEndpoint.get(remoteGlobalId).port, manifestJson
          ) {}
        })
    }

    override suspend fun _shutdown() {
      debugJsProcess("JsProcess", "_shutdown")
    }

    private suspend fun createProcessAndRun(
      processName: String,
      remoteCodeIpc: Ipc,
      apis: JsProcessWebApi,
      bootstrapUrl: String,
      entry: String?,
    ): ProcessInfo {
      debugMM("createProcessAndRun", processName)
      /**
       * 用自己的域名的权限为它创建一个子域名
       */
      val httpDwebServer = createHttpDwebServer(
        DwebHttpServerOptions(subdomain = "${remoteCodeIpc.remote.mmid}-${remoteCodeIpc.pid}"),
      )

      /**
       * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
       */
      remoteCodeIpc.onClosed {
        scopeLaunch(cancelable = false) {
          httpDwebServer.close();
        }
      }

      /**
       * 代理监听
       * 让远端提供 esm 模块代码
       * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
       * 我们会对回来的代码进行处理，然后再执行
       */
      val codeProxyServerIpc = httpDwebServer.listen()

      codeProxyServerIpc.onRequest("codeProxyServer").collectIn(mmScope) { event ->
        val request = event.consume()
        debugMM("code server", request)
        codeProxyServerIpc.postResponse(
          request.reqId,
          // 转发给远端来处理 IpcServerRequest -> PureServerRequest -> PureClientRequest
          /// TODO 对代码进行翻译处理，比如 tsc
          remoteCodeIpc.request(request.toPure().toClient()).also { response ->
            /// 加入跨域配置
            for ((key, value) in JS_CORS_HEADERS) {
              response.headers.set(key, value)
            }
          },
        )
      }
      fun getJsDebugTags(): List<String> {
        val allTags = getDebugTags()
        // 如果有js特调，那么久返回特调的，否则和native共享同一套配置
        val jsOnly = allTags.filter { it.startsWith(":js:") }
        return when {
          jsOnly.isEmpty() -> allTags
          else -> jsOnly.map { tag -> tag.slice(":js:".length..<tag.length) }
        }
      }
      /// TODO env 允许远端传过来扩展
      val env = mutableMapOf<String, String>(
        // ...your envs
        // 这不是是它代码的请求路径，代码请求路径从 import.meta.url 中读取，这里是用来为开发者提供一个 baseURL 而已
        "host" to httpDwebServer.startResult.urlInfo.host,
        // 按需开启输出
        "debug" to when {
          debugJsProcess.isEnable -> Json.encodeToString(getJsDebugTags())
          else -> "[]"
        },
        // jmm的版本信息
        "jsMicroModule" to "${JsMicroModule.VERSION}.${JsMicroModule.PATCH}",
        // web brands
        "brands" to Json.encodeToString(IDWebView.brands),
      )

      /**
       * 创建一个通往 worker 的消息通道
       */
      val processInfo = apis.createProcess(
        processName,
        bootstrapUrl,
        Json.encodeToString(remoteCodeIpc.remote),
        Json.encodeToString(env),
      ) {
        remoteCodeIpc.close()
      }
      remoteCodeIpc.onClosed {
        scopeLaunch(cancelable = true) {
          apis.destroyProcess(processInfo.processId)
        }
      }


      /**
       * 开始执行代码
       */
      apis.runProcessMain(
        processInfo.processId,
        RunProcessMainOptions(main_url = apis.dWebView.resolveUrl(httpDwebServer.startResult.urlInfo.buildInternalUrl {
          resolvePath(entry ?: "/index.js")
        }.toString()))
      )

      return processInfo
    }


  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    JsProcessRuntime(bootstrapContext)
}


