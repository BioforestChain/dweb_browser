package org.dweb_browser.browser.desk

import org.dweb_browser.helper.PureIntPoint
import org.dweb_browser.helper.compose.IosLeaveEasing
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.platform.getAnimationFrameMs
import java.awt.Container
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.min


class MagnetEffect(
  val from: Container,
  val isDisabled: () -> Boolean,
  val getEnds: () -> PureIntPoint,
) {

  private var magnetTimer: Timer? = null
    set(value) {
      if (value != field) {
        field = value
        if (value == null) {
          onPlayEnd?.invoke()
        }
      }
    }
  var onPlayEnd: (() -> Unit)? = null

  private val effectDisplayMs get() = from.getAnimationFrameMs()

  /**
   * 磁吸 X 轴动画
   */
  private var xAni: MagnetEffectAni? = null

  /**
   * 磁吸 Y 轴动画
   */
  private var yAni: MagnetEffectAni? = null

  sealed class MagnetEffectAni(
    var start: Int,
    var end: Int,
    val onDraw: (Float) -> Unit,
    val durationMs: Int,
  ) {
    var progress = 0f
    val easing = IosLeaveEasing
    val startTime = datetimeNow()

    fun draw(nowTime: Long) {
      val diffTime = nowTime - startTime
      progress = min(diffTime.toFloat() / durationMs, 1f)
      val aniFraction = easing.transform(progress)
      onDraw(aniFraction)
    }
  }

  private var targetX = -1
  private var targetY = -1
  private var startSetTarget = false
  private fun effectTarget() {
    if (startSetTarget) {
      return
    }
    startSetTarget = true
    SwingUtilities.invokeLater {
      startSetTarget = false
      from.setLocation(targetX, targetY)
    }
  }

  inner class XAni(startX: Int, endX: Int, durationMs: Int) :
    MagnetEffectAni(startX, endX, {
      targetX = (it * (endX - startX) + startX).toInt()
      effectTarget()
    }, durationMs)

  inner class YAni(startY: Int, endY: Int, durationMs: Int) :
    MagnetEffectAni(startY, endY, {
      targetY = (it * (endY - startY) + startY).toInt()
      effectTarget()
    }, durationMs)


  /**
   * 120ms后启动磁吸屏幕边缘的效果
   */
  fun start() {
    if (isDisabled()) {
      return
    }
    if (startEffectJob != null) {
      return
    }
    startEffectJob = Timer(120) {
      startEffectJob = null
      doMagnetEffect()
    }.apply {
      isRepeats = false
      start()
    }
  }

  private var startEffectJob: Timer? = null

  companion object {

    /**
     * 创建粘合动画
     */
    fun <R : MagnetEffectAni> tryCreateEdgeAni(
      start: Int,
      end: Int,
      oldAni: R?,
      create: (Int, Int, Int) -> R,
    ): R? {
      return when (oldAni) {
        null -> if (start == end) null else create(start, end, 500)
        else -> {
          if (oldAni.start != start || oldAni.end != end) {
            create(start, end, 500).also {
              it.progress = oldAni.progress
            }
          } else oldAni
        }
      }
    }

  }

  /**
   * 磁吸屏幕边缘的效果
   */
  private fun doMagnetEffect() {
    if (isDisabled()) {
      return
    }
    /// 计算动画
    val fromBounds = from.bounds
    val (endX, endY) = getEnds()

    /// 对动画进行配置
    xAni = tryCreateEdgeAni(fromBounds.x, endX, xAni) { start, end, ms ->
      XAni(start, end, ms)
    }

    yAni = tryCreateEdgeAni(fromBounds.y, endY, xAni) { start, end, ms ->
      YAni(start, end, ms)
    }

    /// 启动动画
    if (magnetTimer != null) {
      return
    }

    magnetTimer = Timer(effectDisplayMs) {
      if (isDisabled()) {
        magnetTimer?.stop()
        magnetTimer = null
        xAni = null
        yAni = null
        return@Timer
      }

      val now = datetimeNow()
      xAni?.also {
        it.draw(now)
        if (it.progress >= 1f) {
          xAni = null
        }
      }

      yAni?.also {
        it.draw(now)
        if (it.progress >= 1f) {
          yAni = null
        }
      }

      if (xAni == null && yAni == null) {
        magnetTimer?.stop()
        magnetTimer = null
      }

    }.also {
      it.start()
    }
  }

  val isPlay get() = magnetTimer != null

}