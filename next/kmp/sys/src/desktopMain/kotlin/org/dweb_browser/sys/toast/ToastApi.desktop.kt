package org.dweb_browser.sys.toast

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.platform.getComposeWindowBoundsOrNull
import org.dweb_browser.sys.window.ext.awtIconRoundedImage
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.RoundRectangle2D
import javax.swing.JDialog
import javax.swing.JLabel
import kotlin.properties.Delegates

class Toast(
  val microModule: MicroModule.Runtime,
  val text: String,
  val durationType: ToastDurationType,
  val positionType: ToastPositionType,
  val padding: Int = 10,
  val spacer: Int = 6,
) : JDialog() {

  private lateinit var contentShape: Shape
  private lateinit var iconImage: Image
  private var lineHeight by Delegates.notNull<Int>()
  private val imageSize by lazy { padding + lineHeight + padding - spacer - spacer }
  private val imageRight by lazy { spacer + imageSize + padding }

  companion object {
    private val SHOWING_WM = WeakHashMap<MicroModule.Runtime, JDialog>()

    private suspend fun effectStyle(dialog: Toast) {
      dialog.isUndecorated = true; // 移除窗体装饰
      dialog.background = dialog.background.run { Color(red, green, blue, alpha / 2) }

      val padding = dialog.padding
      val spacer = dialog.spacer

      // 内容
      val label = JLabel(dialog.text, JLabel.RIGHT)
      dialog.iconImage = dialog.microModule.awtIconRoundedImage.await()
      val labelSize = label.preferredSize
      dialog.lineHeight = labelSize.height

      // 布局
      dialog.layout = null // 关闭布局管理器以使用setLocation
      val dialogSize = Dimension(
        dialog.imageRight + labelSize.width + padding,
        padding + labelSize.height + padding
      )
      dialog.size = dialogSize
      label.size = labelSize
      label.setLocation(dialog.imageRight, padding)
      dialog.add(label);

      // 设置窗口圆角
      val round = (padding * 2).toDouble()
      dialog.contentShape = RoundRectangle2D.Double(
        0.0, 0.0, dialog.width.toDouble(), dialog.height.toDouble(), round, round
      )
      dialog.shape = dialog.contentShape

      // 禁止焦点
      dialog.focusableWindowState = false
      dialog.isModal = false
      // 置顶
      dialog.isAlwaysOnTop = true

      // 位置
      dialog.microModule.getComposeWindowBoundsOrNull()?.also { bounds ->
        dialog.location = when (dialog.positionType) {
          ToastPositionType.TOP -> Point(
            (bounds.x + (bounds.width - dialogSize.width) / 2),
            bounds.y,
          )

          ToastPositionType.CENTER -> Point(
            (bounds.x + (bounds.width - dialogSize.width) / 2),
            (bounds.y + (bounds.height - dialogSize.height) / 2),
          )

          ToastPositionType.BOTTOM -> Point(
            (bounds.x + (bounds.width - dialogSize.width) / 2),
            (bounds.y + bounds.height - dialogSize.height),
          )
        }
      }
    }
  }


  suspend fun prepareAndShow() {
    effectStyle(this)
    isVisible = true

  }

  private var autoCloseJob: Job? = null


  override fun setVisible(visible: Boolean) {
    if (visible) {
      SHOWING_WM[microModule]?.isVisible = false
      SHOWING_WM[microModule] = this
      autoCloseJob = microModule.getRuntimeScope().launch {
        delay(durationType.duration)
        isVisible = false
      }
    } else {
      autoCloseJob?.cancel()
      autoCloseJob = null
      SHOWING_WM.remove(microModule, this)
    }
    super.setVisible(visible)
  }

  override fun paint(g: Graphics?) {
    val g2 = g as Graphics2D;
    // 开启抗锯齿
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // 设置内容圆角裁切，需要和 shape 一起合作
    g2.clip = contentShape;
    super.paint(g2);
    g2.drawImage(iconImage, spacer, spacer, imageSize, imageSize, this)
    g2.dispose();
  }
}

actual suspend fun showToast(
  microModule: MicroModule.Runtime,
  text: String,
  durationType: ToastDurationType,
  positionType: ToastPositionType,
) {
  Toast(microModule, text, durationType, positionType).prepareAndShow()
}