package org.dweb_browser.browser.nativeui.torch

import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext

actual object TorchApi {
  private val flashLightController by lazy { FlashLightController(NativeMicroModule.getAppContext()) }

  actual fun toggleTorch() {
    if (flashLightController.hasFlashlight()) {
      if (flashLightController.isOn) {
        flashLightController.lightOff()
      } else {
        flashLightController.lightOn()
      }
    }
  }

  actual fun torchState() = flashLightController.isOn
}
