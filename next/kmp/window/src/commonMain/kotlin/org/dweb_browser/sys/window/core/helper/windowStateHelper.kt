package org.dweb_browser.sys.window.core.helper

import org.dweb_browser.helper.ComparableWrapper
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.enumToComparable
import org.dweb_browser.core.help.types.ICommonAppManifest
import org.dweb_browser.sys.window.core.WindowState
import kotlin.math.sqrt

fun WindowState.setFromManifest(manifest: ICommonAppManifest) {
  title = manifest.name
  manifest.theme_color?.let {
    themeColor = it
  }
  setIconFromManifest(manifest);
}

fun WindowState.setIconFromManifest(manifest: ICommonAppManifest) {
  /**
   * 挑选合适的图标作为应用的图标
   */
  val iconResource = manifest.icons.let { icons ->
    val comparableBuilder =
      ComparableWrapper.Builder<StrictImageResource> { imageResource ->
        mapOf(
          "purpose" to enumToComparable(
            imageResource.purpose,
            listOf(
              ImageResourcePurposes.Maskable,
              ImageResourcePurposes.Any,
              ImageResourcePurposes.Monochrome
            )
          ).first(),
          "type" to enumToComparable(
            imageResource.type,
            listOf("image/svg+xml", "image/png", "image/jpeg", "image/*")
          ),
          "area" to imageResource.sizes.last().let {
            -it.width * it.height
          }
        )
      }

    icons.minOfOrNull { comparableBuilder.build(StrictImageResource.from(it)) }?.value
  }
  if (iconResource != null) {
    iconUrl = iconResource.src
    iconMaskable = iconResource.purpose.contains(ImageResourcePurposes.Maskable)
    iconMonochrome = iconResource.purpose.contains(ImageResourcePurposes.Monochrome)
  }
}

fun WindowState.setDefaultFloatWindowBounds(
  displayWidth: Float,
  displayHeight: Float,
  seed: Float,
  force: Boolean = false,
) {
  updateMutableBounds {
    if (force || width.isNaN()) {
      width = displayWidth / sqrt(3f)
    }
    if (force || height.isNaN()) {
      height = displayHeight / sqrt(5f)
    }
    /// 在 top 和 left 上，为窗口动态配置坐标，避免层叠在一起
    if (force || left.isNaN()) {
      val maxLeft = displayWidth - width
      val gapSize = 47f; // 质数
      val gapCount = (maxLeft / gapSize).toInt();

      left = gapSize + (seed % gapCount) * gapSize
    }
    if (force || top.isNaN()) {
      val maxTop = displayHeight - height
      val gapSize = 71f; // 质数
      val gapCount = (maxTop / gapSize).toInt();
      top = gapSize + (seed % gapCount) * gapSize
    }
  }
}