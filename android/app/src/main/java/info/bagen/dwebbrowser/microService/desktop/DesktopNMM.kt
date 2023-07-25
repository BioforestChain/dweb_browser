package info.bagen.dwebbrowser.microService.desktop

import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import info.bagen.dwebbrowser.microService.core.WindowAppInfo
import org.dweb_browser.helper.ChangeableMap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.browserUI.database.AppInfoDataStore
import org.dweb_browser.browserUI.download.compareAppVersionHigh
import org.dweb_browser.helper.Mmid
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugDesktop(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("Desktop", tag, msg, err)

@DelicateCoroutinesApi
class DesktopNMM : AndroidNativeMicroModule("desk.browser.dweb") {
  private var controller: DesktopController = DesktopController(this)

  companion object {
    private val controllerList = mutableListOf<DesktopController>()
    val desktopController get() = controllerList.firstOrNull()

    fun getRunningAppList() = runningAppList // 获取正在运行的程序
    fun getInstallAppList() = installAppList // 获取已经安装的程序
  }

  private val runningAppsIpc = ChangeableMap<Mmid, Ipc>()

  init {
    controllerList.add(DesktopController(this))
    // 监听runningApps的变化
    debugDesktop("1111111111111111111111111")
    runningAppsIpc.onChange { map ->
      for (app_id in map.keys) {
        // 每次变化对侧边栏图标进行排序(移动到最前面)
        val cur = runningAppList.firstOrNull { it.jsMicroModule.mmid == app_id }
        cur?.let {
          runningAppList.remove(it)
          runningAppList.add(it)
        }
      }
    }
    debugDesktop("22222222222222222222222222222")
    GlobalScope.launch(ioAsyncExceptionHandler) {
      AppInfoDataStore.queryAppInfoList().collectLatest { list -> // TODO 只要datastore更新，这边就会实时更新
        debugDesktop("33333333333333333333333333 ${list.size}")
        list.forEach { appMetaData ->
          val lastAppMetaData = installAppList.find { it.jsMicroModule.mmid == appMetaData.id }
          lastAppMetaData?.let {
            if (compareAppVersionHigh(it.jsMicroModule.metadata.version, appMetaData.version)) {
              bootstrapContext.dns.close(it.jsMicroModule.mmid)
            } else {
              return@forEach
            }
          }
          val jsMicroModule = JsMicroModule(appMetaData).also { jsMicroModule ->
            bootstrapContext.dns.install(jsMicroModule)
          }
          val windowAppInfo = WindowAppInfo(expand = false, jsMicroModule = jsMicroModule)
          installAppList.add(windowAppInfo)
        }
      }
    }
  }

  val queryAppId = Query.string().required("app_id")
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      "/openAppOrActivate" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request)
        val ipc = runningAppsIpc[mmid] ?: bootstrapContext.dns.connect(mmid).ipcForFromMM
        ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
        /// 如果成功打开，将它“追加”到列表中
        runningAppsIpc.remove(mmid)
        runningAppsIpc[mmid] = ipc
        /// 如果应用关闭，将它从列表中移除
        ipc.onClose {
          runningAppsIpc.remove(mmid)
        }
        return@defineHandler true
      },
      "/appsInfo" bind Method.GET to defineHandler { request ->
        val apps = installAppList
        debugDesktop("appInfo", apps.size)
        val responseApps = mutableListOf<BrowserNMM.AppInfo>()
        apps.forEach { item ->
          val meta = item.jsMicroModule.metadata
          responseApps.add(
            BrowserNMM.AppInfo(
              meta.id,
              meta.icon,
              meta.name,
              meta.short_name
            )
          )
        }
        return@defineHandler responseApps
      },

      "/closeApp" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request);
        var closed = false;
        if (runningAppsIpc.containsKey(mmid)) {
          closed = bootstrapContext.dns.close(mmid);
          if (closed) {
            runningAppsIpc.remove(mmid);
          }
        }
        return@defineHandler closed
      },
      "/desktop/apps" bind Method.GET to defineHandler { request ->
        return@defineHandler installAppList
      },
      "/desktop/observe/apps" bind Method.GET to defineHandler { request, ipc ->
        return@defineHandler ReadableStream(onStart = { controller ->
          val off = runningAppsIpc.onChange {
            try {
              withContext(ioAsyncExceptionHandler) {
                controller.enqueue((installAppList.toString() + "\n").toByteArray())
              }
            } catch (e: Exception) {
              controller.close()
              e.printStackTrace()
            }
          }
          ipc.onClose {
            off(Unit)
            controller.close()
          }
        })
      },
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

  override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
    App.startActivity(DesktopActivity::class.java) { intent ->
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      // 由于SplashActivity添加了android:excludeFromRecents属性，导致同一个task的其他activity也无法显示在Recent Screen，比如BrowserActivity
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
      intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
    }
    val activity = controller.waitActivityCreated()
    activitySignal.emit(Pair(this.mmid, activity))
  }
}