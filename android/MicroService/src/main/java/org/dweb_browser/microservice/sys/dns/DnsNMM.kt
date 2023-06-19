package org.dweb_browser.microservice.sys.dns

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.http4k.core.*
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.ConnectResult
import org.dweb_browser.microservice.core.DnsMicroModule
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.core.connectMicroModules
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.message.IpcEvent

fun debugDNS(tag: String, msg: Any = "", err: Throwable? = null) =
  printdebugln("fetch", tag, msg, err)

class DnsNMM : NativeMicroModule("dns.sys.dweb") {
  override val dweb_deeplinks = mutableListOf<DWEB_DEEPLINK>("dweb:open")
  private val installApps = mutableMapOf<Mmid, MicroModule>() // 已安装的应用
  private val runningApps = mutableMapOf<Mmid, PromiseOut<MicroModule>>() // 正在运行的应用

  suspend fun bootstrap() {
    if (!this.running) {
      bootstrapMicroModule(this)
    }
    onActivity()
  }


  data class MM(val fromMmid: Mmid, val toMmid: Mmid) {
    companion object {
      val values = mutableMapOf<Mmid, MutableMap<Mmid, MM>>()
      fun from(fromMmid: Mmid, toMmid: Mmid) = values.getOrPut(fromMmid) { mutableMapOf() }
        .getOrPut(toMmid) { MM(fromMmid, toMmid) }
    }
  }

  /** 对等连接列表 */
  private val mmConnectsMap =
    mutableMapOf<MM, PromiseOut<ConnectResult>>()
  private val mmConnectsMapLock = Mutex()

  /** 为两个mm建立 ipc 通讯 */
  private suspend fun connectTo(
    fromMM: MicroModule, toMmid: Mmid, reason: Request
  ) = mmConnectsMapLock.withLock {
    val mmKey = MM.from(fromMM.mmid, toMmid)
    /**
     * 一个互联实例
     */
    mmConnectsMap.getOrPut(mmKey) {
      PromiseOut<ConnectResult>().also { po ->
        GlobalScope.launch(ioAsyncExceptionHandler) {
          val toMM = open(toMmid);
          debugFetch("DNS/connect", "${fromMM.mmid} => $toMmid")
          val connectResult = connectMicroModules(fromMM, toMM, reason)
          connectResult.ipcForFromMM.onClose {
            mmConnectsMapLock.withLock {
              mmConnectsMap.remove(mmKey)
            }
          }
          po.resolve(connectResult)

          // 如果可以，反向存储
          if (connectResult.ipcForToMM != null) {
            val mmKey2 = MM.from(toMmid, fromMM.mmid)
            mmConnectsMapLock.withLock {
              mmConnectsMap.getOrPut(mmKey2) {
                PromiseOut<ConnectResult>().also { po2 ->
                  val connectResult2 = ConnectResult(
                    connectResult.ipcForToMM,
                    connectResult.ipcForFromMM
                  );
                  connectResult2.ipcForFromMM.onClose {
                    mmConnectsMapLock.withLock {
                      mmConnectsMap.remove(mmKey2)
                    }
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
    override fun install(mm: MicroModule) {
      // TODO 作用域保护
      dnsMM.install(mm)
    }

    override fun uninstall(mm: MicroModule) {
      // TODO 作用域保护
      dnsMM.uninstall(mm)
    }

    override fun query(mmid: Mmid): MicroModule? {
      return dnsMM.query(mmid)
    }

    override fun restart(mmid: Mmid) {
      // 调用重启
      GlobalScope.launch(ioAsyncExceptionHandler) {
        // 关闭后端连接
        dnsMM.close(mmid)
        // TODO 防止启动过快出现闪屏
        delay(1000)
        dnsMM.open(mmid)
      }
    }

    override suspend fun connect(
      mmid: Mmid, reason: Request?
    ): ConnectResult {
      // TODO 权限保护
      return dnsMM.connectTo(
        fromMM, mmid, reason ?: Request(Method.GET, Uri.of("file://$mmid"))
      )
    }

    override suspend fun bootstrap(mmid: Mmid) {
      dnsMM.open(mmid)
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
    _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { fromMM, request ->
      if (request.uri.scheme == "file" && request.uri.host.endsWith(".dweb")) {
        val mmid = request.uri.host
        debugFetch(
          "DNS/fetchAdapter",
          "fromMM=${fromMM.mmid} >> requestMmid=$mmid: >> path=${request.uri.path} >> ${request.uri}"
        )
        installApps[mmid]?.let {
          val (fromIpc) = connectTo(fromMM, mmid, request)
          return@let fromIpc.request(request)
        } ?: Response(Status.BAD_GATEWAY).body(request.uri.toString())
      } else null
    })
    /** dwebDeepLink 适配器*/
    _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { fromMM, request ->
      if (request.uri.scheme == "dweb" && request.uri.host == "") {
        debugFetch(
          "DNS/webDeepLink",
          "path=${request.uri.path} host = ${request.uri.host}"
        )
        for (microModule in installApps) {
          if (microModule.value.dweb_deeplinks.contains("dweb:${request.uri.path}")) {
            val (fromIpc) = connectTo(fromMM, microModule.key, request)
            return@append fromIpc.request(request)
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
  }

  override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
    onActivity(event)
  }

  suspend fun onActivity(event: IpcEvent = IpcEvent.fromUtf8("activity", "")) {
    /// 启动 boot 模块
    open("boot.sys.dweb")
    connect("boot.sys.dweb").ipcForFromMM.postMessage(event)
  }

  override suspend fun _shutdown() {
    installApps.forEach {
      it.value.shutdown()
    }
    installApps.clear()
  }

  /** 安装应用 */
  fun install(mm: MicroModule) {
    installApps[mm.mmid] = mm
  }

  /** 卸载应用 */
  @OptIn(DelicateCoroutinesApi::class)
  fun uninstall(mm: MicroModule) {
    installApps.remove(mm.mmid)
    GlobalScope.launch(ioAsyncExceptionHandler) {
      close(mm.mmid)
    }
  }

  /** 查询应用 */
  fun query(mmid: Mmid): MicroModule? {
    return installApps[mmid]
  }

  /** 打开应用 */
  private fun _open(mmid: Mmid): PromiseOut<MicroModule> {
    return runningApps.getOrPut(mmid) {
      PromiseOut<MicroModule>().also { promiseOut ->
        query(mmid)?.also { openingMm ->
          GlobalScope.launch(ioAsyncExceptionHandler) {
            bootstrapMicroModule(openingMm)
            promiseOut.resolve(openingMm)
          }
        } ?: promiseOut.reject(NotFoundException("no found app: $mmid"))
      }
    }
  }

  suspend fun open(mmid: Mmid): MicroModule {
    return _open(mmid).waitPromise()
  }

  /** 关闭应用 */
  suspend fun close(mmid: Mmid): Int {
    return runningApps.remove(mmid)?.let { microModulePo ->
      runCatching {
        val microModule = microModulePo.waitPromise()
        microModule.shutdown()
        // 这里只需要移除
        mmConnectsMap.remove(MM.from(microModule.mmid, "js.browser.dweb"))
        mmConnectsMap.remove(MM.from("js.browser.dweb", microModule.mmid))
        1
      }.getOrDefault(0)
    } ?: -1
  }
}



