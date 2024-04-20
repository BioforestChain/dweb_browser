package org.dweb_browser.browser.jsProcess

import io.ktor.http.fullPath
import kotlinx.coroutines.async
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.WebMessageEndpoint
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.closeHttpDwebServer
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.listen
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs

val debugJsProcess = Debugger("js-process")

class JsProcessNMM : NativeMicroModule("js.browser.dweb", "Js Process") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);
  }

  inner class JsProcessRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {


    private val JS_PROCESS_WORKER_CODE by lazy {
      mmScope.async {
        nativeFetch("file:///sys/browser/js-process.worker/index.js").binary()
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
            val response = nativeFetch("file:///sys/browser/js-process.main${request.uri.fullPath}")
            serverIpc.postResponse(request.reqId, response)
          }
        }
      }

      val apis = createJsProcessWeb(mainServer, this)
      val bootstrapUrl =
        apis.dWebView.resolveUrl(mainServer.startResult.urlInfo.buildInternalUrl { resolvePath("$INTERNAL_PATH/bootstrap.js") }
          .toString())

      onBeforeShutdown.listen {
        apis.destroy()
      }
      apis.onDestroy {
        shutdown()
      }

      val processIdMap = SafeHashMap<String, Int>()
      routes(
        /**
         * 创建 web worker
         * 那么当前的ipc将会用来用作接下来的通讯
         */
        "/create-process" bind PureMethod.GET by defineStringResponse {
          val processId = createProcessAndRun(
            processName = request.queryOrNull("name") ?: ipc.remote.name,
            remoteCodeIpc = ipc,
            remoteFetchIpc = ipc.waitForkedIpc(request.queryAs("fetch-ipc-pid")),
            apis = apis,
            bootstrapUrl = bootstrapUrl,
            entry = request.queryOrNull("entry"),
          )
          val handlerId = randomUUID()
          processIdMap[handlerId] = processId

          // 创建成功了，注册销毁函数
          ipc.onClosed {
            scopeLaunch(cancelable = false) {
              debugJsProcess("close-all-process", mmid)
              val processMap = processIdMap.remove(handlerId)
              // 关闭代码通道
              closeHttpDwebServer(DwebHttpServerOptions(mmid))
              // 关闭程序
              apis.destroyProcess(processId)
            }
          }
          // 返回具柄
          handlerId
        },
        /// 创建 web 通讯管道
        "/create-ipc" bind PureMethod.GET by defineNumberResponse {
          val handlerId = request.query("id")

          /**
           * 虽然 mmid 是从远程直接传来的，但风险与jsProcess无关，
           * 因为首先我们是基于 ipc 来得到 processId 的，所以这个 mmid 属于 ipc 自己的定义
           */
          val manifestJson = request.query("manifest")
          val processId = processIdMap[handlerId]
            ?: throw Exception("ipc:${ipc.remote.mmid}/processId:$handlerId invalid")

          // 返回 port_id
          createIpc(apis, processId, manifestJson)
        },
//        /// 桥接两个JMM
//        "/bridge-ipc" bind PureMethod.GET by defineEmptyResponse {
//          val handlerId = request.query("id")
//          val fromMMid = request.query("from_mmid")
//          val toMMid = request.query("to_mmid")
//          val processId = processIdMap[handlerId]
//            ?: throw Exception("ipc:${ipc.remote.mmid}/processId:$handlerId invalid")
//
//
//          // 返回 port_id
//          bridgeIpc(apis, ipcProcessID, fromMMid, toMMid)
//        }
      )
    }

    override suspend fun _shutdown() {
      debugJsProcess("JsProcess", "_shutdown")
    }

    private suspend fun createProcessAndRun(
      processName: String,
      remoteCodeIpc: Ipc,
      remoteFetchIpc:Ipc,
      apis: JsProcessWebApi,
      bootstrapUrl: String,
      entry: String?,
    ): Int {
      /**
       * 用自己的域名的权限为它创建一个子域名
       */
      val httpDwebServer = createHttpDwebServer(
        DwebHttpServerOptions(subdomain = "${remoteCodeIpc.remote.mmid}-${remoteCodeIpc.pid}"),
      );

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

      /// TODO env 允许远端传过来扩展
      val env = mutableMapOf<String, String>( // ...your envs
        // 这不是是它代码的请求路径，代码请求路径从 import.meta.url 中读取，这里是用来为开发者提供一个 baseURL 而已
        "host" to httpDwebServer.startResult.urlInfo.host,
        // native环境是否启用调试
        "debug" to debugJsProcess.isEnable.toString(),
        // jmm的版本信息
        "jsMicroModule" to "${JsMicroModule.VERSION}.${JsMicroModule.PATCH}"
      )

      /**
       * 创建一个通往 worker 的消息通道
       */
      val processInfo = apis.createProcess(
        processName,
        bootstrapUrl,
        Json.encodeToString(remoteFetchIpc.remote),
        Json.encodeToString(env),
      )

      val fetchIpc = kotlinIpcPool.createIpc(
        endpoint = WebMessageEndpoint.from(
          "jsWorker-${remoteFetchIpc.remote.mmid}", kotlinIpcPool.scope, processInfo.port
        ),
        pid = 0,
        locale = remoteFetchIpc.remote,
        remote = remoteFetchIpc.remote,
      )
      fetchIpc.onClosed {
        scopeLaunch(cancelable = false) {
          apis.destroyProcess(processInfo.process_id)
        }
      }
      /**
       * 收到 Worker 的数据请求，由 js-process 代理转发回去，然后将返回的内容再代理响应会去
       *
       * TODO 所有的 ipcMessage 应该都有 headers，这样我们在 workerIpcMessage.headers 中附带上当前的 processId，
       * 回来的 remoteIpcMessage.headers 同样如此，否则目前的模式只能代理一个 js-process 的消息。另外开 streamIpc 导致的翻译成本是完全没必要的
       */
      fetchIpc.onMessage("jsWorker-to-process").collectIn(mmScope) { workerIpcMessage ->
        /**
         * 直接转发给远端 ipc，如果是nativeIpc，那么几乎没有性能损耗
         */
        remoteFetchIpc.postMessage(workerIpcMessage.consume())
      }
      remoteFetchIpc.onMessage("process-to-jsWorker").collectIn(mmScope) { remoteIpcMessage ->
        fetchIpc.postMessage(remoteIpcMessage.consume())
      }
      /// 由于 MessagePort 的特殊性，它无法知道自己什么时候被关闭，所以这里通过宿主关系，绑定它的close触发时机
      remoteFetchIpc.onClosed {
        scopeLaunch(cancelable = false) {
          fetchIpc.close()
        }
      }
      /// 双向绑定关闭
      fetchIpc.onClosed {
        scopeLaunch(cancelable = false) {
          remoteFetchIpc.close()
        }
      }

      /**
       * 开始执行代码
       */
      apis.runProcessMain(
        processInfo.process_id,
        RunProcessMainOptions(main_url = apis.dWebView.resolveUrl(httpDwebServer.startResult.urlInfo.buildInternalUrl {
          resolvePath(entry ?: "/index.js")
        }.toString()))
      )

      return processInfo.process_id
    }

    /**创建到worker的Ipc 如果是worker到worker互联，则每个人分配一个messageChannel的port*/
    private suspend fun createIpc(
      apis: JsProcessWebApi, processId: Int, manifestJson: String,
    ): Int {
      return apis.createIpc(processId, manifestJson)
    }

//    private suspend fun bridgeIpc(
//      apis: JsProcessWebApi,
//      processId: Int,
//      fromMMid: MMID,
//      toMMid: MMID,
//    ): Boolean {
//      return apis.bridgeIpc(processId, fromMMid, toMMid)
//    }

  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    JsProcessRuntime(bootstrapContext)
}


