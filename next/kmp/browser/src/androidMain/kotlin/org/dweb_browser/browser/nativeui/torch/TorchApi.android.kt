package org.dweb_browser.browser.nativeui.torch

import org.dweb_browser.core.module.getAppContext

actual class TorchApi actual constructor(mm: TorchNMM) {
  private val flashLightController = FlashLightController(mm.getAppContext())

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
