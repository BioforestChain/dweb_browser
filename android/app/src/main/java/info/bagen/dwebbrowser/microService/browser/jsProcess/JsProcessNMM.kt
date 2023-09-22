package info.bagen.dwebbrowser.microService.browser.jsProcess

import info.bagen.dwebbrowser.App
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.build
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.closeHttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer

fun debugJsProcess(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("js-process", tag, msg, err)

class JsProcessNMM : NativeMicroModule("js.browser.dweb", "Js Process") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);
  }

  private val JS_PROCESS_WORKER_CODE by lazy {
    runBlockingCatching {
      nativeFetch("file:///sys/browser/js-process.worker/index.js").binary()
    }.getOrThrow()
  }

  private val JS_CORS_HEADERS = mapOf(
    Pair("Content-Type", "text/javascript"),
    Pair("Access-Control-Allow-Origin", "*"),
    Pair("Access-Control-Allow-Headers", "*"),// 要支持 X-Dweb-Host
    Pair("Access-Control-Allow-Methods", "*"),
  )

  private val INTERNAL_PATH = "/<internal>".encodeURI()

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// 主页的网页服务
    val mainServer = this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
      // 提供基本的主页服务
      val serverIpc = server.listen();
      serverIpc.onRequest { (request, ipc) ->
        // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
        if (request.uri.encodedPath.startsWith(INTERNAL_PATH)) {
          val internalPath = request.uri.encodedPath.substring(INTERNAL_PATH.length)
          if (internalPath == "/bootstrap.js") {
            ipc.postMessage(
              IpcResponse.fromBinary(
                request.req_id,
                200,
                IpcHeaders(JS_CORS_HEADERS),
                JS_PROCESS_WORKER_CODE,
                ipc
              )
            )
          } else {
            ipc.postMessage(
              IpcResponse.fromText(
                request.req_id,
                404,
                IpcHeaders(JS_CORS_HEADERS),
                "// no found $internalPath",
                ipc
              )
            )
          }
        } else {
          val response = nativeFetch("file:///sys/browser/js-process.main${request.uri.fullPath}")
          ipc.postMessage(
            IpcResponse.fromResponse(request.req_id, response, ipc)
          )
        }
      }
    }
    val apis = createJsProcessWeb(mainServer)
    val bootstrapUrl = mainServer.startResult.urlInfo.buildInternalUrl()
      .build { resolvePath("$INTERNAL_PATH/bootstrap.js") }.toString()

    this.onAfterShutdown {
      apis.dWebView.destroy()
    }
    apis.dWebView.onDestroy {
      shutdown()
    }

    val ipcProcessIdMap = mutableMapOf<String, MutableMap<String, PromiseOut<Int>>>()
    val ipcProcessIdMapLock = Mutex()
    routes(
      /// 创建 web worker
      // request 需要携带一个流，来为 web worker 提供代码服务
      "/create-process" bind HttpMethod.Post to definePureStreamHandler {
        debugJsProcess("create-process", ipc.remote.mmid)
        val po = ipcProcessIdMapLock.withLock {
          val processId = request.queryOrFail("process_id")
          val processIdMap = ipcProcessIdMap.getOrPut(ipc.remote.mmid) {
            mutableMapOf()
          }

          if (processIdMap.contains(processId)) {
            throw Exception("ipc:${ipc.remote.mmid}/processId:$processId has already using")
          }
          // 创建成功了，注册销毁函数
          ipc.onClose {
            closeAllProcessByIpc(apis, ipcProcessIdMap, ipc.remote.mmid)
          }

          PromiseOut<Int>().also { processIdMap[processId] = it }
        }
        val result = createProcessAndRun(
          ipc, apis,
          bootstrapUrl,
          request.query("entry"), request,
        )
        // 将自定义的 processId 与真实的 js-process_id 进行关联
        po.resolve(result.processHandler.info.process_id)

        // 返回流，因为构建了一个双工通讯用于代码提供服务
        result.streamIpc.input.stream
      },
      /// 创建 web 通讯管道
      "/create-ipc" bind HttpMethod.Get to defineNumberResponse {
        val processId = request.queryOrFail("process_id")

        /**
         * 虽然 mmid 是从远程直接传来的，但风险与jsProcess无关，
         * 因为首先我们是基于 ipc 来得到 processId 的，所以这个 mmid 属于 ipc 自己的定义
         */
        val mmid = request.queryOrFail("mmid")
        val ipcProcessID = ipcProcessIdMapLock.withLock {
          ipcProcessIdMap[ipc.remote.mmid]?.get(processId)
            ?: throw Exception("ipc:${ipc.remote.mmid}/processId:$processId invalid")
        }.waitPromise()

        // 返回 port_id
        createIpc(ipc, apis, ipcProcessID, mmid)
      },
      /// 关闭process
      "/close-all-process" bind HttpMethod.Get to defineBooleanResponse {
        return@defineBooleanResponse closeAllProcessByIpc(
          apis,
          ipcProcessIdMap,
          ipc.remote.mmid
        ).also {
          /// 强制关闭Ipc
          ipc.close()
        }
      },
      // ipc 创建错误
      "/create-ipc-fail" bind HttpMethod.Get to defineBooleanResponse {
        val processId = request.queryOrFail("process_id")
        val processMap = ipcProcessIdMap[ipc.remote.mmid]?.get(processId)
        debugJsProcess("create-ipc-fail", ipc.remote.mmid)
        if (processMap === null) {
          throw Exception("ipc:${ipc.remote.mmid}/processId:${processId} invalid")
        }
        val mmid = request.queryOrFail("mmid")
        val reason = request.queryOrFail("reason")
        apis.createIpcFail(processId, mmid, reason)
        return@defineBooleanResponse false
      })
  }

  private suspend fun createJsProcessWeb(mainServer: HttpDwebServer): JsProcessWebApi {
    val afterReadyPo = PromiseOut<Unit>()
    /// WebView 实例
    val apis = withContext(Dispatchers.Main) {

      val urlInfo = mainServer.startResult.urlInfo
      JsProcessWebApi(DWebView(
        App.appContext, this@JsProcessNMM, DWebView.Options(
          url = urlInfo.buildInternalUrl().build { resolvePath("/index.html") }.toString()
        )
      ).also { it.settings.safeBrowsingEnabled = false }).also { api ->
        api.dWebView.onReady { afterReadyPo.resolve(Unit) }
      }
    }
    afterReadyPo.waitPromise()
    return apis
  }

  override suspend fun _shutdown() {
    debugJsProcess("JsProcess", "_shutdown")
  }

  private suspend fun createProcessAndRun(
    ipc: Ipc,
    apis: JsProcessWebApi,
    bootstrapUrl: String,
    entry: String?,
    requestMessage: PureRequest,
  ): CreateProcessAndRunResult {
    /**
     * 用自己的域名的权限为它创建一个子域名
     */
    val httpDwebServer = createHttpDwebServer(
      DwebHttpServerOptions(subdomain = ipc.remote.mmid),
    );

    /**
     * 远端是代码服务，所以这里是 client 的身份
     */
    val streamIpc = ReadableStreamIpc(ipc.remote, "code-proxy-server").also {
      it.bindIncomeStream(requestMessage.body.toPureStream());
    }
    this.addToIpcSet(streamIpc)

    /**
     * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
     */
    ipc.onClose {
      streamIpc.close()
    }
    /**
     * “代码IPC流通道”关闭的时候，关闭这个子域名
     */
    streamIpc.onClose {
      httpDwebServer.close();
    }

    /**
     * 代理监听
     * 让远端提供 esm 模块代码
     * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
     * 我们会对回来的代码进行处理，然后再执行
     */
    val codeProxyServerIpc = httpDwebServer.listen()

    codeProxyServerIpc.onRequest { (request, ipc) ->
      ipc.postResponse(
        request.req_id,
        // 转发给远端来处理
        /// TODO 对代码进行翻译处理
        streamIpc.request(request.toRequest()).let {
          /// 加入跨域配置
          var response = it;
          for ((key, value) in JS_CORS_HEADERS) {
            response.headers.apply { set(key, value) }
          }
          response
        },
      )
    }

    @Serializable
    data class JsProcessMetadata(val mmid: MMID) {}
    /// TODO 需要传过来，而不是自己构建
    val metadata = JsProcessMetadata(ipc.remote.mmid)

    /// TODO env 允许远端传过来扩展
    val env = mutableMapOf( // ...your envs
      Pair("host", httpDwebServer.startResult.urlInfo.host),
      Pair("debug", "true"),
      Pair("ipc-support-protocols", "")
    )

    /**
     * 创建一个通往 worker 的消息通道
     */
    val processHandler = apis.createProcess(
      bootstrapUrl,
      Json.encodeToString(metadata),
      Json.encodeToString(env),
      ipc.remote,
      httpDwebServer.startResult.urlInfo.host
    )
    processHandler.ipc.onClose {
      apis.destroyProcess(processHandler.info.process_id)
    }
    /**
     * 收到 Worker 的数据请求，由 js-process 代理转发回去，然后将返回的内容再代理响应会去
     *
     * TODO 所有的 ipcMessage 应该都有 headers，这样我们在 workerIpcMessage.headers 中附带上当前的 processId，回来的 remoteIpcMessage.headers 同样如此，否则目前的模式只能代理一个 js-process 的消息。另外开 streamIpc 导致的翻译成本是完全没必要的
     */
    processHandler.ipc.onMessage { (workerIpcMessage) ->
      /**
       * 直接转发给远端 ipc，如果是nativeIpc，那么几乎没有性能损耗
       */
      ipc.postMessage(workerIpcMessage)
    }
    ipc.onMessage { (remoteIpcMessage) ->
      processHandler.ipc.postMessage(remoteIpcMessage)
    }
    /// 由于 MessagePort 的特殊性，它无法知道自己什么时候被关闭，所以这里通过宿主关系，绑定它的close触发时机
    ipc.onClose {
      processHandler.ipc.close()
    }
    /// 双向绑定关闭
    processHandler.ipc.onClose {
      ipc.close()
    }

    /**
     * 开始执行代码
     */
    apis.runProcessMain(processHandler.info.process_id,
      JsProcessWebApi.RunProcessMainOptions(main_url = httpDwebServer.startResult.urlInfo.buildInternalUrl()
        .build { resolvePath(entry ?: "/index.js") }.toString()))

    return CreateProcessAndRunResult(streamIpc, processHandler)
  }

  data class CreateProcessAndRunResult(
    val streamIpc: ReadableStreamIpc, val processHandler: JsProcessWebApi.ProcessHandler
  )

  private suspend fun createIpc(
    ipc: Ipc, apis: JsProcessWebApi, process_id: Int, mmid: MMID
  ): Int {
    return apis.createIpc(process_id, mmid)
  }

  private suspend fun closeAllProcessByIpc(
    apis: JsProcessWebApi,
    ipcProcessIdMap: MutableMap<String, MutableMap<String, PromiseOut<Int>>>,
    mmid: MMID
  ): Boolean {
    debugJsProcess("close-all-process", mmid)
    val processMap = ipcProcessIdMap.remove(mmid) ?: return false;
    // 关闭程序
    for (po in processMap.values) {
      val processId = po.waitPromise()
      apis.destroyProcess(processId)
    }

    // 关闭代码通道
    closeHttpDwebServer(DwebHttpServerOptions(80, mmid))
    return true;
  }
}


