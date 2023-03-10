package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.toJsonAble
import org.http4k.lens.*

class QueryHelper {
    companion object {
        init {
            NativeMicroModule.ResponseRegistry.registryJsonAble(Color::class.java) { it.toJsonAble() }
            NativeMicroModule.ResponseRegistry.registryJsonAble(WindowInsets::class.java) { it.toJsonAble() }
            NativeMicroModule.ResponseRegistry.registryJsonAble(NativeUiController.StatusBarController::class.java) { it.toJsonAble() }
        }

        val color = Query.composite {
            Color(
                red = int().required("red")(it),
                blue = int().required("blue")(it),
                green = int().required("green")(it),
                alpha = int().required("alpha")(it)
            )
        }
        val style =
            Query.composite {
                NativeUiController.BarStyle.from(
                    Query.string().required("style")(it)
                )
            }
        val visible = Query.boolean().required("visible")
        val overlay = Query.boolean().required("overlay")
    }
}



