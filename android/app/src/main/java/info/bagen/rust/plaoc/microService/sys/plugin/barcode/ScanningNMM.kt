package info.bagen.rust.plaoc.microService.sys.plugin.barcode

import info.bagen.libappmgr.ui.camera.QRCodeIntent
import info.bagen.libappmgr.ui.camera.ScanType
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class ScanningNMM:NativeMicroModule("scanning.sys.dweb") {
    override suspend fun _bootstrap() {
        apiRouting = routes(
            // 二维码扫码
            "/qr/start" bind Method.GET to defineHandler { request ->
                println("ScanningNMM#apiRouting /qr/start===>$mmid  request:${request.uri.query} ")
                App.browserActivity?.also {
                    it.qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
                    Response(Status.OK)
                }
                Response(Status.CONNECTION_REFUSED)
            },
            // 二维码关闭
            "/qr/stop" bind Method.GET to defineHandler { request ->
                println("ScanningNMM#apiRouting /qr/stop===>$mmid  request:${request.uri.query} ")
                App.browserActivity?.also {
                    it.qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(false))
                    Response(Status.OK)
                }
                Response(Status.CONNECTION_REFUSED)
            },
            // 条形码开启
            "/barcode/start" bind Method.GET to defineHandler { request ->
                println("ScanningNMM#apiRouting /barcode/start===>$mmid  request:${request.uri.query} ")
                App.browserActivity?.also {
                    it.qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true, ScanType.BARCODE))
                    Response(Status.OK)
                }
                Response(Status.CONNECTION_REFUSED)
            },
            // 条形码关闭
            "/barcode/stop" bind Method.GET to defineHandler { request ->
                println("ScanningNMM#apiRouting /barcode/stop===>$mmid  request:${request.uri.query} ")
                App.browserActivity?.also {
                    it.qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(false, ScanType.BARCODE))
                    Response(Status.OK)
                }
                Response(Status.CONNECTION_REFUSED)
            },
        )
    }

    // 打开二维码
    fun openScannerActivity() {
        QRCodeScanningActivity().initCameraScan()
    }


    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }

}