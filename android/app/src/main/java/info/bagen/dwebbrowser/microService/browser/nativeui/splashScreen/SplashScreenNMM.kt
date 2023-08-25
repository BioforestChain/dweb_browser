package info.bagen.dwebbrowser.microService.browser.nativeui.splashScreen

import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.composite
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugSplashScreen(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("SplashScreen", tag, msg, err)

class SplashScreenNMM : NativeMicroModule("splash-screen.nativeui.browser.dweb","splashScreen") {

  override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val querySplashScreenSettings = Query.composite {
            SplashScreenSettings(
                autoHide = boolean().defaulted("autoHide", true)(it),
                fadeInDuration = long().defaulted("fadeInDuration", 200L)(it),
                fadeOutDuration = long().defaulted("fadeOutDuration", 200L)(it),
                showDuration = long().defaulted("showDuration", 3000L)(it),
            )
        }
        val queryHideOptions = Query.composite {
            HideOptions(
                fadeOutDuration = long().defaulted("fadeOutDuration", 200L)(it)
            )
        }

        apiRouting = routes(
            /** 显示*/
            "/show" bind Method.GET to defineHandler { request, ipc ->
//                val options = querySplashScreenSettings(request)
//                val currentController = currentController(ipc.remote.mmid)
//                val microModule = bootstrapContext.dns.query(ipc.remote.mmid)
//                if (microModule is JsMicroModule) {
//                    val metadata = microModule.metadata
//                    debugSplashScreen(
//                        "show",
//                        "remoteId:${ipc.remote.mmid} show===>${options} "
//                    )
//                    if (currentController != null) {
//                        show(currentController, metadata, options)
//                        return@defineHandler Response(Status.OK)
//                    }
//                }

                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
            /** 隐藏*/
            "/hide" bind Method.GET to defineHandler { request, ipc ->
//                val options = queryHideOptions(request)
//                val currentActivity = currentController(ipc.remote.mmid)?.activity
//                debugSplashScreen("hide", "apiRouting hide===>${options}")
//                if (currentActivity != null) {
////                    splashScreen.hide(options)
//                    return@defineHandler Response(Status.OK)
//                }
//                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
        )
    }

    override suspend fun _shutdown() {
    }
}

