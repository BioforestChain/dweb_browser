package info.bagen.dwebbrowser.microService.desktop

import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import info.bagen.dwebbrowser.microService.browser.debugBrowser
import org.dweb_browser.helper.DesktopAppMetaData
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.browser.jmm.debugJMM
import info.bagen.dwebbrowser.util.ChangeableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.dweb_browser.browserUI.database.AppInfoDataStore
import org.dweb_browser.helper.toDesktopAppMetaData
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes


class DesktopNMM : NativeMicroModule("desk.browser.dweb") {

  companion object {
    private val controllerList = mutableListOf<DesktopController>()
    val desktopController get() = controllerList.firstOrNull()
  }

  // 侧边栏，需要存储
  private val taskbarAppList = mutableSetOf<JsMicroModule>()
  private val runningApps = ChangeableMap<Mmid, Ipc>()

  init {
    controllerList.add(DesktopController(this))
    // 监听runningApps的变化
    runningApps.onChange { map ->
      for (app_id in map.keys) {
        // 每次变化对侧边栏图标进行排序(移动到最前面)
        // taskbarAppList.unshift(app_id);
        // 存储到内存
//        desktopStore.set("taskbar/apps", new Set(taskbarAppList));
      }
    }
  }

  val queryAppId = Query.string().required("app_id")
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      "/openAppOrActivate" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request)
        var ipc = runningApps[mmid]

        if (ipc == null) {
          ipc = bootstrapContext.dns.connect(mmid).ipcForFromMM
        }
        debugJMM("openApp", "postMessage==>activity ${ipc.remote.mmid}")
        ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
        /// 如果成功打开，将它“追加”到列表中
        runningApps.remove(mmid);
        runningApps[mmid] = ipc;
        /// 如果应用关闭，将它从列表中移除
        ipc.onClose {
          runningApps.remove(mmid);
        }
        return@defineHandler true
      },
      "/closeApp" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request);
        var closed = false;
        if (runningApps.containsKey(mmid)) {
          closed = bootstrapContext.dns.close(mmid);
          if (closed) {
            runningApps.remove(mmid);
          }
        }
        return@defineHandler closed
      },
      "/desktop/apps" bind Method.GET to defineHandler { request ->
        return@defineHandler getDesktopAppList()
      },
      "/desktop/observe/apps" bind Method.GET to defineHandler { request, ipc ->
        return@defineHandler ReadableStream(onStart = { controller ->
          val off = runningApps.onChange {
            try {
              withContext(Dispatchers.IO) {
                controller.enqueue((getDesktopAppList().toString() + "\n").toByteArray())
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
      "/appsInfo" bind Method.GET to defineHandler { request ->
        val apps = JmmNMM.getAndUpdateJmmNmmApps()
        debugBrowser("appInfo", apps.size)
        val responseApps = mutableListOf<BrowserNMM.AppInfo>()
        apps.forEach { item ->
          val meta = item.value.metadata
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
    )
  }

  private suspend fun getDesktopAppList(): DesktopAppMetaData? {
    val list = AppInfoDataStore.queryAppInfoList().toList()
    for (appList in list) {
      for (app in appList) {
        return app.toDesktopAppMetaData(runningApps.containsKey(app.id))
      }
    }
    return null
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
  }
}