package org.dweb_browser.helper

import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@OptIn(kotlinx.coroutines.FlowPreview::class)
suspend fun <T> debounce(
  delayMillis: Long,
  block: suspend () -> T
): T = flow { emit(block()) }.debounce(delayMillis).first()

/**
 * 将Long转为带单位的空间值，如1.11 MB
 */
fun Long.toSpaceSize() : String {
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  return if (this > GB) {
    "${((this * 100 / GB) / 100.0f)} GB";
  } else if (this > MB) {
    "${((this * 100 / MB) / 100.0f)} MB";
  } else if (this > KB) { //如果当前Byte的值大于等于1KB
    "${((this * 100 / KB) / 100.0f)} KB";
  } else {
    "${(this * 100 / 100.0f)} B";
  }
}

private fun main() {
  val a = 23634L
  println("a=$a, result=${a.toSpaceSize()}")
}