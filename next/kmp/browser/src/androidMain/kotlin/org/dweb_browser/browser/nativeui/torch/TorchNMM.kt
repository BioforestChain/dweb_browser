package org.dweb_browser.browser.nativeui.torch

import io.ktor.http.HttpMethod
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.bind

class TorchNMM : NativeMicroModule("torch.nativeui.browser.dweb", "torch") {

  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val flashLightController = FlashLightController(getAppContext())
    routes(
      // 打开关闭手电筒
      "/toggleTorch" bind HttpMethod.Get to defineBooleanResponse {
        flashLightController.toggleTorch()
        return@defineBooleanResponse true
      },
      // 查询状态
      "/torchState" bind HttpMethod.Get to defineBooleanResponse {
        return@defineBooleanResponse flashLightController.torchState()
      }).cors()
  }

  private fun FlashLightController.toggleTorch() {
    if (hasFlashlight()) {
      if (isOn) {
        lightOff()
      } else {
        lightOn()
      }
    }
  }

  private fun FlashLightController.torchState(): Boolean {
    return isOn
  }

  override suspend fun _shutdown() {
  }
}