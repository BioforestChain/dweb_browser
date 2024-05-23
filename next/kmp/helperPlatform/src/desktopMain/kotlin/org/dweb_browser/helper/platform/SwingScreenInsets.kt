package org.dweb_browser.helper.platform

import androidx.compose.ui.unit.IntRect
import java.awt.Insets
import java.awt.Window


/**
 * 获取窗口所在屏幕的最大可操作的安全区域
 * 参考算法：
 * GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds.bounds
 * = SunGraphicsEnvironment.getUsableBounds(screenDevice)
 * = public static Rectangle getUsableBounds(GraphicsDevice var0) {
 *     GraphicsConfiguration var1 = var0.getDefaultConfiguration();
 *     Insets var2 = Toolkit.getDefaultToolkit().getScreenInsets(var1);
 *     Rectangle var3 = var1.getBounds();
 *     var3.x += var2.left;
 *     var3.y += var2.top;
 *     var3.width -= var2.left + var2.right;
 *     var3.height -= var2.top + var2.bottom;
 *     return var3;
 *   }
 */
fun Window.getScreenBounds() = with(getSafeAreaInsets()) {
  /// 获取屏幕大小
  with(graphicsConfiguration.bounds) {
    IntRect(left = left, top = top, right = width - right, bottom = height - bottom)
  }
}

/**
 * 获取窗口锁在位置的屏幕安全区域
 */
fun Window.getSafeAreaInsets(): Insets = toolkit.getScreenInsets(graphicsConfiguration)