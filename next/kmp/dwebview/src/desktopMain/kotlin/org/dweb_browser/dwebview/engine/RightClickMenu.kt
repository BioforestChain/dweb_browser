package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.frame.EditorCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DwebViewI18nResource
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.border.BevelBorder
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

// 创建右击菜单
fun Browser.createRightClickMenu(scope: CoroutineScope): Pair<JPopupMenu, Flow<Unit>> {
  val backgroundColor = Color(88, 90, 91)
  val clickEffect = MutableSharedFlow<Unit>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  // 创建右键功能
  fun createMenuItem(title: String, cb: () -> Unit): JMenuItem {
    val menuItem = JMenuItem(title).apply {
      font = Font("Sans-Serif", Font.PLAIN, 12)
      background = backgroundColor
      border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
      isOpaque = false
//          foreground = Color.white
    }
    menuItem.addActionListener {
      cb()
      scope.launch(start = CoroutineStart.UNDISPATCHED) { clickEffect.emit(Unit) }
    }
    return menuItem
  }

  // 绑定命令能力
  fun createCommandMenuItem(title: String, editorCommand: EditorCommand): Component {
    val menuItem = createMenuItem(title) {
      this.mainFrame().ifPresent {
        it.execute(editorCommand)
      }
    }
    return menuItem
  }
  // 创建一个带阴影的边框
  val shadowBorder = BorderFactory.createSoftBevelBorder(BevelBorder.LOWERED)
  // 画样式
  // val matteBorder = BorderFactory.createMatteBorder(0, 0, 0, 0, Color(214, 214, 214))
  val popupMenu = JPopupMenu().apply {
    font = Font("Sans-Serif", Font.PLAIN, 12)
    background = backgroundColor
    border = shadowBorder
    isLightWeightPopupEnabled =false
    // 组合这两个边框
    // border = BorderFactory.createCompoundBorder(matteBorder, shadowBorder)
  }


  // popupMenu事件
  popupMenu.addPopupMenuListener(object : PopupMenuListener {
    //弹出菜单将变为可见
    override fun popupMenuWillBecomeVisible(p0: PopupMenuEvent?) {
      println("QWQ popupMenuWillBecomeVisible")
    }

    //弹出菜单将变为不可见
    override fun popupMenuWillBecomeInvisible(p0: PopupMenuEvent?) {
      println("QWQ popupMenuWillBecomeInvisible")
      scope.launch { clickEffect.emit(Unit) }
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
    }
  })

  // 添加开发者工具
  popupMenu.add(createMenuItem(DwebViewI18nResource.popup_menu_devtool.text) {
    this.devTools().show()
  })

  popupMenu.addSeparator()
  popupMenu.add(
    createCommandMenuItem(
      DwebViewI18nResource.popup_menu_copy.text,
      EditorCommand.copy()
    )
  )
  popupMenu.add(
    createCommandMenuItem(
      DwebViewI18nResource.popup_menu_paste.text,
      EditorCommand.paste()
    )
  )
  popupMenu.add(
    createCommandMenuItem(
      DwebViewI18nResource.popup_menu_select_all.text,
      EditorCommand.selectAll()
    )
  )
  popupMenu.addSeparator()
  //放大
  popupMenu.add(createMenuItem(DwebViewI18nResource.popup_menu_zoomIn.text) {
    this.zoom().`in`()
  })
  // 缩小
  popupMenu.add(createMenuItem(DwebViewI18nResource.popup_menu_zoomOut.text) {
    this.zoom().out()
  })
  // 重置大小
  popupMenu.add(createMenuItem(DwebViewI18nResource.popup_menu_reset.text) {
    this.zoom().reset()
  })
  return Pair(popupMenu, clickEffect)
}