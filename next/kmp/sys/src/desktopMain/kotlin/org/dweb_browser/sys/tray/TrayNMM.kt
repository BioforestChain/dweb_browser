package org.dweb_browser.sys.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import dweb_browser_kmp.sys.generated.resources.Res
import dweb_browser_kmp.sys.generated.resources.tray_dweb_browser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.sys.SysI18nResource
import org.dweb_browser.sys.window.render.loadSourceToImageBitmap
import org.jetbrains.compose.resources.painterResource

/**这是一个桌面端组件*/
class TrayNMM : NativeMicroModule("tray.sys.dweb", "tray") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service)
  }

  private val allActionsFlow = MutableStateFlow(mapOf<String, TrayAction>())

  fun getRender(): @Composable ApplicationScope.() -> Unit {
    return {
      Render()
    }
  }

  @Composable
  fun ApplicationScope.Render() {
    Tray(icon = painterResource(Res.drawable.tray_dweb_browser), menu = {
      val actions by allActionsFlow.collectAsState()
      for (group in actions.values.groupBy { it.group }) {
        for (action in group.value) {
          Item(
            text = action.title,
            icon = action.icon,
            enabled = action.enabled,
            mnemonic = action.mnemonic,
            shortcut = action.shortcut?.asComposeKeyShortcut,
          ) {
            runtime.scopeLaunch(cancelable = true) {
              runtime.nativeFetch(action.url)
            }
          }
        }
        Separator()
      }
      Item(SysI18nResource.exit_app()) {
        runBlocking {
          PureViewController.exitDesktop()
        }
      }
    })
  }

  data class TrayAction(
    val id: String,
    val title: String,
    val url: String,
    val group: String? = null,
    val enabled: Boolean = true,
    val mnemonic: Char? = null,
    val icon: Painter? = null,
    val shortcut: KeyShortcut? = null,
  ) {

    @Serializable
    data class KeyShortcut(
      /**
       * @see androidx.compose.ui.input.key.Key
       */
      val keyCode: Int,
      /**
       * UNKNOWN 0
       * STANDARD 1
       * LEFT 2
       * RIGHT 3
       * NUMPAD 4
       */
      val keyLocation: Int,
      val ctrl: Boolean = false,
      val meta: Boolean = false,
      val alt: Boolean = false,
      val shift: Boolean = false,
    ) {
      val asComposeKeyShortcut by lazy {
        androidx.compose.ui.input.key.KeyShortcut(
          key = Key(keyCode, keyLocation),
          ctrl = ctrl,
          meta = meta,
          alt = alt,
          shift = shift,
        )
      }
    }
  }

  inner class TrayRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      routes("/registry" bind PureMethod.GET by defineStringResponse {
        val id = request.queryOrNull("id") ?: randomUUID()
        val title = request.query("title")
        val url = request.query("url")
        val group = request.queryOrNull("group")
        val enabled = request.queryAsOrNull<Boolean>("enabled")
        val mnemonic = request.queryAsOrNull<Char>("mnemonic")
        val icon = request.queryAsOrNull<String>("icon")
          ?.let { loadSourceToImageBitmap(it, 64, 64)?.toAwtImage()?.toPainter() }
        val shortcut = request.queryAsOrNull<TrayAction.KeyShortcut>("shortcut")
        val action = TrayAction(
          id = id,
          title = title,
          url = url,
          group = group,
          enabled = enabled ?: true,
          mnemonic = mnemonic,
          icon = icon,
          shortcut = shortcut,
        )
        allActionsFlow.value += action.id to action
        action.id
      }).cors()
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = TrayRuntime(bootstrapContext)
  override val runtime
    get() = super.runtime as TrayRuntime
}
