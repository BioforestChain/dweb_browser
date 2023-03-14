package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class KeyboardNMM : NativeMicroModule("keyboard.sys.dweb") {

    private fun getController(mmid: Mmid) =
        NativeUiController.fromMultiWebView(mmid).virtualKeyboard


    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        QueryHelper.init()
        apiRouting = routes(
            /** 显示键盘*/
            "/show" bind Method.GET to defineHandler { request ->
                val virtualKeyboard = getController(mmid)
                virtualKeyboard.showState.value = true
                return@defineHandler null
            },
            /** 隐藏键盘*/
            "/hide" bind Method.GET to defineHandler { request ->
                println("VirtualKeyboard#apiRouting hide===>$mmid  ${request.uri.path} ")
                val virtualKeyboard = getController(mmid)
                virtualKeyboard.showState.value = false
                return@defineHandler null
            },
            /** 安全区域*/
            "/safeArea" bind Method.GET to defineHandler { request ->
                println("VirtualKeyboard#apiRouting safeArea===>$mmid  ${request.uri.path} ")
                val virtualKeyboard = getController(mmid)
                return@defineHandler virtualKeyboard.imeInsets.value
            },
        )
    }

    override suspend fun _shutdown() {
    }
}
