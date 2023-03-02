package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class KeyboardNMM: NativeMicroModule("keyboard.sys.dweb")  {

    private val virtualKeyboard = run {
        App.browserActivity?.dWebBrowserModel?.getSystemUi()?.virtualKeyboard
    }

    override suspend fun _bootstrap() {
        apiRouting = routes(
            /** 显示键盘*/
            "/show" bind Method.GET to defineHandler { request ->
                println("VirtualKeyboard#apiRouting show===>$mmid  ${request.uri.path} ")

                val result = virtualKeyboard?.show()
                Response(Status.OK).body(result.toString())
            },
            /** 隐藏键盘*/
            "/hide" bind Method.GET to defineHandler { request ->
                println("VirtualKeyboard#apiRouting hide===>$mmid  ${request.uri.path} ")
                val result = virtualKeyboard?.hide()
                Response(Status.OK).body(result.toString())
            },
            /** 安全区域*/
            "/safeArea" bind Method.GET to defineHandler { request ->
                println("VirtualKeyboard#apiRouting safeArea===>$mmid  ${request.uri.path} ")
                val result = virtualKeyboard?.getSafeArea()
                if (result != null) {
                    Response(Status.OK).body(result)
                }
                Response(Status.NOT_FOUND).body("safeArea return zero!!!")
            },
            /** 隐藏键盘*/
            "/height" bind Method.GET to defineHandler { request ->
                println("VirtualKeyboard#apiRouting height===>$mmid  ${request.uri.path} ")
                val result = virtualKeyboard?.getHeight()
                Response(Status.OK).body(result.toString())
            },
        )
    }
    override suspend fun _shutdown() {
        virtualKeyboard?.hide()
    }
}