package org.dweb_browser.sys.tray

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

data class TrayItem(
  val id: String? = null,
  val type: TRAY_ITEM_TYPE = TRAY_ITEM_TYPE.Item,
  val title: String,
  val parent: String? = null,
  val url: String? = null,
  val group: String? = null,
  val enabled: Boolean? = null,
  val mnemonic: Char? = null,
  val icon: String? = null,
  val shortcut: SysKeyShortcut? = null,
)

enum class TRAY_ITEM_TYPE {
  Menu,
  Item,
  ;

  companion object {
    val ALL = entries.associateBy { it.name }
  }

}

internal class TrayComposeItem(
  val id: String,
  val type: TRAY_ITEM_TYPE,
  val title: String,
  parent: TrayComposeItem? = null,
  val url: String? = null,
  val group: String? = null,
  val enabled: Boolean = true,
  val mnemonic: Char? = null,
  val icon: String? = null,
  val shortcut: SysKeyShortcut? = null,
  children: List<TrayComposeItem> = listOf(),
) {
  var parent: TrayComposeItem? = parent
    private set
  val children = MutableStateFlow(children)
  fun findById(findId: String): TrayComposeItem? {
    if (id == findId) {
      return this
    }
    for (child in children.value) {
      child.findById(findId)?.also { return it }
    }
    return null
  }

  fun replaceChild(oldChild: TrayComposeItem?, newChild: TrayComposeItem) {
    if (oldChild == newChild || oldChild == this || newChild == this) {
      return
    }
    if (oldChild == null || oldChild.parent != this) {
      return addChild(newChild)
    }

    children.value = children.value.map {
      when (it) {
        oldChild -> newChild
        else -> it
      }
    }

    newChild.parent = this
    oldChild.parent = null
  }

  fun addChild(child: TrayComposeItem) {
    if (child == this) {
      return
    }
    if (child.parent == this) {
      return
    }
    child.remove()
    child.parent = this
    children.value += child
  }

  fun removeChild(child: TrayComposeItem) {
    if (child == this) {
      return
    }
    if (child.parent == this) {
      children.value -= child
      child.parent = null
    }
  }

  fun remove() {
    parent?.removeChild(this)
  }
}

@Serializable
data class SysKeyShortcut(
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
//  val asComposeKeyShortcut by lazy {
//    androidx.compose.ui.input.key.KeyShortcut(
//      key = Key(keyCode, keyLocation),
//      ctrl = ctrl,
//      meta = meta,
//      alt = alt,
//      shift = shift,
//    )
//  }
}