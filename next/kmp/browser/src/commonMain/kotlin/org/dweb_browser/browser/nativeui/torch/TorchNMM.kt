package org.dweb_browser.browser.nativeui.torch

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.pure.http.PureMethod

class TorchNMM : NativeMicroModule("torch.nativeui.browser.dweb", "torch") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  inner class TorchRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    override suspend fun _bootstrap() {
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

  override fun createRuntime(bootstrapContext: BootstrapContext) = TorchRuntime(bootstrapContext)
}
