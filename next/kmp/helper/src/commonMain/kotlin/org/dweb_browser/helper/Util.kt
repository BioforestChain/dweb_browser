package org.dweb_browser.helper

const val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
const val MB = 1024 * 1024 // 定义MB的计算常量
const val KB = 1024 // 定义KB的计算常量

/**
 * 将Long转为带单位的空间值，如1.11 MB
 */
fun Long.toSpaceSize(): String {
  return if (this > GB) {
    "${((this * 100 / GB) / 100.00f)} GB";
  } else if (this > MB) {
    "${((this * 100 / MB) / 100.00f)} MB";
  } else if (this > KB) { //如果当前Byte的值大于等于1KB
    "${((this * 100 / KB) / 100.00f)} KB";
  } else {
    "${(this * 100 / 100.00f)} B";
  }
}

private fun main() {
  val a = 23634L
  println("a=$a, result=${a.toSpaceSize()}")
}

/**
 * 用于对 或 判断的返回
 */
inline fun <T> T.valueIn(vararg item: T): Boolean {
  return item.contains(this)
}

/**
 * 用于对 或 判断的返回
 */
inline fun <T> T.valueNotIn(vararg item: T): Boolean {
  return !item.contains(this)
}