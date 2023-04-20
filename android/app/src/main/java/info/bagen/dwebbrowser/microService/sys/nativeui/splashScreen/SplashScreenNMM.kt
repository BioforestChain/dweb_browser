package info.bagen.dwebbrowser.microService.sys.nativeui.splashScreen

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.commonAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.printdebugln
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.mwebview.MultiWebViewController
import info.bagen.dwebbrowser.microService.sys.mwebview.MultiWebViewNMM
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

inline fun debugSplashScreen(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("SplashScreen", tag, msg, err)

class SplashScreenNMM : NativeMicroModule("splash-screen.nativeui.sys.dweb") {

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
                val metadata = bootstrapContext.dns.query(ipc.remote.mmid)?.metadata
                debugSplashScreen(
                    "show",
                    "remoteId:${ipc.remote.mmid} show===>${options} ${metadata?.splashScreen}"
                )
                if (currentController != null && metadata !== null) {
                    show(currentController, metadata, options)
                    return@defineHandler Response(Status.OK)
                }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
            /** 隐藏*/
            "/hide" bind Method.GET to defineHandler { request, ipc ->
                val options = query_HideOptions(request)
                val currentActivity = currentController(ipc.remote.mmid)?.activity
                debugSplashScreen("hide", "apiRouting hide===>${options}")
                if (currentActivity != null) {
//                    splashScreen.hide(options)
                    return@defineHandler Response(Status.OK)
                }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
        )
    }

    fun show(
        controller: MultiWebViewController,
        metadata: JmmMetadata,
        options: SplashScreenSettings
    ) {
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

