package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.ColorJson
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.toJsonAble
import org.http4k.core.Request
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.string

class QueryHelper {
    companion object {
        init {
            NativeMicroModule.ResponseRegistry.registryJsonAble(Color::class.java) { it.toJsonAble() }
            NativeMicroModule.ResponseRegistry.registryJsonAble(WindowInsets::class.java) { it.toJsonAble() }
            NativeMicroModule.ResponseRegistry.registryJsonAble(NativeUiController.StatusBarController::class.java) { it.toJsonAble() }
            NativeMicroModule.ResponseRegistry.registryJsonAble(NativeUiController.NavigationBarController::class.java) { it.toJsonAble() }
        }

        fun init() {
            // 确保 init 里头的类被注册
        }

        fun color(req: Request) = Query.string().optional("color")(req)?.let {
            gson.fromJson(it, ColorJson::class.java).toColor()
        }
        fun style(req: Request) = Query.string().optional("style")(req)?.let {
            NativeUiController.BarStyle.from(it)
        }
        val visible = Query.boolean().optional("visible")
        val overlay = Query.boolean().optional("overlay")
    }
}



