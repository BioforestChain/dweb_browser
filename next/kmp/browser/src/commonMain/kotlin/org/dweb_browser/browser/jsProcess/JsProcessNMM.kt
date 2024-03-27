package org.dweb_browser.browser.jsProcess

import io.ktor.http.fullPath
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcOptions
import org.dweb_browser.core.ipc.ReadableStreamIpc
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.closeHttpDwebServer
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest

val debugJsProcess = Debugger("js-process")

class JsProcessNMM : NativeMicroModule("js.browser.dweb", "Js Process") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);
  }

  private val JS_PROCESS_WORKER_CODE by lazy {
    ioAsyncScope.async {
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

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// 主页的网页服务
    val mainServer = this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
      // 提供基本的主页服务
      val serverIpc = server.listen()
      serverIpc.requestFlow.onEach { (request, ipc) ->
        // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
        if (request.uri.encodedPath.startsWith(INTERNAL_PATH)) {
          val internalPath = request.uri.encodedPath.substring(INTERNAL_PATH.length)
          if (internalPath == "/bootstrap.js") {
            ipc.postMessage(
              IpcResponse.fromBinary(
                request.reqId,
                200,
                PureHeaders(JS_CORS_HEADERS),
                JS_PROCESS_WORKER_CODE.await(),
                ipc
              )
            )
          } else {
            ipc.postMessage(
              IpcResponse.fromText(
                request.reqId, 404, PureHeaders(JS_CORS_HEADERS), "// no found $internalPath", ipc
              )
            )
          }
        } else {
          val response = nativeFetch("file:///sys/browser/js-process.main${request.uri.fullPath}")
          ipc.postMessage(
            IpcResponse.fromResponse(request.reqId, response, ipc)
          )
        }
      }.launchIn(ioAsyncScope)
    }

    val apis = createJsProcessWeb(mainServer, this)
    val bootstrapUrl =
      apis.dWebView.resolveUrl(mainServer.startResult.urlInfo.buildInternalUrl { resolvePath("$INTERNAL_PATH/bootstrap.js") }
        .toString())

    this.onAfterShutdown {
      apis.destroy()
    }
    apis.onDestroy {
      shutdown()
    }

    val ipcProcessIdMap = mutableMapOf<String, MutableMap<String, PromiseOut<Int>>>()
    val ipcProcessIdMapLock = Mutex()
    routes(
      /// 创建 web worker
      // request 需要携带一个流，来为 web worker 提供代码服务
      "/create-process" bind PureMethod.POST by definePureStreamHandler {
        val processId = request.query("process_id")
        val po = ipcProcessIdMapLock.withLock {
          debugJsProcess("-create-process", "mmid=${ipc.remote.mmid} processId=$processId")

          val processIdMap = ipcProcessIdMap.getOrPut(ipc.remote.mmid) {
            mutableMapOf()
          }

          if (processIdMap.contains(processId)) {
            throw Exception("ipc:${ipc.remote.mmid}/processId:$processId has already using")
          }
          PromiseOut<Int>().also { processIdMap[processId] = it }
        }
        // 创建成功了，注册销毁函数
        ioAsyncScope.launch {
          ipc.closeDeferred.await()
          closeAllProcessByIpc(apis, ipcProcessIdMap, ipc.remote.mmid)
        }
        val result = createProcessAndRun(
          processId,
          ipc, apis,
          bootstrapUrl,
          request.queryOrNull("entry"), request,
        )
        // 将自定义的 processId 与真实的 js-process_id 进行关联
        po.resolve(result.processHandler.info.process_id)

        // 返回流，因为构建了一个双工通讯用于代码提供服务
        result.streamIpc.input.stream
      },
      /// 创建 web 通讯管道
      "/create-ipc" bind PureMethod.GET by defineNumberResponse {
        val processId = request.query("process_id")

        /**
         * 虽然 mmid 是从远程直接传来的，但风险与jsProcess无关，
         * 因为首先我们是基于 ipc 来得到 processId 的，所以这个 mmid 属于 ipc 自己的定义
         */
        val mmid = request.query("mmid")
        val ipcProcessID = ipcProcessIdMapLock.withLock {
          ipcProcessIdMap[ipc.remote.mmid]?.get(processId)
            ?: throw Exception("ipc:${ipc.remote.mmid}/processId:$processId invalid")
        }.waitPromise()

        // 返回 port_id
        createIpc(apis, ipcProcessID, mmid)
      },
      /// 桥接两个JMM
      "/bridge-ipc" bind PureMethod.GET by defineEmptyResponse {
        val processId = request.query("process_id")
        val fromMMid = request.query("from_mmid")
        val toMMid = request.query("to_mmid")
        val ipcProcessID = ipcProcessIdMapLock.withLock {
          ipcProcessIdMap[ipc.remote.mmid]?.get(processId)
            ?: throw Exception("ipc:${ipc.remote.mmid}/processId:$processId invalid")
        }.waitPromise()

        // 返回 port_id
        bridgeIpc(apis, ipcProcessID, fromMMid, toMMid)
      }
    )
  }

  override suspend fun _shutdown() {
    debugJsProcess("JsProcess", "_shutdown")
  }

  private suspend fun createProcessAndRun(
    processId: String,
    ipc: Ipc,
    apis: JsProcessWebApi,
    bootstrapUrl: String,
    entry: String?,
    requestMessage: PureServerRequest,
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
    val streamIpc =
      kotlinIpcPool.create<ReadableStreamIpc>(
        "code-proxy-server-$processId",
        IpcOptions(ipc.remote)
      )
    streamIpc.bindIncomeStream(requestMessage.body.toPureStream())
    this.addToIpcSet(streamIpc)

    /**
     * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
     */
    ioAsyncScope.launch {
      ipc.closeDeferred.await()
      streamIpc.close()
    }
    /**
     * “代码IPC流通道”关闭的时候，关闭这个子域名
     */
    ioAsyncScope.launch {
      streamIpc.closeDeferred.await()
      httpDwebServer.close();
    }

    /**
     * 代理监听
     * 让远端提供 esm 模块代码
     * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
     * 我们会对回来的代码进行处理，然后再执行
     */
    val codeProxyServerIpc = httpDwebServer.listen()

    codeProxyServerIpc.requestFlow.onEach { (request, ipc) ->
      ipc.postResponse(
        request.reqId,
        // 转发给远端来处理 IpcServerRequest -> PureServerRequest -> PureClientRequest
        /// TODO 对代码进行翻译处理
        streamIpc.request(request.toPure().toClient()).let {
          /// 加入跨域配置
          val response = it;
          for ((key, value) in JS_CORS_HEADERS) {
            response.headers.apply { set(key, value) }
          }
          response
        },
      )
    }.launchIn(ioAsyncScope)

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
    ioAsyncScope.launch {
      processHandler.ipc.closeDeferred.await()
      apis.destroyProcess(processHandler.info.process_id)
    }
    /**
     * 收到 Worker 的数据请求，由 js-process 代理转发回去，然后将返回的内容再代理响应会去
     *
     * TODO 所有的 ipcMessage 应该都有 headers，这样我们在 workerIpcMessage.headers 中附带上当前的 processId，
     * 回来的 remoteIpcMessage.headers 同样如此，否则目前的模式只能代理一个 js-process 的消息。另外开 streamIpc 导致的翻译成本是完全没必要的
     */
    processHandler.ipc.messageFlow.onEach { (workerIpcMessage) ->
      /**
       * 直接转发给远端 ipc，如果是nativeIpc，那么几乎没有性能损耗
       */
      ipc.postMessage(workerIpcMessage)
    }.launchIn(ioAsyncScope)
    ipc.messageFlow.onEach { (remoteIpcMessage) ->
      processHandler.ipc.postMessage(remoteIpcMessage)
    }.launchIn(ioAsyncScope)
    /// 由于 MessagePort 的特殊性，它无法知道自己什么时候被关闭，所以这里通过宿主关系，绑定它的close触发时机
    ioAsyncScope.launch {
      ipc.closeDeferred.await()
      processHandler.ipc.close()
    }
    /// 双向绑定关闭
    ioAsyncScope.launch {
      processHandler.ipc.closeDeferred.await()
      ipc.close()
    }

    /**
     * 开始执行代码
     */
    apis.runProcessMain(
      processHandler.info.process_id,
      RunProcessMainOptions(main_url = apis.dWebView.resolveUrl(httpDwebServer.startResult.urlInfo.buildInternalUrl {
        resolvePath(entry ?: "/index.js")
      }.toString()))
    )

    return CreateProcessAndRunResult(streamIpc, processHandler)
  }

  data class CreateProcessAndRunResult(
    val streamIpc: ReadableStreamIpc, val processHandler: ProcessHandler
  )

  /**创建到worker的Ipc 如果是worker到worker互联，则每个人分配一个messageChannel的port*/
  private suspend fun createIpc(
    apis: JsProcessWebApi, processId: Int, mmid: MMID
  ): Int {
    return apis.createIpc(processId, mmid)
  }

  private suspend fun bridgeIpc(
    apis: JsProcessWebApi,
    processId: Int,
    fromMMid: MMID,
    toMMid: MMID
  ): Boolean {
    return apis.bridgeIpc(processId, fromMMid, toMMid)
  }

  private suspend fun closeAllProcessByIpc(
    apis: JsProcessWebApi,
    ipcProcessIdMap: MutableMap<String, MutableMap<String, PromiseOut<Int>>>,
    mmid: MMID
  ) {
    debugJsProcess("close-all-process", mmid)
    val processMap = ipcProcessIdMap.remove(mmid) ?: return
    // 关闭代码通道
    closeHttpDwebServer(DwebHttpServerOptions(mmid))
    // 关闭程序
    for (po in processMap.values) {
      val processId = po.waitPromise()
      apis.destroyProcess(processId)
    }

  }
}


