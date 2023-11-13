package org.dweb_browser.browser.nativeui.torch

expect class TorchApi(mm: TorchNMM) {
  fun toggleTorch()
  fun torchState(): Boolean
}
