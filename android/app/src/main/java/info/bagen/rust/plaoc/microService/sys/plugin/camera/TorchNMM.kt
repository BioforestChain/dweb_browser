package info.bagen.rust.plaoc.microService.sys.plugin.camera

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class TorchNMM:NativeMicroModule("torch.sys.dweb") {
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