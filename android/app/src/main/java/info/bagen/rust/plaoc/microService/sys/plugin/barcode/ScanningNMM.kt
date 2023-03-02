package info.bagen.rust.plaoc.microService.sys.plugin.barcode
import android.content.Intent
import info.bagen.libappmgr.ui.camera.QRCodeIntent
import info.bagen.libappmgr.ui.camera.ScanType
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.browser.BrowserActivity
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM.Companion.activityMap
import info.bagen.rust.plaoc.microService.sys.mwebview.MutilWebViewActivity
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugScanning(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Scanning", tag, msg, err)

class ScanningNMM() :NativeMicroModule("scanning.sys.dweb") {

    companion object {
        val QrCallBackData = PromiseOut<String>()
    }

    override suspend fun _bootstrap() {
        apiRouting = routes(
            // 二维码扫码
            "/qr/start" bind Method.GET to defineHandler { request,ipc ->
                debugScanning("ScanningNMM#apiRouting"," /qr/start===>$mmid  request:${request.uri.query} ")
                val result =  openScannerActivity(ipc.remote)
                println("/qr/start===>$mmid  openScannerActivity:$result")
                if(result.isNotEmpty()) {
                 return@defineHandler Response(Status.OK).body(result)
                }
                Response(Status.CLIENT_TIMEOUT).body("Client Timeout")
            },
            // 二维码关闭
            "/qr/stop" bind Method.GET to defineHandler { request ->
                debugScanning("ScanningNMM#apiRouting"," /qr/stop===>$mmid  request:${request.uri.query} ")
                App.browserActivity?.also {
                    it.qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(false))
                    Response(Status.OK)
                }
                Response(Status.CONNECTION_REFUSED)
            },
            // 条形码开启
            "/barcode/start" bind Method.GET to defineHandler { request ->
                debugScanning("ScanningNMM#apiRouting "," /barcode/start===>$mmid  request:${request.uri.query} ")
                App.browserActivity?.also {
                    it.qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true, ScanType.BARCODE))
                    Response(Status.OK)
                }
                Response(Status.CONNECTION_REFUSED)
            },
            // 条形码关闭
            "/barcode/stop" bind Method.GET to defineHandler { request ->
                debugScanning("ScanningNMM#apiRouting "," /barcode/stop===>$mmid  request:${request.uri.query} ")
                App.browserActivity?.also {
                    it.qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(false, ScanType.BARCODE))
                    Response(Status.OK)
                }
                Response(Status.CONNECTION_REFUSED)
            },
        )
    }

    // 打开二维码
    private suspend fun openScannerActivity(microModule: MicroModule): String {
        val getActivity = activityMap[microModule.mmid]?.waitPromise()
        if (getActivity == null) {
            App.startActivity(MutilWebViewActivity::class.java) {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        val intent = Intent(getActivity,QRCodeScanningActivity::class.java)
        getActivity!!.startActivity(intent)
        QRCodeScanningActivity.promise_op = PromiseOut()
       return QRCodeScanningActivity.promise_op.waitPromise()
    }


    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }

}