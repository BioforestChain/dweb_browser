package org.dweb_browser.sys.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.Tray
import dweb_browser_kmp.sys.generated.resources.Res
import dweb_browser_kmp.sys.generated.resources.tray_dweb_browser
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.buildRequestX
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.SysI18nResource
import org.jetbrains.compose.resources.painterResource

/**这是一个桌面端组件*/
class TrayNMM : NativeMicroModule("tray.sys.dweb", "tray") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service)
  }

  private val trayRootTree = TrayComposeItem(id = "", title = "", type = TRAY_ITEM_TYPE.Menu)

  @Composable
  private fun MenuScope.RenderNode(node: TrayComposeItem) {
    key(node.id) {
      when (node.type) {
        TRAY_ITEM_TYPE.Menu -> Menu(
          text = node.title,
          enabled = node.enabled,
          mnemonic = node.mnemonic,
        ) {
          RenderNodeList(node.children.collectAsState().value)
        }

        TRAY_ITEM_TYPE.Item -> Item(
          text = node.title,
          icon = node.icon?.let {
            PureImageLoader.SmartLoad(it, 64.dp, 64.dp).success?.toAwtImage()?.toPainter()
          },
          enabled = node.enabled,
          mnemonic = node.mnemonic,
          shortcut = remember(node.shortcut) { node.shortcut?.toComposeKeyShortcut() },
          onClick = {
            node.url?.also { url ->
              runtime.scopeLaunch(cancelable = true) {
                runtime.nativeFetch(url)
              }
            }
          }
        )
      }
    }
  }

  @Composable
  private fun MenuScope.RenderNodeList(nodeList: List<TrayComposeItem>) {
    for (group in nodeList.groupBy { it.group }) {
      for (node in group.value) {
        RenderNode(node)
      }
      Separator()
    }
  }

  fun getRender(): @Composable ApplicationScope.() -> Unit {
    return {
      Tray(icon = painterResource(Res.drawable.tray_dweb_browser), menu = {
        RenderNodeList(trayRootTree.children.collectAsState().value)

        Item(
          text = "${SysI18nResource.capture()}     Alt+PrtScr",
          // shortcut = KeyShortcut(key = Key.PrintScreen, ctrl = true) // 有异常
        ) {
          runtime.scopeLaunch(cancelable = false) {
            val imageBitmap = PureViewController.awaitScreenCapture()
            // TODO 打开扫码
            runtime.nativeFetch(
              buildRequestX(
                url = "file://scan.browser.dweb/parseImage",
                method = PureMethod.POST,
                headers = PureHeaders(),
                body = imageBitmap.toByteArray()
              )
            )
          }
        }

        Item(SysI18nResource.exit_app()) {
          runBlocking {
            PureViewController.exitDesktop()
          }
        }
      })
    }
  }

  private fun SysKeyShortcut.toComposeKeyShortcut() = KeyShortcut(
    key = Key(keyCode, keyLocation),
    ctrl = ctrl,
    meta = meta,
    alt = alt,
    shift = shift,
  )


  inner class TrayRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      routes(
        "/registry" bind PureMethod.GET by defineStringResponse {
          val id = request.queryOrNull("id") ?: randomUUID()
          val title = request.query("title")
          val type = request.query("type").let {
            TRAY_ITEM_TYPE.ALL[it] ?: throwException(
              code = HttpStatusCode.NotFound,
              message = "no found type: $it"
            )
          }
          val parentId = request.queryOrNull("parent")
          val url = request.queryOrNull("url")
          val group = request.queryOrNull("group")
          val enabled = request.queryAsOrNull<Boolean>("enabled")
          val mnemonic = request.queryAsOrNull<Char>("mnemonic")
          val icon = request.queryAsOrNull<String>("icon")
          val shortcut = request.queryAsOrNull<SysKeyShortcut>("shortcut")

          val parent = parentId?.let {
            trayRootTree.findById(parentId) ?: throwException(
              code = HttpStatusCode.NotFound,
              message = "no found parent by id: $parentId"
            )
          } ?: trayRootTree
          val newItem = TrayComposeItem(
            id = id,
            type = type,
            title = title,
            url = url,
            group = group,
            enabled = enabled ?: true,
            mnemonic = mnemonic,
            icon = icon,
            shortcut = shortcut,
          )
          when (val oldItem = parent.findById(id)) {
            null -> parent.addChild(newItem)
            else -> parent.replaceChild(oldItem, newItem)
          }

          newItem.id
        }).cors()
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = TrayRuntime(bootstrapContext)
  override val runtime
    get() = super.runtime as TrayRuntime
}
