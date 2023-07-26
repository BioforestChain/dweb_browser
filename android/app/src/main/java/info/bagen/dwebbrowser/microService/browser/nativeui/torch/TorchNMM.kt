package info.bagen.dwebbrowser.microService.browser.nativeui.torch

import org.dweb_browser.helper.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class TorchNMM: NativeMicroModule("torch.nativeui.browser.dweb","torch") {

    override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            // 打开关闭手电筒
            "/toggleTorch" bind Method.GET to defineHandler { request ->
                toggleTorch()
                return@defineHandler true
            },
            "/torchState" bind Method.GET to defineHandler { request ->
                return@defineHandler torchState()
            }
        )
    }

    private fun toggleTorch() {
        if (FlashLightUtils.hasFlashlight()) {
            if (FlashLightUtils.isOn) {
                FlashLightUtils.lightOff()
            } else {
                FlashLightUtils.lightOn()
            }
        }
    }

    private fun torchState(): Boolean {
        return FlashLightUtils.isOn
    }

    override suspend fun _shutdown() {
    }
}