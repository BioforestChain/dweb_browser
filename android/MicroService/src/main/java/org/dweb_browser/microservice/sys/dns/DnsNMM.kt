package org.dweb_browser.microservice.sys.dns

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.ConnectResult
import org.dweb_browser.microservice.core.DnsMicroModule
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.core.connectMicroModules
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.InitRequest
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.microservice.help.buildRequestX
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.http4k.core.*
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugDNS(tag: String, msg: Any = "", err: Throwable? = null) =
  printdebugln("fetch", tag, msg, err)

class DnsNMM() : NativeMicroModule("dns.std.dweb", "Dweb Name System") {
  override val dweb_deeplinks = mutableListOf<DWEB_DEEPLINK>("dweb:open")
  override val short_name = "DNS";
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Routing_Service);

  private val installApps = ChangeableMap<MMID, MicroModule>() // 已安装的应用
  private val runningApps = mutableMapOf<MMID, PromiseOut<MicroModule>>() // 正在运行的应用

  suspend fun bootstrap() {
    if (!this.running) {
      bootstrapMicroModule(this)
    }
  }


  data class MM(val fromMMID: MMID, val toMMID: MMID) {
    companion object {
      val values = mutableMapOf<MMID, MutableMap<MMID, MM>>()
      fun from(fromMMID: MMID, toMMID: MMID) = values.getOrPut(fromMMID) { mutableMapOf() }
        .getOrPut(toMMID) { MM(fromMMID, toMMID) }
    }
  }

  /** 对等连接列表 */
  private val mmConnectsMap =
    mutableMapOf<MM, PromiseOut<ConnectResult>>()
  private val mmConnectsMapLock = Mutex()

  /** 为两个mm建立 ipc 通讯 */
  private suspend fun connectTo(
    fromMM: MicroModule, toMMID: MMID, reason: Request
  ) = mmConnectsMapLock.withLock {
    val mmKey = MM.from(fromMM.mmid, toMMID)
    /**
     * 一个互联实例
     */
    mmConnectsMap.getOrPut(mmKey) {
      PromiseOut<ConnectResult>().also { po ->
        GlobalScope.launch(ioAsyncExceptionHandler) {
          debugFetch("DNS/open", "${fromMM.mmid} => $toMMID")
          val toMM = open(toMMID)
          debugFetch("DNS/connect", "${fromMM.mmid} <=> $toMMID")
          val connectResult = connectMicroModules(fromMM, toMM, reason)
          connectResult.ipcForFromMM.onClose {
            mmConnectsMap.remove(mmKey)
          }
          po.resolve(connectResult)

          // 如果可以，反向存储
          if (connectResult.ipcForToMM != null) {
            val mmKey2 = MM.from(toMMID, fromMM.mmid)
            mmConnectsMapLock.withLock {
              mmConnectsMap.getOrPut(mmKey2) {
                PromiseOut<ConnectResult>().also { po2 ->
                  val connectResult2 = ConnectResult(
                    connectResult.ipcForToMM,
                    connectResult.ipcForFromMM
                  );
                  connectResult2.ipcForToMM?.onClose {
                    mmConnectsMap.remove(mmKey2)
                  }
                  po2.resolve(connectResult2)
                }
              }
            }
          }
        }
      }
    }

  }.waitPromise()

  class MyDnsMicroModule(private val dnsMM: DnsNMM, private val fromMM: MicroModule) :
    DnsMicroModule {

    override val onChange = dnsMM.installApps.onChange

    override fun install(mm: MicroModule) {
      // TODO 作用域保护
      dnsMM.install(mm)
    }

    override fun uninstall(mmid: MMID): Boolean {
      // TODO 作用域保护
      return dnsMM.uninstall(mmid)
    }

    override fun query(mmid: MMID): MicroModule? {
      return dnsMM.query(mmid)
    }

    override suspend fun search(category: MICRO_MODULE_CATEGORY): MutableList<MicroModuleManifest> {
      return dnsMM.search(category)
    }

    override suspend fun restart(mmid: MMID) {
      // 调用重启
      // 关闭后端连接
      dnsMM.close(mmid)
      dnsMM.open(mmid)
    }

    override suspend fun connect(
      mmid: MMID, reason: Request?
    ): ConnectResult {
      // TODO 权限保护
      return dnsMM.connectTo(
        fromMM, mmid, reason ?: Request(Method.GET, Uri.of("file://$mmid"))
      )
    }

    override suspend fun open(mmid: MMID): Boolean {
      if (this.dnsMM.runningApps[mmid] == null) {
        return false
      }
      dnsMM.open(mmid)
      return true
    }

    override suspend fun close(mmid: MMID): Boolean {
      if (this.dnsMM.runningApps[mmid] !== null) {
        dnsMM.close(mmid);
        return true;
      }
      return false;
    }
  }

  class MyBootstrapContext(override val dns: MyDnsMicroModule) : BootstrapContext {}

  suspend fun bootstrapMicroModule(fromMM: MicroModule) {
    fromMM.bootstrap(MyBootstrapContext(MyDnsMicroModule(this@DnsNMM, fromMM)))
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    install(this)
    runningApps[this.mmid] = PromiseOut.resolve(this)

    /**
     * 对全局的自定义路由提供适配器
     * 对 nativeFetch 定义 file://xxx.dweb的解析
     */
    this.onAfterShutdown(nativeFetchAdaptersManager.append { fromMM, request ->
      if (request.uri.scheme == "file" && request.uri.host.endsWith(".dweb")) {
        val mmid = request.uri.host
        debugFetch("DNS/nativeFetch", "$fromMM => ${request.uri}")
        val url = request.uri.toString();
        val reasonRequest =
          buildRequestX(url, InitRequest(request.method, request.headers, request.body));
        installApps[mmid]?.let {
          val (fromIpc) = connectTo(fromMM, mmid, reasonRequest)
          return@let fromIpc.request(request)
        } ?: Response(Status.BAD_GATEWAY).body(url)
      } else null
    })
    /** dwebDeepLink 适配器*/
    this.onAfterShutdown(nativeFetchAdaptersManager.append { fromMM, request ->
      if (request.uri.scheme == "dweb" && request.uri.host == "") {
        debugFetch("DPLink/nativeFetch", "$fromMM => ${request.uri}")
        for (microModule in installApps) {
          for (deeplink in microModule.value.dweb_deeplinks) {
            if (request.uri.toString().startsWith(deeplink)) {
              val (fromIpc) = connectTo(fromMM, microModule.key, request)
              return@append fromIpc.request(request)
            }
          }
        }
        return@append Response(Status.BAD_GATEWAY).body(request.uri.toString())
      } else null
    })


    val query_app_id = Query.string().required("app_id")

    /// 定义路由功能
    apiRouting = routes(
      // 打开应用
      "/open" bind Method.GET to defineHandler { request ->
        val mmid = query_app_id(request)
        debugDNS("open/$mmid", request.uri.path)
        open(mmid)
        true
      },
      // 关闭应用
      // TODO 能否关闭一个应该应该由应用自己决定
      "/close" bind Method.GET to defineHandler { request ->
        val mmid = query_app_id(request)
        debugDNS("close/$mmid", request.uri.path)
        close(mmid)
        true
      },
    )

    /// 启动 boot 模块
    connect("boot.sys.dweb").postMessage(IpcEvent.fromUtf8("activity", ""))
  }

  override suspend fun _shutdown() {
    installApps.forEach {
      it.value.shutdown()
    }
    installApps.clear()
  }

  /** 安装应用 */
  fun install(mm: MicroModule) {
    installApps.put(mm.mmid, mm)
  }

  /** 卸载应用 */
  @OptIn(DelicateCoroutinesApi::class)
  fun uninstall(mmid: MMID): Boolean {
    installApps.remove(mmid)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      close(mmid)
    }
    return true
  }

  /** 查询应用 */
  fun query(mmid: MMID): MicroModule? {
    return installApps[mmid]
  }

  /**
   * 根据类目搜索模块
   * > 这里暂时不需要支持复合搜索，未来如果有需要另外开接口
   * @param category
   */
  fun search(category: MICRO_MODULE_CATEGORY): MutableList<MicroModuleManifest> {
    val categoryList = mutableListOf<MicroModuleManifest>()
    for (app in this.installApps.values) {
      if (app.categories.contains(category)) {
        categoryList.add(app.toManifest())
      }
    }
    return categoryList
  }

  /** 打开应用 */
  private fun _open(mmid: MMID): PromiseOut<MicroModule> {
    return runningApps.getOrPut(mmid) {
      PromiseOut<MicroModule>().also { promiseOut ->
        query(mmid)?.also { openingMm ->
          GlobalScope.launch(ioAsyncExceptionHandler) {
            bootstrapMicroModule(openingMm)
            openingMm.onAfterShutdown {
              if (runningApps[mmid] !== null) {
                runningApps.remove(mmid)
              }
            }
            promiseOut.resolve(openingMm)
          }
        } ?: {
          this.runningApps.remove(mmid)
          promiseOut.reject(Exception("no found app: $mmid"))
        }
      }
    }
  }

  suspend fun open(mmid: MMID): MicroModule {
    return _open(mmid).waitPromise()
  }

  /** 关闭应用 */
  suspend fun close(mmid: MMID): Int {
    return runningApps.remove(mmid)?.let { microModulePo ->
      runCatching {
        val microModule = microModulePo.waitPromise()
        microModule.shutdown()
        1
      }.getOrDefault(0)
    } ?: -1
  }
}



