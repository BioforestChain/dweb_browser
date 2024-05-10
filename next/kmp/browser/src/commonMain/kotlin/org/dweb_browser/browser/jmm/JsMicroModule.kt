package org.dweb_browser.browser.jmm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.alsoLaunchIn
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.printError
import org.dweb_browser.pure.http.PureMethod

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
  }) {
  override fun toString(): String {
    return "JMM($mmid)"
  }

  companion object {
    /**
     * 当前JsMicroModule的版本
     */
    const val VERSION = 3
    const val PATCH = 0

    init {
      val nativeToWhiteList = listOf<MMID>("js.browser.dweb", "file.std.dweb")

      data class MmDirection(
        val endJmm: JsMicroModule.JmmRuntime, val startMm: MicroModule,
      )
      // jsMM对外创建ipc的适配器，给DnsNMM的connectMicroModules使用
      connectAdapterManager.append(1) { fromMM, toMM, reason ->

        val jsMM = if (nativeToWhiteList.contains(toMM.mmid)) null
        /// 这里优先判断 toMM 是否是 endJmm
        else if (toMM is JsMicroModule.JmmRuntime) MmDirection(toMM, fromMM)
        else if (fromMM is JsMicroModule) MmDirection(fromMM.runtime, toMM.microModule)
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
      debugJsMM(
        "bootstrap...", "$mmid/ minTarget:${metadata.minTarget} maxTarget:${metadata.maxTarget}"
      )
      val errorMessage = metadata.canSupportTarget(VERSION, disMatchMinTarget = {
        return@canSupportTarget "应用($mmid)与容器版本不匹配，当前版本:${VERSION}，应用最低要求:${metadata.minTarget}"
      }, disMatchMaxTarget = {
        return@canSupportTarget "应用($mmid)与容器版本不匹配，当前版本:${VERSION}，应用最高兼容到:${metadata.maxTarget}"
      })

      if (errorMessage !== null) {
        this.showMessage(errorMessage)
        throw RuntimeException(errorMessage, Exception("$short_name 无法启动"))
      }

      val jsProcess = createJsProcess(metadata.server.entry, "$mmid-$short_name")
      jsProcessDeferred.complete(jsProcess)
      jsProcess.defineEsm(esmLoader)

      // 监听关闭事件
      jsProcess.codeIpc.onClosed {
        scopeLaunch(cancelable = false) {
          shutdown()
        }
      }

      val fileIpc = connect("file.std.dweb")
      fileIpc.start(await = false)
      jsProcess.fetchIpc.onRequest("file").collectIn(mmScope) { event ->
        val ipcRequest = event.consume()
        val response = nativeFetch(ipcRequest.toPure().toClient())
        jsProcess.fetchIpc.postResponse(ipcRequest.reqId, response)
      }

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
                val targetIpc = connect(connectMmid) // 由上面的适配器产生
                /// 只要不是我们自己创建的直接连接的通道，就需要我们去 创造直连并进行桥接
                val resultMmid: MMID
                if (targetIpc.locale.mmid == mmid) {
                  resultMmid = targetIpc.remote.mmid
                  when (val globalEndpoint = targetIpc.endpoint) {
                    is GlobalWebMessageEndpoint -> {
                      jsProcess.bridgeIpc(globalEndpoint.globalId, targetIpc.remote)
                    }
                  }
                } else {
                  resultMmid = targetIpc.locale.mmid
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
                    "dns/connect/done",
                    Json.encodeToString(done)
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
                    "dns/connect/error",
                    Json.encodeToString(error)
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


    private val fromMMIDOriginIpcWM = mutableMapOf<MMID, CompletableDeferred<Ipc>>();

    internal suspend fun ipcBridge(fromMM: MicroModule) =
      fromMMIDOriginIpcWM.getOrPut(fromMM.mmid) {
        CompletableDeferred<Ipc>().alsoLaunchIn(mmScope) {
          debugJsMM("ipcBridge", "fromMmid:${fromMM.mmid} ")

          val toJmmIpc = getJsProcess().createIpc(fromMM.manifest)
          toJmmIpc.onClosed {
            debugJsMM("ipcBridge close", "toJmmIpc=>${toJmmIpc.remote.mmid} fromMM:${fromMM.mmid}")
            fromMMIDOriginIpcWM.remove(fromMM.mmid)
          }
          // 如果是jsMM相互连接，直接把port丢过去

          toJmmIpc
        }
      }.await()


    override suspend fun _shutdown() {
      debugJsMM("shutdown") {
        "ipc-count=>${fromMMIDOriginIpcWM.size}"
      }
      val jsProcess = getJsProcess()
      jsProcess.codeIpc.close()
      fromMMIDOriginIpcWM.clear()
    }

    private suspend fun showMessage(message: String) {
      this.nativeFetch("file://toast.sys.dweb/show?message=$message")
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = JmmRuntime(bootstrapContext)

  override val runtime get() = super.runtime as JmmRuntime

  override fun toManifest(): CommonAppManifest {
    return this.metadata.toCommonAppManifest()
  }
}