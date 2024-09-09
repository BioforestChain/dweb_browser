package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.RadioButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import squircleshape.SquircleShape

internal class ActivityControllerDevParams {
  var showDevLayer by mutableStateOf(false)
  sealed interface ResizePolicy {
    companion object Enum {
      data object ImmediateResize : ResizePolicy

      /**
       * 是否减少 resize 的发生
       * 在windows上，最好不好进行resize，resize会导致视图消失，开关双缓冲也没用，目前无解，所以只能给一个尽可能大的空间来绘制
       * 但是，好在windows上，透明区域默认是点击穿透的
       */
      data object ReduceResize : ResizePolicy

      /**
       * 延迟策略
       * 使用
       * 按需回收
       */
      data class LazyResize(
        val sampleTime: Long = defaultTime,
        val safePadding: IntSize = defaultSafePadding,
      ) : ResizePolicy {
        companion object {
          val defaultTime = 100L
          val defaultSafePadding = IntSize(80, 20)
        }

        var dirty by mutableStateOf(false)
        var delayDoneJob: Job? = null
      }

      /**
       * 自定义宽高
       */
      data class CustomResize(val width: Int, val height: Int) : ResizePolicy
    }
  }

  var resizePolicy by mutableStateOf<ResizePolicy>(ResizePolicy.Enum.ImmediateResize)

  enum class TranslateMode {
    Auto, TopStart, Center, EndBottom,
  }

  @Composable
  fun Render(avc: ActivityViewController) {
    val devParams = this
    Box(Modifier.fillMaxSize()) {
      Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
          Text("打开图层辅助")
          Switch(devParams.showDevLayer, { devParams.showDevLayer = it })
        }
        HorizontalDivider()
        Column(
          Modifier.background(
            MaterialTheme.colorScheme.background.copy(alpha = 0.2f), shape = SquircleShape(8.dp)
          )
        ) {
          Text("setSize策略", style = MaterialTheme.typography.titleMedium)
          Row {
            RadioButton(
              resizePolicy == ResizePolicy.Enum.ImmediateResize,
              { resizePolicy = ResizePolicy.Enum.ImmediateResize },
            )
            Text("Immediate")
          }
          Row {
            RadioButton(
              resizePolicy == ResizePolicy.Enum.ReduceResize,
              { resizePolicy = ResizePolicy.Enum.ReduceResize },
            )
            Text("Reduce")
          }
          Row {
            var time by remember { mutableStateOf(ResizePolicy.Enum.LazyResize.defaultTime) }
            var safePadding by remember { mutableStateOf(ResizePolicy.Enum.LazyResize.defaultSafePadding) }
            RadioButton(
              resizePolicy is ResizePolicy.Enum.LazyResize,
              { resizePolicy = ResizePolicy.Enum.LazyResize(time, safePadding) },
            )
            Text("Lazy")
            TextField(
              "$time", { value -> value.toLongOrNull()?.also { time = it } },
              label = { Text("time") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            TextField(
              "${safePadding.width},${safePadding.height}",
              { value ->
                runCatching {
                  val (width, height) = value.split(Regex( "[,x\\s]+"))
                  safePadding = IntSize(width.toInt(), height.toInt())
                }
              },
              label = { Text("safePadding") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
          }
          Row {
            Text("Custom")
            var width by remember { mutableIntStateOf(avc.dialog.width) }
            var height by remember { mutableIntStateOf(avc.dialog.height) }
            val isEnabled = resizePolicy is ResizePolicy.Enum.CustomResize
            RadioButton(
              isEnabled,
              { resizePolicy = ResizePolicy.Enum.CustomResize(width, height) },
            )
            TextField(
              "$width",
              { width = it.toIntOrNull() ?: width },
              label = { Text("width") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            TextField(
              "$height",
              { height = it.toIntOrNull() ?: height },
              label = { Text("height") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (isEnabled) {
              LaunchedEffect(width, height) {
                avc.dialog.setSize(width, height)
              }
            }
          }
        }
        HorizontalDivider()
        ActivityDevPanel(avc.controller)
      }
    }
  }
}