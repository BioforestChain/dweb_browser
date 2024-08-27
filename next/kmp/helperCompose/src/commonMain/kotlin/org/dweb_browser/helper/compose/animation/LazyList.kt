package org.dweb_browser.helper.compose.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastForEach
import org.dweb_browser.helper.contentEquals

@Composable
fun <T> LazyList(
  list: List<T>,
  endAnimationFinished: (T) -> Boolean,
  playEndAnimation: (T) -> Unit,
  getKey: ((T) -> Any?)? = null,
  content: @Composable (List<T>) -> Unit,
) {
  var showList by remember { mutableStateOf<List<T>>(emptyList()) }
  val newShowList = animationList(
    showList,
    list,
    endAnimationFinished = endAnimationFinished,
    playEndAnimation = playEndAnimation,
  )
  content(newShowList)
  if (!showList.contentEquals(newShowList)) {
    showList = newShowList
  }
}

/**
 * 对比两个list，找出要执行 移除动画的元素
 */
private fun <T> animationList(
  preList: List<T>,
  newList: List<T>,
  endAnimationFinished: (T) -> Boolean,
  playEndAnimation: (T) -> Unit,
): List<T> {
  val oldList = preList.toMutableList()
  val resultList = newList.reversed().toMutableList()
  resultList.fastForEach { oldList -= it }
  oldList.fastForEach { item ->
    if (endAnimationFinished(item)) {
      resultList -= item
    } else {
      playEndAnimation(item)
      resultList += item
    }
  }
  return resultList.reversed()
}