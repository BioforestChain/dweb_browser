package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("browser", tag, msg, err)

class BrowserNMM : NativeMicroModule("browser.sys.dweb") {
    companion object {
        lateinit var browserController: BrowserController
    }
    init {
        browserController = BrowserController("browser.sys.dweb", this)
    }
    val query_app_id = Query.string().required("app_id")
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            "/openApp" bind Method.GET to defineHandler { request ->
                // TODO 直接调这个后端没启动
               val mmid = query_app_id(request)
                browserController.showLoading.value = true
                return@defineHandler browserController.openApp(mmid)
            })
    }

    override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
        App.startActivity(BrowserActivity::class.java) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            // TODO 由于SplashActivity添加了android:excludeFromRecents属性，导致同一个task的其他activity也无法显示在Recent Screen，比如BrowserActivity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
        }
    }

    override suspend fun _shutdown() {
    }
}