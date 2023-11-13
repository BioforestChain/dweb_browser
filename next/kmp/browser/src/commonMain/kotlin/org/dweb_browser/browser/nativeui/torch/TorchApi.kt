package org.dweb_browser.browser.nativeui.torch

expect object TorchApi {
  fun toggleTorch()
  fun torchState(): Boolean
}
