package org.dweb_browser.helper

const val GB = 1024.0 * 1024 * 1024 // 定义GB的计算常量
const val MB = 1024.0 * 1024 // 定义MB的计算常量
const val KB = 1024.0 // 定义KB的计算常量

/**
 * 将Long转为带单位的空间值，如1.11 MB
 */
fun Long.toSpaceSize(): String {
  return if (this > GB) {
    "${(this / GB).toFixed(2)} GB";
  } else if (this > MB) {
    "${(this / MB).toFixed(2)} MB";
  } else if (this > KB) { //如果当前Byte的值大于等于1KB
    "${(this / KB).toFixed(2)} KB";
  } else {
    "$this B";
  }
}

fun Double.toFixed(digits: Int): String {
  val roundedValue = toString()
  val dotIndex = roundedValue.indexOf('.')
  if (dotIndex == -1) {
    return roundedValue + "." + "0".repeat(digits)
  }
  val end = dotIndex + 1 + digits
  if (end < roundedValue.length) {
    return roundedValue.substring(0, end)
  }
  return roundedValue + "0".repeat(end - roundedValue.length)
}

fun Float.toFixed(digits: Int) = toDouble().toFixed(digits)

/**
 * 用于对 或 判断的返回
 */
fun <T> T.valueIn(vararg item: T): Boolean {
  return item.contains(this)
}

/**
 * 用于对 或 判断的返回
 */
fun <T> T.valueNotIn(vararg item: T): Boolean {
  return !item.contains(this)
}