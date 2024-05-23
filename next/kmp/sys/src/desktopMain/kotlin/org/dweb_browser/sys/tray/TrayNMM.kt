package org.dweb_browser.sys.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import dweb_browser_kmp.sys.generated.resources.Res
import dweb_browser_kmp.sys.generated.resources.tray_dweb_browser
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureMethod
import org.jetbrains.compose.resources.painterResource

/**这是一个桌面端组件*/
class TrayNMM : NativeMicroModule("tray.sys.dweb", "tray") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service)
  }

  private val actions = mutableStateListOf<TrayAction>()
  fun getRender(): @Composable ApplicationScope.() -> Unit {
    return {
      Render()
    }
  }

  @Composable
  fun ApplicationScope.Render() {
    Tray(icon = painterResource(Res.drawable.tray_dweb_browser), menu = {
      for (action in actions) {
        Item(action.title) {
          runtime.scopeLaunch(cancelable = true) {
            runtime.nativeFetch(action.url)
          }
        }
      }
      Item("Exit App") {
        runBlocking {
          PureViewController.exitDesktop()
        }
      }
    })
  }

  data class TrayAction(
    val title: String, val url: String
  ) {
    val id = randomUUID()
  }

  inner class TrayRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      routes(
        "/registry" bind PureMethod.GET by defineStringResponse {
          val title = request.query("title")
          val url = request.query("url")
          debugMM("tray-/registry") { println("title:$title,url=$url") }
          val action = TrayAction(title, url)
          actions += action
          action.id
        }
      ).cors()
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = TrayRuntime(bootstrapContext)
  override val runtime
    get() = super.runtime as TrayRuntime
}
