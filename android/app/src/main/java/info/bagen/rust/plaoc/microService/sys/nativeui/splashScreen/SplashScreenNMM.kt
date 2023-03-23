package info.bagen.rust.plaoc.microService.sys.nativeui.splashScreen

import android.os.Handler
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.commonAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM.Companion.getBfsMetaData
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewActivity
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.composite
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.*
import kotlin.concurrent.schedule

inline fun debugSplashScreen(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("SplashScreen", tag, msg, err)

class SplashScreenNMM : NativeMicroModule("splash-screen.nativeui.sys.dweb") {


//    private val splashScreen: SplashScreen = SplashScreen(App.appContext, SplashScreenConfig())
    private fun currentController(mmid: Mmid): MultiWebViewController? {
        return MultiWebViewNMM.getCurrentWebViewController(mmid)
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val query_SplashScreenSettings = Query.composite {
            SplashScreenSettings(
                autoHide = boolean().defaulted("autoHide", true)(it),
                fadeInDuration = long().defaulted("fadeInDuration", 200L)(it),
                fadeOutDuration = long().defaulted("fadeOutDuration", 200L)(it),
                showDuration = long().defaulted("showDuration", 3000L)(it),
            )
        }
        val query_HideOptions = Query.composite {
            HideOptions(
                fadeOutDuration = long().defaulted("fadeOutDuration", 200L)(it)
            )
        }

        apiRouting = routes(
            /** 显示*/
            "/show" bind Method.GET to defineHandler { request, ipc ->
                val options = query_SplashScreenSettings(request)
                val currentController = currentController(ipc.remote.mmid)
                val metadata = getBfsMetaData(ipc.remote.mmid)
                debugSplashScreen("show","remoteId:${ipc.remote.mmid} show===>${options} ${metadata?.splashScreen}")
                if (currentController != null && metadata !== null) {
                    show(currentController,metadata,options)
                    return@defineHandler Response(Status.OK)
                }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
            /** 隐藏*/
            "/hide" bind Method.GET to defineHandler { request, ipc ->
                val options = query_HideOptions(request)
                val currentActivity = currentController(ipc.remote.mmid)?.activity
                debugSplashScreen("hide","apiRouting hide===>${options}")
                if (currentActivity != null) {
//                    splashScreen.hide(options)
                    return@defineHandler Response(Status.OK)
                }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
        )
    }

    fun show(controller: MultiWebViewController,metadata: JmmMetadata,options: SplashScreenSettings) {
        val webview = controller.lastViewOrNull?.webView
        val entry = metadata.splashScreen.entry
        if (webview !== null && entry !== null) {
            GlobalScope.launch(Dispatchers.Main + commonAsyncExceptionHandler) {
                webview.loadUrl(entry)
                if (options.autoHide) {
                    delay(options.showDuration)
                    webview.goBack()
                }
            }

        }
    }

    override suspend fun _shutdown() {
    }


}

