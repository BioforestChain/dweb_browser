package org.dweb_browser.sys.shortcut

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.cbor.ByteString
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.platform.toImageBitmap

@Serializable
data class ShortcutParameter @OptIn(ExperimentalSerializationApi::class) constructor(
  val title: String,
  val data: String, // 打开的应用的地址
  @ByteString var icon: ByteArray? = null,
)

@Serializable
data class SystemShortcut(
  val title: String,
  val data: String, // 打开的应用的地址
  var icon: ByteArray? = null,
  var order: Long = datetimeNow(), // 用于排序，按照时间顺序来，如果后面调整顺序，就修改这个值
  var mmid: MMID = "",
) {
  @Transient
  val iconImage get() = icon?.toImageBitmap()

  override fun toString(): String {
    return "SystemShortcut(title=$title, mmid=$mmid, data=$data,icon=${icon?.size}, order=$order)"
  }
}

class ShortcutStore(nmm: NativeMicroModule.NativeRuntime) {
  private val shortcutStore = nmm.createStore("shortcut", false)

  suspend fun getAll(): MutableMap<String, SystemShortcut> {
    return shortcutStore.getAll()
  }

  suspend fun set(key: String, data: SystemShortcut) {
    shortcutStore.set(key, data)
  }

  suspend fun delete(key: String) {
    shortcutStore.delete(key)
  }
}