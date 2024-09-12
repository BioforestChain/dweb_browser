package org.dweb_browser.browser.jmm

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jsProcess.ext.JsProcess
import org.dweb_browser.browser.jsProcess.ext.createJsProcess
import org.dweb_browser.browser.kit.GlobalWebMessageEndpoint
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.help.types.IpcSupportProtocols
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.http.router.HttpHandlerToolkit
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.connectAdapterManager
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.core.std.permission.ext.doRequestWithPermissions
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.printError
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureRequest
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.sys.toast.ext.showToast

val debugJsMM = Debugger("JsMM")

open class JsMicroModule(val metadata: JmmAppInstallManifest) :
  MicroModule(MicroModuleManifest().apply {
    assign(metadata)
    categories += MICRO_MODULE_CATEGORY.Application
    icons.ifEmpty {
      icons = listOf(ImageResource(src = metadata.logo))
    }
    mmid = metadata.id
    ipc_support_protocols = IpcSupportProtocols(
      cbor = true, protobuf = false, json = true
    )
    targetType = "jmm"
  }) {
  override fun toString(): String {
    return "JMM($mmid)"
  }

  companion object {
    /**
     * 当前JsMicroModule的版本
     */
    const val VERSION = 3
    const val PATCH = 2

    init {
      val nativeToWhiteList =
        listOf<MMID>("js.browser.dweb", "file.std.dweb", "permission.sys.dweb")

      data class MmDirection(
        val startMm: MicroModule,
        val endJmm: JsMicroModule.JmmRuntime,
      )
      // jsMM对外创建ipc的适配器，给DnsNMM的connectMicroModules使用
      connectAdapterManager.append(1) { fromMM, toMM, reason ->
        val jsMM = if (nativeToWhiteList.contains(toMM.mmid)) null
        /// 这里优先判断 toMM 是否是 endJmm
        else if (toMM is JsMicroModule.JmmRuntime) MmDirection(fromMM, toMM)
        else if (fromMM is JsMicroModule) MmDirection(toMM.microModule, fromMM.runtime)
        else null

        jsMM?.let {
          debugJsMM("JsMM/connectAdapter") {
            "fromMM:${fromMM.mmid} => toMM:${toMM.mmid} ==> jsMM:$jsMM"
          }
          /**
           * 与 NMM 相比，这里会比较难理解：
           * 因为这里是直接创建一个 Native2JsIpc 作为 ipcForFromMM，
           * 而实际上的 ipcForToMM ，是在 js-context 里头去创建的，因此在这里是 一个假的存在
           *
           * 也就是说。如果是 jsMM 内部自己去执行一个 connect，那么这里返回的 ipcForFromMM，其实还是通往 js-context 的， 而不是通往 toMM的。
           * 也就是说，能跟 toMM 通讯的只有 js-context，这里无法通讯。
           */
          val toJmmIpc = jsMM.endJmm.ipcBridge(jsMM.startMm) //(tip:创建到worker内部的桥接)
          toMM.beConnect(toJmmIpc, reason)
          toJmmIpc
        }
      }

      nativeFetchAdaptersManager.append(order = 1) { fromMM, request ->
        if (fromMM is JmmRuntime && request.href.startsWith("dweb:")) {
          val toMM =
            fromMM.bootstrapContext.dns.queryDeeplink(request.href) ?: return@append PureResponse(
              HttpStatusCode.BadGateway, body = PureStringBody(request.href)
            )

          if (!nativeToWhiteList.contains(toMM.mmid)) {
            val fromIpc = fromMM.getJsProcess().fetchIpc
            fromMM.debugMM("proxy-deeplink") { "${fromMM.mmid} => ${request.href}" }
            fromMM.doRequestWithPermissions { fromIpc.request(request) }
          } else null
        } else null
      }
      nativeFetchAdaptersManager.append(order = 1.1f) { fromMM, request ->
        if (fromMM is JmmRuntime && request.url.protocol.name == "file" && request.url.host.endsWith(
            ".dweb"
          )
        ) {
          val mpid = request.url.host
          val mmid = fromMM.bootstrapContext.dns.query(mpid)?.mmid ?: return@append null
          if (!nativeToWhiteList.contains(mmid)) {
            val fromIpc = fromMM.getJsProcess().fetchIpc
            fromMM.debugMM("roxy-request") { "${fromMM.mmid} => ${request.href}" }
            fromMM.doRequestWithPermissions { fromIpc.request(request) }
            null
          } else null
        } else null
      }
    }
  }

  override suspend fun getSafeDwebPermissionProviders() =
    this.dweb_permissions.mapNotNull { PermissionProvider.from(this, it, metadata.bundle_url) }

  open inner class JmmRuntime(override val bootstrapContext: BootstrapContext) : Runtime() {
    private val jsProcessDeferred = CompletableDeferred<JsProcess>()
    suspend fun getJsProcess() = jsProcessDeferred.await()

    open val esmLoader: HttpHandlerToolkit.() -> Unit = {
      val serverRoot = metadata.server.root.trimEnd('/')
      "/" bindPrefix PureMethod.GET by definePureResponse {
        // 将多个 '/' 转为单个
        val url = "file://" + (serverRoot + request.url.encodedPath).replace(Regex("/{2,}"), "/")
        debugMM("esmLoader", url)
        nativeFetch(url)
      }
    }

    override suspend fun _bootstrap() {
      scopeLaunch(cancelable = false) {
        startJsProcess()
      }
    }

    private suspend fun startJsProcess() {
      debugJsMM("bootstrap...") {
        "$mmid/ minTarget:${metadata.minTarget} maxTarget:${metadata.maxTarget}"
      }
      val errorMessage = metadata.canSupportTarget(VERSION, disMatchMinTarget = {
        BrowserI18nResource.JsMM.canNotSupportMinTarget
      }, disMatchMaxTarget = {
        BrowserI18nResource.JsMM.canNotSupportMaxTarget
      })
      errorMessage?.also { i18nMsg ->
        scopeLaunch(cancelable = true) {
          showToast(i18nMsg.text {
            appId = mmid
            currentVersion = VERSION
            minTarget = metadata.minTarget
            maxTarget = metadata.maxTarget ?: metadata.minTarget
          })
        }
      }
      val jsProcess = createJsProcess(metadata.server.entry, "$mmid-$short_name")
      jsProcessDeferred.complete(jsProcess)
      jsProcess.defineEsm(esmLoader)
      // 监听关闭事件
      jsProcess.fetchIpc.onClosed {
        tryShutdown()
      }

      fun proxyIpcTunnel(remoteMmid: MMID, key: String) = scopeLaunch(cancelable = true) {
        val conIpc = connect(remoteMmid)
        conIpc.start(await = false)
        val proxyIpc = jsProcess.fetchIpc.fork(remote = conIpc.remote)
        proxyIpc.start(await = false)
        conIpc.onEvent("$key~(event)~>proxy").collectIn(mmScope) { msgEvent ->
          proxyIpc.postMessage(msgEvent.consume())
        }
        proxyIpc.onEvent("proxy~(event)~>$key").collectIn(mmScope) { msgEvent ->
          conIpc.postMessage(msgEvent.consume())
        }
        conIpc.onRequest("$key~(request)~>proxy").collectIn(mmScope) { msgEvent ->
          val request = msgEvent.consume()
          val response = proxyIpc.request(request.toPure().toClient())
          proxyIpc.postResponse(request.reqId, response)
        }
        proxyIpc.onRequest("proxy~(request)~>$key").collectIn(mmScope) { msgEvent ->
          val request = msgEvent.consume()
          val response = conIpc.request(request.toPure().toClient())
          proxyIpc.postResponse(request.reqId, response)
        }
        /// 将pid发送给js
        jsProcess.fetchIpc.postMessage(IpcEvent.fromUtf8("$key-ipc-pid", proxyIpc.pid.toString()))
      }
      /// 提供file.std.dweb的绑定
      proxyIpcTunnel("file.std.dweb", "file")
      proxyIpcTunnel("permission.sys.dweb", "permission")
      /**
       * 收到 Worker 的事件，如果是指令，执行一些特定的操作
       */
      jsProcess.fetchIpc.onEvent("fetch-ipc-proxy-event").collectIn(mmScope) { event ->
        event.consumeFilter { ipcEvent ->
          /**
           * 收到要与其它模块进行ipc连接的指令
           */
          when (ipcEvent.name) {
            "dns/connect" -> {
              val connectMmid = ipcEvent.text
              debugMM("dns/connect", connectMmid)
              try {
                /**
                 * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
                 * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
                 *
                 * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
                 *
                 * 向目标模块发起连接，注意，这里是很特殊的，因为我们自定义了 JMM 的连接适配器 connectAdapterManager，
                 * 所以 JsMicroModule 这里作为一个中间模块，是没法直接跟其它模块通讯的。
                 *
                 * TODO 如果有必要，未来需要让 connect 函数支持 force 操作，支持多次连接。
                 */
                val targetIpc = connect(
                  connectMmid,
                  // 如果对方是 jmm，会认得这个reason，它就不会做 beConnect
                  // 而自己也是jmm，所以自己也不会执行 beConnect
                  PureClientRequest("file://$connectMmid/jmm/dns/connect", method = PureMethod.GET),
                ) // 由上面的适配器产生
                /// 只要不是我们自己创建的直接连接的通道，就需要我们去 创造直连并进行桥接
                val resultMmid: MMID
                if (targetIpc.locale.mmid == mmid) {
                  resultMmid = targetIpc.remote.mmid
                  when (val globalEndpoint = targetIpc.endpoint) {
                    is GlobalWebMessageEndpoint -> {
                      // 如果是jsMM相互连接，直接把port丢过去
                      jsProcess.bridgeIpc(globalEndpoint.globalId, targetIpc.remote)
                    }

                    else -> {
                      // 发现自己还是需要做 beConnect，但是这句代码目前不可能走进来。
                      beConnect(targetIpc, null)
                    }
                  }
                } else {
                  resultMmid = targetIpc.locale.mmid
                  // 发现自己还是需要做 beConnect
                  beConnect(targetIpc, null)
                }

                /**
                 * connectMmid 可能是子协议，所以result提供真正的mmid
                 *
                 * 连接成功，正式告知它数据返回。注意，create-ipc虽然也会resolve任务，但是我们还是需要一个明确的done事件，来确保逻辑闭环
                 * 否则如果遇到ipc重用，create-ipc是不会触发的
                 */
                @Serializable
                data class DnsConnectDone(val connect: MMID, val result: MMID)

                val done = DnsConnectDone(connect = connectMmid, result = resultMmid)
                jsProcess.fetchIpc.postMessage(
                  IpcEvent.fromUtf8(
                    "dns/connect/done", Json.encodeToString(done)
                  )
                )
              } catch (e: Exception) {
                printError("dns/connect", null, e)
                @Serializable
                data class DnsConnectError(val connect: MMID, val reason: String)

                val error = DnsConnectError(
                  connect = connectMmid,
                  reason = e.message ?: "unknown error reason to connect $connectMmid"
                )
                jsProcess.fetchIpc.postMessage(
                  IpcEvent.fromUtf8(
                    "dns/connect/error", Json.encodeToString(error)
                  )
                )
              }
              true
            }

            else -> false
          }
        }
      }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun connect(remoteMmid: MMID, reason: PureRequest?): Ipc {
      val ipc = super.connect(remoteMmid, reason)
      if (reason?.url?.encodedPath == "/jmm/dns/connect") {// && reason.url.host == mmid
        connectionMap[remoteMmid]?.also {
          if (it.isCompleted && it.getCompleted() == ipc) {
            connectionMap.remove(remoteMmid, it)
          }
        }
      }
      return ipc
    }

    override suspend fun beConnect(ipc: Ipc, reason: PureRequest?) {
      if (reason?.url?.encodedPath == "/jmm/dns/connect") {// && reason.url.host == mmid
        return
      }
      super.beConnect(ipc, reason)
    }

    internal suspend fun ipcBridge(fromMM: MicroModule) = getJsProcess().createIpc(fromMM.manifest)

    override suspend fun _shutdown() {
      debugJsMM("shutdown $mmid") {}
      val jsProcess = getJsProcess()
      jsProcess.codeIpc.close()
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = JmmRuntime(bootstrapContext)

  override val runtime get() = super.runtime as JmmRuntime

  override fun toManifest(): CommonAppManifest {
    return this.metadata.toCommonAppManifest()
  }
}