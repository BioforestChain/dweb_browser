package info.bagen.rust.plaoc.microService.sys.plugin.splash

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewActivity
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.composite
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.routing.routes

class SplashScreenNMM : NativeMicroModule("splash.sys.dweb") {


    private val splashScreen: SplashScreen = SplashScreen(App.appContext, SplashScreenConfig())
    private fun currentActivity(mmid: Mmid): MultiWebViewActivity? {
        return MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity
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
                val currentActivity = currentActivity(ipc.remote.mmid)
                println("SplashScreenNMM#apiRouting show===>${options} $splashScreen  $currentActivity")
                if (currentActivity != null) {
                    splashScreen.show(currentActivity, options)
                    return@defineHandler Response(Status.OK)
                }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
            /** 显示*/
            "/hide" bind Method.GET to defineHandler { request, ipc ->
                val options = query_HideOptions(request)
                val currentActivity = currentActivity(ipc.remote.mmid)
                println("SplashScreenNMM#apiRouting hide===>${options} $splashScreen  $currentActivity")
                if (currentActivity != null) {
                    splashScreen.hide(options)
                    return@defineHandler Response(Status.OK)
                }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
        )
    }

    override suspend fun _shutdown() {
    }


}

