package info.bagen.rust.plaoc.microService.sys.plugin

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule

class PluginNMM:NativeMicroModule("plugin.sys.dweb") {
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        //负责管理所有的plugin
    }

    override suspend fun _shutdown() {
    }
}