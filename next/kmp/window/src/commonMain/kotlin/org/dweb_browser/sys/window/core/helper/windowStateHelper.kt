package org.dweb_browser.sys.window.core.helper

import org.dweb_browser.core.help.types.ICommonAppManifest
import org.dweb_browser.helper.ComparableWrapper
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.enumToComparable
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import kotlin.math.sqrt

/**设置manifest状态*/
fun WindowController.setStateFromManifest(manifest: ICommonAppManifest) {
  val win = this
  val windowState = win.state
  windowState.title = manifest.name
  manifest.theme_color?.let {
    windowState.themeColor = it
  }

  /**
   * 挑选合适的图标作为应用的图标
   */
  val iconResource = manifest.icons.toStrict().pickLargest()
  if (iconResource != null) {
    windowState.iconUrl = iconResource.src
    windowState.iconMaskable = iconResource.purpose.contains(ImageResourcePurposes.Maskable)
    windowState.iconMonochrome = iconResource.purpose.contains(ImageResourcePurposes.Monochrome)
  }
}

private val comparableBuilder =
  ComparableWrapper.builder<StrictImageResource> { imageResource ->
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

fun List<ImageResource>.toStrict(baseUri: String? = null) =
  map { StrictImageResource.from(it, baseUri) }

/**
 * 选择最大的图标
 */
fun List<StrictImageResource>.pickLargest() =
  minOfOrNull { comparableBuilder.build(it) }?.value

/**
 * 选择最小的图标
 */
fun List<StrictImageResource>.pickMinimal() =
  maxOfOrNull { comparableBuilder.build(it) }?.value


/**设置默认窗口边界*/
fun WindowState.setDefaultFloatWindowBounds(
  maxWindowWidth: Float,
  maxWindowHeight: Float,
  seed: Float,
  force: Boolean = false,
) {
  updateMutableBounds {
    if (force || width.isNaN()) {
      width = maxWindowWidth / sqrt(3f)
    }
    if (force || height.isNaN()) {
      height = maxWindowHeight / sqrt(5f)
    }
    /// 在 top 和 left 上，为窗口动态配置坐标，避免层叠在一起
    if (force || x.isNaN()) {
      val maxLeft = maxWindowWidth - width
      val gapSize = 47f; // 质数
      val gapCount = (maxLeft / gapSize).toInt();

      x = gapSize + (seed % gapCount) * gapSize
    }
    if (force || y.isNaN()) {
      val maxTop = maxWindowHeight - height
      val gapSize = 71f; // 质数
      val gapCount = (maxTop / gapSize).toInt();
      y = gapSize + (seed % gapCount) * gapSize
    }
  }
}