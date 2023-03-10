package info.bagen.rust.plaoc.microService.sys.plugin.splash

import android.app.Activity
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.lens.Query
import org.http4k.routing.bind
import org.http4k.routing.routes

class SplashScreenNMM: NativeMicroModule("splash.sys.dweb") {


    private val splashScreen:SplashScreen = SplashScreen(App.appContext, SplashScreenConfig())
    private  val currentActivity  by lazy {
         MultiWebViewNMM.getCurrentWebViewController()?.activity
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            /** 显示*/
            "/show" bind Method.GET to defineHandler { request ->
                val options = Query.auto<SplashScreenSettings>().required("options")(request)
                println("SplashScreenNMM#apiRouting show===>${options} $splashScreen  $currentActivity")
                   if (currentActivity!=null){
                       splashScreen.show(currentActivity!!, options){
                           throw Exception(it)
                       }
                     return@defineHandler  Response(Status.OK)
                   }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
            /** 显示*/
            "/hide" bind Method.GET to defineHandler { request ->
                val options = Query.auto<HideOptions>().required("options")(request)
                println("SplashScreenNMM#apiRouting hide===>${options} $splashScreen  $currentActivity")
                if (currentActivity!=null){
                    splashScreen.hide(options)
                    return@defineHandler  Response(Status.OK)
                }
                Response(Status.INTERNAL_SERVER_ERROR).body("No current activity found")
            },
        )
    }

    override suspend fun _shutdown() {
    }



}

