package org.dweb_browser.sys.toast

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.ext.getComposeWindowOrNull
import java.awt.Point
import java.awt.Toolkit
import javax.swing.JOptionPane
import javax.swing.Timer

actual suspend fun showToast(
  microModule: MicroModule, text: String, durationType: DurationType, positionType: PositionType
) {
  val composeWindow = microModule.getComposeWindowOrNull() ?: return
  val optionPane = JOptionPane(text, JOptionPane.INFORMATION_MESSAGE)
  val dialog = optionPane.createDialog(composeWindow, microModule.mmid)
  dialog.isModal = false
  dialog.isVisible = true

  val screenSize = composeWindow.size
  val dialogSize = dialog.size

  when (positionType) {
    PositionType.TOP -> {
      dialog.location = Point((screenSize.width - dialogSize.width) / 2, 0)
    }

    PositionType.CENTER -> {
      dialog.location = Point(
        (screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2
      )
    }

    PositionType.BOTTOM -> {
      dialog.location =
        Point((screenSize.width - dialogSize.width) / 2, screenSize.height - dialogSize.height)
    }
  }

  Timer(durationType.duration.toInt()) { dialog.isVisible = false}.start()
}