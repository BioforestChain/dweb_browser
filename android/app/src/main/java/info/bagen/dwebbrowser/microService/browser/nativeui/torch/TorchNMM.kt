package info.bagen.dwebbrowser.microService.browser.nativeui.torch

import io.ktor.http.HttpMethod
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.bind

class TorchNMM : NativeMicroModule("torch.nativeui.browser.dweb", "torch") {

  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      // 打开关闭手电筒
      "/toggleTorch" bind HttpMethod.Get to defineBooleanResponse {
        toggleTorch()
        return@defineBooleanResponse true
      }, "/torchState" bind HttpMethod.Get to defineBooleanResponse {
        return@defineBooleanResponse torchState()
      }).cors()
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