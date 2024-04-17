package org.dweb_browser.sys.toast

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.ext.getComposeWindowBoundsOrNull
import java.awt.Point
import javax.swing.JOptionPane
import javax.swing.Timer

actual suspend fun showToast(
  microModule: MicroModule, text: String, durationType: DurationType, positionType: PositionType
) {
  // val composeWindow = microModule.getComposeWindowOrNull() ?: return
  // val composeBounds = composeWindow.bounds // 这个窗口获取到的数据是主窗口，并非当前打开的窗口
  val composeBounds = microModule.getComposeWindowBoundsOrNull() ?: return

  val optionPane = JOptionPane(text, JOptionPane.INFORMATION_MESSAGE)
  //val dialog = optionPane.createDialog(composeWindow, microModule.mmid) // 使用composeWindow会聚焦到主窗口
  val dialog = optionPane.createDialog(microModule.mmid)
  dialog.isModal = false
  dialog.isVisible = true

  val dialogSize = dialog.size

  when (positionType) {
    PositionType.TOP -> {
      dialog.location = Point(
        (composeBounds.x + (composeBounds.width - dialogSize.width) / 2).toInt(),
        composeBounds.y.toInt()
      )
    }

    PositionType.CENTER -> {
      dialog.location = Point(
        (composeBounds.x + (composeBounds.width - dialogSize.width) / 2).toInt(),
        (composeBounds.y + (composeBounds.height - dialogSize.height) / 2).toInt()
      )
    }

    PositionType.BOTTOM -> {
      dialog.location =
        Point(
          (composeBounds.x + (composeBounds.width - dialogSize.width) / 2).toInt(),
          (composeBounds.y + composeBounds.height - dialogSize.height).toInt()
        )
    }
  }

  Timer(durationType.duration.toInt()) { dialog.isVisible = false }.start()
}