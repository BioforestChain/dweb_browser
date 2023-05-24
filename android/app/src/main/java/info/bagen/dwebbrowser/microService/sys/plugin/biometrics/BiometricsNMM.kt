package info.bagen.dwebbrowser.microService.sys.plugin.biometrics

import android.content.Intent
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.printdebugln
import info.bagen.dwebbrowser.microService.sys.plugin.biometrics.BiometricsActivity.Companion.biometrics_promise_out
import info.bagen.dwebbrowser.microService.sys.plugin.biometrics.BiometricsController.Companion.biometricsController
import info.bagen.dwebbrowser.microService.sys.plugin.share.ShareOptions
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.composite
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugBiometrics(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("biometrics", tag, msg, err)

class BiometricsNMM: AndroidNativeMicroModule("biometrics.sys.dweb") {

    val queryType = Query.string().defaulted("type", "")
    val queryBiometricsData = Query.composite { spec ->
        BiometricsData(
            title = string().optional("title")(spec),
            subtitle = string().optional("subtitle")(spec),
            description = string().optional("description")(spec),
            useFallback = boolean().optional("useFallback")(spec),
            negativeButtonText = string().optional("negativeButtonText")(spec),
        )
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {

        apiRouting = routes(
            /** 检查识别支持生物识别*/
            "/check" bind Method.GET to defineHandler { request, ipc ->
                val type = queryType(request)
                debugBiometrics("check",type)
                openActivity(ipc.remote.mmid, queryBiometricsData(request))
                val context = biometricsController.waitActivityCreated()
                val result = context.chuck()
                context.finish()
                return@defineHandler  result
            },
            /** 生物识别*/
            "/biometrics" bind Method.GET to defineHandler { request, ipc ->
                debugBiometrics("fingerprint",ipc.remote.mmid)
                openActivity(ipc.remote.mmid)
                val context = biometricsController.waitActivityCreated()
                context.biometrics()
                return@defineHandler  biometrics_promise_out.waitPromise()
            })
    }

    override fun openActivity(remoteMmid: Mmid) {
        val activity = getActivity(remoteMmid)
        val intent = Intent(getActivity(remoteMmid), BiometricsActivity::class.java)
        intent.`package` = App.appContext.packageName
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity?.startActivity(intent)
    }

    private fun openActivity(remoteMmid: Mmid, data: BiometricsData) {
        val activity = getActivity(remoteMmid)
        val intent = Intent(getActivity(remoteMmid), BiometricsActivity::class.java)
        intent.`package` = App.appContext.packageName
        intent.putExtra("data", data)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity?.startActivity(intent)
    }

    override suspend fun _shutdown() {
    }
}