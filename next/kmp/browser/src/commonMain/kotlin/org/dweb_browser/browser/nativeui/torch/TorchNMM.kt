package org.dweb_browser.browser.nativeui.torch

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

class TorchNMM : NativeMicroModule("torch.nativeui.browser.dweb", "torch") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      // 打开关闭手电筒
      "/toggleTorch" bind PureMethod.GET by defineBooleanResponse {
        TorchApi.toggleTorch()
        return@defineBooleanResponse true
      },
      // 查询状态
      "/torchState" bind PureMethod.GET by defineBooleanResponse {
        return@defineBooleanResponse TorchApi.torchState()
      }).cors()
  }

  override suspend fun _shutdown() {
  }
}
