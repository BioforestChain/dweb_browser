package org.dweb_browser.sys.key

import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.pure.http.PureMethod

class KeyNMM : NativeMicroModule("key.sys.dweb", "key generator") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    KeyApi.generatePrivateKey()

    routes(
      "/encrypt" bind PureMethod.POST by definePureBinaryHandler {
        val input = request.body.toPureBinary()

        KeyApi.encrypt(input)
      },
      "/decrypt" bind PureMethod.POST by definePureBinaryHandler {
        val encryptedData = request.body.toPureBinary()

        KeyApi.decrypt(encryptedData)
      }
    )
  }

  override suspend fun _shutdown() {}
}