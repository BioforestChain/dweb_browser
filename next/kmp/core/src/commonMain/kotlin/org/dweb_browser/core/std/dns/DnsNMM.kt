package org.dweb_browser.core.std.dns

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.dweb_browser.core.help.buildRequestX
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MPID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.ConnectResult
import org.dweb_browser.core.module.DnsMicroModule
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.connectMicroModules
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.toJsonElement

val debugDNS = Debugger("dns")

class DnsNMM : NativeMicroModule("dns.std.dweb", "Dweb Name System") {
  init {
    dweb_deeplinks = listOf("dweb://open")
    short_name = "DNS";
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Routing_Service);
  }

  /**
   * 所有应用列表，这里基于 MMID 存储，ChangeableMap 提供变动监听功能
   */
  private val allApps = ChangeableMap<MMID, MicroModule>()

  /**
   * 所有应用和协议列表，这里基于 MMID 与 Protocol 存储
   */
  private val _installApps = mutableMapOf<MPID, MutableSet<MicroModule>>()
  private fun installApps(mpid: MPID) = _installApps.getOrElse(mpid) { emptySet() }
  private fun addInstallApps(mpid: MPID, app: MicroModule) {
    when (val beforeMms = _installApps[mpid]) {
      null -> _installApps[mpid] = mutableSetOf(app)
      else -> beforeMms += app
    }
  }

  private fun removeInstallApps(mpid: MPID, app: MicroModule) =
    when (val beforeMms = _installApps[mpid]) {
      null -> false
      else -> beforeMms.remove(app)
    }


  private val _runningApps =
    ChangeableMap</* mmid or dweb-protocol */MPID, Map</*真正的 realMmid*/MMID, RunningApp>>() // 正在运行的应用

  private class RunningApp(val module: MicroModule, private val afterBootstrap: PromiseOut<Unit>) {
    suspend fun ready(): MicroModule {
      afterBootstrap.waitPromise()
      return module
    }
  }

  private fun runningApps(mpid: MPID) = _runningApps.getOrPut(mpid) { mapOf() }

  private fun addRunningApp(runningApp: RunningApp) {
    val mmid = runningApp.module.mmid
    fun add(mpid: MPID) {
      val apps = runningApps(mpid)
      if (!apps.containsKey(mmid)) {
        _runningApps[mpid] = apps + (mmid to runningApp)
      }
    }
    add(runningApp.module.mmid)
    runningApp.module.dweb_protocols.forEach { add(it) }
  }

  private fun removeRunningApp(runningApp: RunningApp) {
    val mmid = runningApp.module.mmid
    fun remove(mpid: MPID) {
      val apps = runningApps(mpid)
      if (apps.containsKey(mmid)) {
        _runningApps[mmid] = apps.filter { it.key != mmid }
      }
    }
    remove(runningApp.module.mmid)
    runningApp.module.dweb_protocols.forEach { remove(it) }
  }

  /**
   * 根据mmid获取模块
   * TODO 一个MMID被多个模块同时实现时，需要提供选择器
   */
  private fun getPreferenceApp(mpid: MPID, fromMM: IMicroModuleManifest) =
    installApps(mpid).firstOrNull { it.mmid != fromMM.mmid }

  suspend fun bootstrap() {
    if (!this.running) {
      bootstrapMicroModule(this)
    }
  }

  @JvmInline
  private value class MM(private val fromTo: String) {
    constructor(fromMMID: MMID, toMMID: MMID) : this("$fromMMID $toMMID")

    private val fromMMID get() = fromTo.substring(0..fromTo.indexOf(' '))
    private val toMMID get() = fromTo.substring(fromTo.indexOf(' ') + 1)
  }

  /** 对等连接列表 */
  private val mmConnectsMap = mutableMapOf<MM, PromiseOut<ConnectResult>>()
  private val mmConnectsMapLock = Mutex()

  /** 为两个mm建立 ipc 通讯 */
  private suspend fun connectTo(
    fromMM: MicroModule, toMPID: MPID, reason: PureRequest
  ) = mmConnectsMapLock.withLock {
    val toRunningApp = _open(toMPID, fromMM)
    val toMM = toRunningApp.module
    val aKey = MM(fromMM.mmid, toMM.mmid)
    mmConnectsMap[aKey] ?: run {
      val bKey = MM(toMM.mmid, fromMM.mmid)
      val aPromiseOut = PromiseOut<ConnectResult>()
      val bPromiseOut = PromiseOut<ConnectResult>()
      mmConnectsMap[aKey] = aPromiseOut
      mmConnectsMap[bKey] = bPromiseOut
      aPromiseOut.alsoLaunchIn(ioAsyncScope) {
        debugDNS("connect", "${fromMM.mmid} <=> $toMPID/${toMM.mmid}")
        toRunningApp.ready()
        val aConnectResult = connectMicroModules(fromMM, toMM, reason)
        val bConnectResult = ConnectResult(aConnectResult.ipcForToMM, aConnectResult.ipcForFromMM)
        bPromiseOut.resolve(bConnectResult)
        aConnectResult.ipcForFromMM.onClose {
          mmConnectsMap.remove(aKey)
          mmConnectsMap.remove(bKey)
        }
        aConnectResult
      }
    }
  }.waitPromise()

  class MyDnsMicroModule(private val dnsMM: DnsNMM, private val fromMM: MicroModule) :
    DnsMicroModule {
    override fun install(mm: MicroModule) {
      // TODO 作用域保护
      dnsMM.install(mm)
    }

    override suspend fun uninstall(mmid: MMID): Boolean {
      // TODO 作用域保护
      return dnsMM.uninstall(mmid)
    }

    override fun query(mmid: MMID): MicroModule? {
      return dnsMM.query(mmid, fromMM)
    }

    override suspend fun search(category: MICRO_MODULE_CATEGORY): MutableList<MicroModule> {
      return dnsMM.search(category)
    }

    // 调用重启
    override suspend fun restart(mmid: MMID) {
      // 关闭后端连接
      dnsMM.close(mmid)
      dnsMM.open(mmid, fromMM)
    }

    override suspend fun connect(
      mmid: MMID, reason: PureRequest?
    ): ConnectResult {
      // TODO 权限保护
      return dnsMM.connectTo(
        fromMM, mmid, reason ?: PureRequest("file://$mmid", IpcMethod.GET)
      )
    }

    override suspend fun open(mmid: MMID): Boolean {
      if (this.dnsMM.runningApps(mmid).isEmpty()) {
        dnsMM.open(mmid, fromMM)
        return true
      }
      return false
    }

    override suspend fun close(mmid: MMID): Boolean {
      if (this.dnsMM.runningApps(mmid).isNotEmpty()) {
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
    addRunningApp(RunningApp(this, PromiseOut.resolve(Unit)))

    /**
     * 对全局的自定义路由提供适配器
     * 对 nativeFetch 定义 file://xxx.dweb的解析
     */
    nativeFetchAdaptersManager.append { fromMM, request ->
      if (request.url.protocol.name == "file" && request.url.host.endsWith(".dweb")) {
        val mpid = request.url.host
        debugDNS("fetch ipc", "$fromMM => ${request.href}")
        val url = request.href
        val reasonRequest = buildRequestX(url, request.method, request.headers, request.body);
        if (installApps(mpid).isNotEmpty()) {
          val (fromIpc) = connectTo(fromMM, mpid, reasonRequest)
          fromIpc.request(request)
        } else PureResponse(HttpStatusCode.BadGateway, body = PureStringBody(url))
      } else null
    }.removeWhen(this.onAfterShutdown)
    /** dwebDeepLink 适配器*/
    nativeFetchAdaptersManager.append { fromMM, request ->
      if (request.href.startsWith("dweb:")) {
        debugDNS("fetch deeplink", "$fromMM => ${request.href}")
        for (microModule in allApps.values) {
          for (deeplink in microModule.dweb_deeplinks) {
            if (request.href.startsWith(deeplink)) {
              val (fromIpc) = connectTo(fromMM, microModule.mmid, request)
              return@append fromIpc.request(request)
            }
          }
        }
        return@append PureResponse(
          HttpStatusCode.BadGateway, body = PureStringBody(request.href)
        )
      } else null
    }.removeWhen(this.onAfterShutdown)

    val queryAppId = PureRequest.query("app_id")
    val openApp = defineBooleanResponse {
      val mmid = request.queryAppId()
      debugDNS("open/$mmid", request.url.fullPath)
      open(mmid)
      true
    }
    /// 定义路由功能
    routes("open" bindDwebDeeplink openApp,
      // 打开应用
      "/open" bind HttpMethod.Get to openApp,
      // 关闭应用
      // TODO 能否关闭一个应该应该由应用自己决定
      "/close" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.queryAppId()
        debugDNS("close/$mmid", request.url.fullPath)
        close(mmid)
        true
      },
      //
      "/query" bind HttpMethod.Get to defineJsonResponse {
        val mmid = request.queryAppId()
        Json.encodeToString("")
        query(mmid, ipc.remote)?.toManifest()?.toJsonElement() ?: JsonNull
      },
      //
      "/observe/install-apps" bind HttpMethod.Get to defineJsonLineResponse {
        allApps.onChange { changes ->
          emit(
            ChangeState(
              changes.adds, changes.updates, changes.removes
            )
          )
        }.removeWhen(onDispose)
      },
      //
      "/observe/running-apps" bind HttpMethod.Get to defineJsonLineResponse {
        _runningApps.onChange { changes ->
          emit(
            ChangeState(
              changes.adds, changes.updates, changes.removes
            )
          )
        }
      })
    /// 启动 boot 模块
    val bootIpc = connect("boot.sys.dweb");
    bootIpc.postMessage(IpcEvent.createActivity(""))
  }

  override suspend fun _shutdown() {
    allApps.values.forEach {
      it.shutdown()
    }
    allApps.clear()
    _installApps.clear()
    ioAsyncScope.cancel()
  }

  /** 安装应用 */
  fun install(mm: MicroModule) {
    allApps[mm.mmid] = mm
    addInstallApps(mm.mmid, mm)
    for (protocol in mm.dweb_protocols) {
      addInstallApps(protocol, mm)
    }
  }

  /** 卸载应用 */
  suspend fun uninstall(mmid: MMID): Boolean {
    val mm = allApps.remove(mmid) ?: return false
    /// 首先进行关闭
    close(mmid)
    /// 执行销毁的生命周期函数
    mm.dispose()
    removeInstallApps(mmid, mm)
    for (protocol in mm.dweb_protocols) {
      removeInstallApps(protocol, mm)
    }
    return true
  }

  /** 查询应用 */
  fun query(mmid: MMID, fromMM: IMicroModuleManifest): MicroModule? {
    return getPreferenceApp(mmid, fromMM)
  }

  /**
   * 根据类目搜索模块
   * > 这里暂时不需要支持复合搜索，未来如果有需要另外开接口
   * @param category
   */
  fun search(category: MICRO_MODULE_CATEGORY): MutableList<MicroModule> {
    val categoryList = mutableListOf<MicroModule>()
    for (app in allApps.values) {
      if (app.categories.contains(category)) {
        categoryList.add(app)
      }
    }
    return categoryList
  }

  private val openLock = SynchronizedObject()

  /** 打开应用
   */
  private fun _open(mpid: MPID, fromMM: MicroModule) = synchronized(openLock) {
    runningApps(mpid).firstNotNullOfOrNull {
      // 这里基于传入 fromMmid，从而才能做到 protocol 模块直接寻址 superMM
      // 首先 it.key != mpid 意味着这个mpid是一个protocol
      // 在此基础上，it.key == fromMM.mmid 意味着它循环引用到自己的protocol了，这没必要，所以返回null
      if (it.key != mpid && it.key == fromMM.mmid) null else it.value
    } ?: run {
      debugDNS("open start", "$mpid(by ${fromMM.mmid})")
      val app = query(mpid, fromMM) ?: throw Exception("no found app: $mpid")
      val afterBootstrap = PromiseOut<Unit>().alsoLaunchIn(ioAsyncScope) {
        bootstrapMicroModule(app)
        debugDNS("open end", "$mpid(by ${fromMM.mmid})")
      }
      RunningApp(app, afterBootstrap).also { running ->
        addRunningApp(running)
        app.onAfterShutdown {
          removeRunningApp(running)
        }
      }
    }
  }


  suspend fun open(mmid: MMID, fromMM: MicroModule = this) = _open(mmid, fromMM).ready()

  /** 关闭应用 */
  suspend fun close(mmid: MMID): Int {
    return runningApps(mmid).let { apps ->
      var count = 0;
      apps.filter { it.key == mmid }.forEach {
        it.value.ready().shutdown();
        count += 1
      }
      count
    }
  }
}



