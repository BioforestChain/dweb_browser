package org.dweb_browser.sys.shortcut

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

val debugShortcut = Debugger("Shortcut")

class ShortcutNMM : NativeMicroModule("shortcut.sys.dweb", "Shortcut") {
  private val shortcutManage = ShortcutManage()

  init {
    dweb_deeplinks = listOf("dweb://shortcutopen")
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

  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = ShortcutStore(this)
    val shortcutList = loadShortcut(store)

    routes(
      "/registry" bind PureMethod.POST by defineBooleanResponse {
        val body = request.body.toPureBinary()
        val data = Cbor.decodeFromByteArray<ShortcutParameter>(body)
        debugShortcut("/registry=>", "data=$data")
        val systemShortcut = SystemShortcut(
          data.title,
          mmid = ipc.remote.mmid,
          data = data.data,
          icon = data.icon
        )
        if (systemShortcut.icon == null) {
          bootstrapContext.dns.query(ipc.remote.mmid)?.let { fromMM ->
            val imageResource = fromMM.icons.firstOrNull() ?: icons.first()
            systemShortcut.icon = shortcutManage.getValidIcon(this@ShortcutNMM, imageResource)
          }
        }
        // 移除重复的
        val remove =
          shortcutList.removeAll { it.title == systemShortcut.title && it.mmid == systemShortcut.mmid }
        debugShortcut("/registry=>", "remove=$remove")
        if (remove) {
          store.delete(systemShortcut.title)
        }
        shortcutList.add(systemShortcut)
        store.set(systemShortcut.title, systemShortcut)
        shortcutManage.registryShortcut(shortcutList)
      },
      "shortcutopen" bindDwebDeeplink defineEmptyResponse {
        val mmid = request.query("mmid")
        val data = request.query("data")
        val (ipc) = bootstrapContext.dns.connect(mmid)
        debugShortcut("shortcut-open=>", "${ipc.remote.mmid}=> $data")
        ipc.postMessage(IpcEvent.fromUtf8("shortcut", data))
      }
    )

    onRenderer {
      getMainWindow().apply {
        setStateFromManifest(this@ShortcutNMM)
        windowAdapterManager.provideRender(id) { modifier ->
          ShortcutManagerRender(
            modifier = modifier,
            windowRenderScope = this,
            shortcutList = shortcutList,
            onDragMove = { from, to ->
              shortcutList.add(to.index, shortcutList.removeAt(from.index))
            },
            onDragEnd = { _, _ ->
              ioAsyncScope.launch {
                shortcutManage.registryShortcut(shortcutList)
              }
            },
            onRemove = { item ->
              ioAsyncScope.launch {
                shortcutList.removeAll { it.title == item.title }
                shortcutManage.registryShortcut(shortcutList)
                store.delete(item.title)
              }
            }
          )
        }
      }
    }
  }

  override suspend fun _shutdown() {
  }

  private suspend fun loadShortcut(store: ShortcutStore): MutableList<SystemShortcut> {
    val shortcutList = mutableStateListOf<SystemShortcut>()
    val map = store.getAll()
    val list = map.values.sortedBy { it.order }
    shortcutList.addAll(list)
    shortcutManage.registryShortcut(shortcutList)
    ioAsyncScope.launch {
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
    return shortcutList
  }

  private suspend fun doObserve(urlPath: String, cb: suspend ChangeState<MMID>.() -> Unit) {
    val response = createChannel(urlPath) {
      for (frame in income){
        when (frame) {
          is PureTextFrame -> {
            Json.decodeFromString<ChangeState<MMID>>(frame.data).also {
              it.cb()
            }
          }

          else -> {}
        }
      }
    }
    debugShortcut("doObserve error", response.status)
  }
}