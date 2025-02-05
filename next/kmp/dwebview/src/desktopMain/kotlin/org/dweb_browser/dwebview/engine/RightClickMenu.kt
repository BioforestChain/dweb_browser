package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.frame.EditorCommand
import org.dweb_browser.dwebview.DwebViewI18nResource

sealed interface ContextMenuItem
class ContextMenuAction(val text: String, val icon: String? = null, val onClick: () -> Unit) :
  ContextMenuItem

fun Browser.createExecuteRunner(editorCommand: EditorCommand): () -> Unit {
  return {
    mainFrame().ifPresent {
      it.execute(editorCommand)
    }
  }
}

object contextMenuSeparator : ContextMenuItem

// 创建右击菜单
fun Browser.getContextMenuItems(): List<ContextMenuItem> {
  val items = mutableListOf<ContextMenuItem>()

  // 添加开发者工具
  items += ContextMenuAction(DwebViewI18nResource.popup_menu_devtool.text) {
    this.devTools().show()
  }

  items += contextMenuSeparator
  // 复制
  items += ContextMenuAction(
    text = DwebViewI18nResource.popup_menu_copy.text,
    onClick = createExecuteRunner(EditorCommand.copy())
  )
  // 粘贴
  items += ContextMenuAction(
    text = DwebViewI18nResource.popup_menu_paste.text,
    onClick = createExecuteRunner(EditorCommand.paste())
  )
  // 全选
  items += ContextMenuAction(
    text = DwebViewI18nResource.popup_menu_select_all.text,
    onClick = createExecuteRunner(EditorCommand.selectAll())
  )
  items += contextMenuSeparator
  // 放大
  items += ContextMenuAction(DwebViewI18nResource.popup_menu_zoomIn.text) {
    zoom().`in`()
  }
  // 缩小
  items += ContextMenuAction(DwebViewI18nResource.popup_menu_zoomOut.text) {
    zoom().out()
  }
  // 重置大小
  items += ContextMenuAction(DwebViewI18nResource.popup_menu_reset.text) {
    zoom().reset()
  }
  return items
}