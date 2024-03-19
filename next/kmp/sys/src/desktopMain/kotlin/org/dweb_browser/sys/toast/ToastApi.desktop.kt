package org.dweb_browser.sys.toast

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.ext.getComposeWindowOrNull
import java.awt.Point
import javax.swing.JOptionPane
import javax.swing.Timer

actual suspend fun showToast(
  microModule: MicroModule,
  text: String,
  durationType: DurationType,
  positionType: PositionType
) {
  val composeWindow = microModule.getComposeWindowOrNull() ?: return
//  val frame = JFrame();
//  frame.setSize(300, 200);
  val optionPane = JOptionPane("Toast", JOptionPane.INFORMATION_MESSAGE);
  val dialog = optionPane.createDialog(composeWindow, text);
  dialog.isModal = false;
  dialog.isVisible = true;
  dialog.location = Point()
  Timer(durationType.duration.toInt()) { dialog.isVisible = false }.start();
}