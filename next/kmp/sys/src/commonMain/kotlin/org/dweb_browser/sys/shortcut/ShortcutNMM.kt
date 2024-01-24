package org.dweb_browser.sys.shortcut

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

val debugShortcut = Debugger("Shortcut")

class ShortcutNMM : NativeMicroModule("shortcut.sys.dweb", "Shortcut") {
  private val shortcutManage = ShortcutManage()
  private val shortcutList = mutableStateListOf<SystemShortcut>()
  init {
    short_name = "Shortcut"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    icons = listOf(
      ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml")
    )
    ioAsyncScope.launch {
      shortcutManage.initShortcut() // 初始化
    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = ShortcutStore(this)
    loadShortcut(store)

    routes(
      "/registry" bind PureMethod.GET by defineBooleanResponse {
        val systemShortcut = request.queryAs<SystemShortcut>()
        systemShortcut.mmid = ipc.remote.mmid // TODO 这个需要额外初始化，传参未必包含改字段
        debugShortcut("registry", "shortcut=$systemShortcut")
        if (systemShortcut.icon == null) {
          bootstrapContext.dns.query(ipc.remote.mmid)?.let { fromMM ->
            fromMM.icons.firstOrNull()?.let { imageRes ->
              systemShortcut.icon = nativeFetch(imageRes.src).body.toPureBinary()
            } ?: run {
              systemShortcut.icon = nativeFetch(icons.first().src).body.toPureBinary()
            }
          }
        }
        shortcutList.removeAll { it.uri == systemShortcut.uri }
        shortcutList.add(systemShortcut)
        store.set(systemShortcut.uri, systemShortcut)
        shortcutManage.registryShortcut(shortcutList)
      },
    )

    onRenderer {
      getMainWindow().apply {
        state.setFromManifest(this@ShortcutNMM)
        windowAdapterManager.provideRender(id) { modifier ->
          ShortcutManagerRender(modifier, this, shortcutList) { current, swap ->
            ioAsyncScope.launch {
              val currentItem = shortcutList[current]
              val swapItem = shortcutList[swap]
              currentItem.swap(swapItem)
              shortcutList[current] = swapItem
              shortcutList[swap] = currentItem
              shortcutManage.registryShortcut(shortcutList)
            }
          }
        }
      }
    }
  }

  override suspend fun _shutdown() {
  }

  private fun loadShortcut(store: ShortcutStore) {
    ioAsyncScope.launch {
      val map = store.getAll()
      val list = map.values.sortedBy { it.order }
      shortcutList.addAll(list)
      shortcutManage.registryShortcut(shortcutList)
      // 监听 dns 中应用的变化来实时更新快捷方式列表
      doObserve("file://dns.std.dweb/observe/install-apps") {
        // 对排序app列表进行更新
        debugShortcut("listenApps", "add=$adds, remove=$removes, updates=$updates")
        removes.map { mmid ->
          shortcutList.removeAll { it.mmid == mmid }
          shortcutManage.registryShortcut(shortcutList)
        }
        adds.map { mmid ->
          val addList = map.values.filter { it.mmid == mmid }
          if (addList.isNotEmpty()) {
            shortcutList.addAll(addList)
            shortcutManage.registryShortcut(shortcutList)
          }
        }
      }
    }
  }

  private suspend fun doObserve(urlPath: String, cb: suspend ChangeState<MMID>.() -> Unit) {
    val response = createChannel(urlPath) { frame, close ->
      when (frame) {
        is PureTextFrame -> {
          Json.decodeFromString<ChangeState<MMID>>(frame.data).also {
            it.cb()
          }
        }

        else -> {}
      }
    }
    debugShortcut("doObserve error", response.status)
  }
}