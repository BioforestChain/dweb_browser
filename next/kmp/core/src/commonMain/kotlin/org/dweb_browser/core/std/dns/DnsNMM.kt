package org.dweb_browser.core.std.dns

import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonNull
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MMPT
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.DnsApi
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.connectMicroModules
import org.dweb_browser.core.std.boot.BootNMM
import org.dweb_browser.core.std.dns.ext.createActivity
import org.dweb_browser.core.std.permission.ext.doRequestWithPermissions
import org.dweb_browser.core.std.permission.permissionAdapterManager
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ReentrantReadWriteLock
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.some
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureRequest
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.PureUrl

val debugDNS = Debugger("dns")

class DnsNMM : NativeMicroModule("dns.std.dweb", "Dweb Name System") {
  init {
    dweb_deeplinks = listOf("dweb://open")
    short_name = "DNS";
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Routing_Service);
  }

  class RunningApp(
    val module: MicroModule, private val afterBootstrap: Deferred<MicroModule.Runtime>,
  ) {
    suspend fun ready() = afterBootstrap.await()
  }


  class MyDnsApi(private val dnsMM: DnsNMM, private val fromMM: MicroModule) : DnsApi {
    override suspend fun install(mm: MicroModule) {
      // TODO 作用域保护
      dnsMM.install(mm)
    }

    override suspend fun uninstall(mmpt: MMPT): Boolean {
      // TODO 作用域保护
      return dnsMM.uninstall(mmpt)
    }

    override suspend fun query(mmpt: MMPT): MicroModule? {
      return dnsMM.queryByIdOrProtocol(mmpt, fromMM)
    }

    override suspend fun queryAll(mmpt: MMPT): List<MicroModule> {
      return dnsMM.queryAllByIdOrProtocol(mmpt, fromMM)
    }

    override suspend fun queryDeeplink(deeplinkUrl: String): MicroModule? {
      return dnsMM.queryByDeeplink(deeplinkUrl, fromMM)
    }

    override suspend fun queryDeeplinkAll(deeplinkUrl: String): List<MicroModule> {
      return dnsMM.queryAllByDeeplink(deeplinkUrl, fromMM)
    }

    override suspend fun search(category: MICRO_MODULE_CATEGORY): MutableList<MicroModule> {
      return dnsMM.search(category)
    }

    override suspend fun isRunning(mmid: MMID): Boolean {
      return dnsMM.runtime.isRunning(mmid)
    }

    // 调用重启
    override suspend fun restart(mmpt: MMPT) {
      // 关闭后端连接
      val num = dnsMM.runtime.close(mmpt)
      debugDNS("dns_restart", "restart $num $mmpt")
      dnsMM.runtime.open(mmpt, fromMM)
    }

    // TODO 权限保护
    override suspend fun connect(mmpt: MMPT, reason: PureRequest?): Ipc {
      // 找到要连接的模块
      val toMicroModule = dnsMM.queryByIdOrProtocol(mmpt, fromMM) ?: throw ResponseException(
        code = HttpStatusCode.NotFound, message = "not found app->$mmpt"
      )

      val toMMID = toMicroModule.mmid
      val fromMMID = fromMM.mmid
      debugDNS("connectTo") { "$fromMMID <=> $toMMID" }

      val toAppRuntime = dnsMM.dnsRuntime.open(toMicroModule.mmid, fromMM)
      debugDNS("connectTo/opened", toAppRuntime)
      return connectMicroModules(
        fromMM, toAppRuntime, reason ?: PureClientRequest("file://$mmpt", PureMethod.GET)
      )
    }

    override suspend fun open(mmpt: MMPT): Boolean {
      if (this.dnsMM.getRunningApps(mmpt).isEmpty()) {
        return runCatching {
          dnsMM.runtime.open(mmpt, fromMM)
          true
        }.getOrDefault(false)
      }
      return true
    }

    override suspend fun close(mmpt: MMPT): Boolean {
      if (this.dnsMM.getRunningApps(mmpt).isNotEmpty()) {
        return runCatching {
          dnsMM.runtime.close(mmpt)
          true
        }.getOrDefault(false)
      }
      return false
    }
  }

  class MyBootstrapContext(override val dns: MyDnsApi) : BootstrapContext {}

  /**
   * 所有应用列表，这里基于 MMID 存储，ChangeableMap 提供变动监听功能
   */
  private val allApps = ChangeableMap<MMID, MicroModule>()

  /**
   * 所有应用和协议列表，这里基于 MMID 与 Protocol 存储
   */
  private val _installApps = mutableMapOf<MMPT, MutableSet<MicroModule>>()
  private fun queryInstallApps(mpid: MMPT) = _installApps.getOrElse(mpid) { emptySet() }
  private fun addInstallApps(mmpt: MMPT, app: MicroModule) {
    when (val beforeMms = _installApps[mmpt]) {
      null -> _installApps[mmpt] = mutableSetOf(app)
      else -> beforeMms += app
    }
  }

  private fun removeInstallApps(mmpt: MMPT, app: MicroModule) =
    when (val beforeMms = _installApps[mmpt]) {
      null -> false
      else -> beforeMms.remove(app)
    }


  private val runningAppLock = SynchronizedObject()
  private val runningApps =
    ChangeableMap</* mmid or dweb-protocol */MMPT, Map</*真正的 realMmid*/MMID, RunningApp>>() // 正在运行的应用

  private val dnsRuntime get() = runtime as DnsRuntime

  internal fun getRunningApps(mmpt: MMPT) = runningApps.getOrPut(mmpt) { mapOf() }

  internal fun addRunningApp(runningApp: RunningApp) = synchronized(runningAppLock) {
    val mmid = runningApp.module.mmid
    debugDNS("add-running", mmid)

    for (mmpt in runningApp.module.getMmptList()) {
      val apps = getRunningApps(mmpt)
      if (!apps.containsKey(mmid)) {
        runningApps[mmpt] = apps + (mmid to runningApp)
      }
    }
  }

  internal fun removeRunningApp(runningApp: RunningApp) = synchronized(runningAppLock) {
    val mmid = runningApp.module.mmid
    debugDNS("remove-running", mmid)

    for (mmpt in runningApp.module.getMmptList()) {
      val apps = getRunningApps(mmpt)
      if (apps.containsKey(mmid)) {
        runningApps[mmid] = apps.filter { it.key != mmid }
      }
    }
  }


  private val installLock = ReentrantReadWriteLock()

  /** 安装应用 */
  suspend fun install(mm: MicroModule): Boolean = installLock.write {
    if (allApps.containsKey(mm.mmid)) {
      return false
    }
    allApps[mm.mmid] = mm
    addInstallApps(mm.mmid, mm)
    for (protocol in mm.dweb_protocols) {
      addInstallApps(protocol, mm)
    }
    for (provider in mm.getSafeDwebPermissionProviders()) {
      permissionAdapterManager.append(adapter = provider)
    }
    return true
  }

  /** 卸载应用 */
  suspend fun uninstall(mmid: MMID): Boolean = installLock.write {
    val mm = allApps.remove(mmid) ?: return false
    /// 首先进行关闭
    dnsRuntime.close(mmid)
    removeInstallApps(mmid, mm)
    for (protocol in mm.dweb_protocols) {
      removeInstallApps(protocol, mm)
    }
    return true
  }

  /** 根据mmid查询偏好模块 */
  suspend fun queryByIdOrProtocol(mmid: MMID, fromMM: IMicroModuleManifest): MicroModule? =
    installLock.read {
      return queryInstallApps(mmid).map { it to if (it.mmid != fromMM.mmid) 1 else 0 }
        .maxByOrNull { it.second }?.first
    }

  /** 根据mmid查询所有模块 */
  suspend fun queryAllByIdOrProtocol(mmid: MMID, fromMM: IMicroModuleManifest): List<MicroModule> =
    installLock.read {
      return queryInstallApps(mmid).map { it to if (it.mmid != fromMM.mmid) 1 else 0 }
        .sortedBy { it.second }.map { it.first }
    }

  suspend fun queryByDeeplink(href: String, fromMM: IMicroModuleManifest): MicroModule? =
    installLock.read {
      return allApps.values.firstOrNull { microModule ->
        microModule.dweb_deeplinks.some { deeplink ->
          href.startsWith(deeplink)
        }
      }
    }

  suspend fun queryAllByDeeplink(href: String, fromMM: IMicroModuleManifest): List<MicroModule> =
    installLock.read {
      return allApps.values.filter { microModule ->
        microModule.dweb_deeplinks.some { deeplink ->
          href.startsWith(deeplink)
        }
      }
    }

  /**
   * 根据类目搜索模块
   * > 这里暂时不需要支持复合搜索，未来如果有需要另外开接口
   * @param category
   */
  suspend fun search(category: MICRO_MODULE_CATEGORY): MutableList<MicroModule> = installLock.read {
    val categoryList = mutableListOf<MicroModule>()
    for (app in allApps.values) {
      if (app.categories.contains(category)) {
        categoryList.add(app)
      }
    }
    return categoryList
  }

  inner class DnsRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {


    override suspend fun _bootstrap() {
      install(this@DnsNMM)
      addRunningApp(RunningApp(this@DnsNMM, CompletableDeferred(this)))

      /**
       * dwebDeepLink 适配器
       */
      nativeFetchAdaptersManager.append(order = 0) { fromMM, request ->
        if (request.href.startsWith("dweb:")) {
          val toMM =
            fromMM.bootstrapContext.dns.queryDeeplink(request.href) ?: return@append PureResponse(
              HttpStatusCode.BadGateway, body = PureStringBody(request.href)
            )
          return@append fromMM.connect(toMM.mmid, request).let { ipc ->
            fromMM.doRequestWithPermissions { ipc.request(request) }
          }
        } else null
      }.removeWhen(this.mmScope)

      val queryAppId = PureUrl.query("app_id")
      val queryCategory = PureUrl.query("category")
      val queryDeepLink = PureUrl.query("deeplink")
      val openApp = defineBooleanResponse {
        val mmid = request.queryAppId()
        debugDNS("open/$mmid", request.url.fullPath)
        open(mmid)
        true
      }
      /// 定义路由功能
      routes("open" bindDwebDeeplink openApp,
        // 打开应用
        "/open" bind PureMethod.GET by openApp,
        "/install" bind PureMethod.GET by defineEmptyResponse {
          val mmid = request.queryAppId()
          queryByIdOrProtocol(mmid, ipc.remote)?.let {
            install(it)
          }
        },
        "/uninstall" bind PureMethod.GET by defineBooleanResponse {
          val mmid = request.queryAppId()
          uninstall(mmid)
        },
        // 关闭应用
        // TODO 能否关闭一个应该应该由应用自己决定
        "/close" bind PureMethod.GET by defineBooleanResponse {
          val mmid = request.queryAppId()
          debugDNS("close/$mmid", request.url.fullPath)
          close(mmid)
          true
        },
        "/restart" bind PureMethod.GET by defineEmptyResponse {
          val mmid = request.queryAppId()
          val restartMMID = (request.queryOrNull("app_id") as MMID?) ?: ipc.remote.mmid
          scopeLaunch(cancelable = true) {
            debugDNS("restart", "start")
            val num = close(restartMMID)
            debugDNS("restart", "closed mmid=$restartMMID code=$num")
            val mm = open(mmid)
            debugDNS("restart", "opened mmid=$restartMMID mm=$mm")
          }
        },
        //
        "/query" bind PureMethod.GET by defineJsonResponse {
          val mmid = request.queryAppId()
          queryByIdOrProtocol(mmid, ipc.remote)?.toManifest()?.toJsonElement() ?: JsonNull
        },
        "/queryDeeplink" bind PureMethod.GET by defineJsonResponse {
          queryByDeeplink(request.queryDeepLink(), ipc.remote)?.toManifest().toJsonElement()
        },
        "/search" bind PureMethod.GET by defineJsonResponse {
          val category = request.queryCategory()
          val manifests = mutableListOf<CommonAppManifest>()
          search(category as MICRO_MODULE_CATEGORY).map { app ->
            manifests.add(app.toManifest())
          }
          manifests.toJsonElement()
        },
        //
        "/observe/install-apps" byChannel { ctx ->
          debugDNS("/observe/install-apps", "byChannel")
          allApps.onChange { changes ->
            debugDNS(
              "allApps",
              "onChange adds: ${changes.adds} updates: ${changes.updates} removes: ${changes.removes}"
            )
            ctx.sendJsonLine(
              ChangeState(
                changes.adds, changes.updates, changes.removes
              )
            )
          }.removeWhen(onClose)
          ctx.sendJsonLine(ChangeState(setOf<String>(), setOf(), setOf()))
        },
        //
        "/observe/running-apps" byChannel { ctx ->
          runningApps.onChange { changes ->
            ctx.sendJsonLine(
              ChangeState(
                changes.adds, changes.updates, changes.removes
              )
            )
          }.removeWhen(onClose)
        })
    }

    suspend fun boot(bootNMM: BootNMM) {
      /// 启动 boot 模块
      val bootIpc = connect(bootNMM.mmid);
      bootIpc.postMessage(IpcEvent.createActivity(""))
    }

    override suspend fun _shutdown() {
      allApps.values.forEach {
        if (it !== this@DnsNMM) {
          it.runtimeOrNull?.shutdown()
        }
      }
    }

    private val openLock = Mutex()

    /** 打开应用
     */
    suspend fun open(mmpt: MMPT, fromMM: IMicroModuleManifest = this) = openLock.withLock {
      getRunningApps(mmpt).firstNotNullOfOrNull {
        // 这里基于传入 fromMmid，从而才能做到 protocol 模块直接寻址 superMM
        // 首先 it.key != mpid 意味着这个mpid是一个protocol
        // 在此基础上，it.key == fromMM.mmid 意味着它循环引用到自己的protocol了，这没必要，所以返回null
        if (it.key != mmpt && it.key == fromMM.mmid) null else it.value
      } ?: run {
        debugDNS("dns_open", "$mmpt(by ${fromMM.mmid})")
        val app = queryByIdOrProtocol(mmpt, fromMM) ?: throw ResponseException(
          code = HttpStatusCode.NotFound, message = "no found app: $mmpt"
        )
        RunningApp(app, scopeAsync(cancelable = false) {
          bootstrapMicroModule(app)
        }).also { running ->
          addRunningApp(running)
          scopeLaunch(cancelable = false) {
            runCatching {
              running.ready().also { appRuntime ->
                appRuntime.onShutdown {
                  removeRunningApp(running)
                }
              }
            }.getOrElse {
              // 启动失败，移除running
              removeRunningApp(running)
              // 提示错误信息
              scopeLaunch(cancelable = true) {
                it.message?.also { msg ->
                  debugDNS("open", mmpt, it)
                  nativeFetch(buildUrlString("file://toast.sys.dweb/show") {
                    parameters["message"] = msg
                  })
                }
              }
            }
          }
        }
      }
    }.ready()


    /** 关闭应用 */
    suspend fun close(mmid: MMID) = openLock.withLock {
      getRunningApps(mmid).let { apps ->
        var count = 0;
        apps.filter { it.key == mmid }.forEach {
          it.value.ready().shutdown()
          count += 1
        }
        count
      }
    }

    suspend fun isRunning(mmid: MMID) = openLock.withLock {
      getRunningApps(mmid).filter { it.key == mmid }.isNotEmpty()
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = DnsRuntime(bootstrapContext)

  private suspend fun bootstrapMicroModule(fromMM: MicroModule) =
    fromMM.bootstrap(MyBootstrapContext(MyDnsApi(this, fromMM)))

  override val runtime get() = super.runtime as DnsRuntime
  suspend fun bootstrap() = bootstrapMicroModule(this) as DnsRuntime

  suspend fun reset() {
    runtimeOrNull?.shutdown()
    allApps.clear()
    _installApps.clear()
  }
}
