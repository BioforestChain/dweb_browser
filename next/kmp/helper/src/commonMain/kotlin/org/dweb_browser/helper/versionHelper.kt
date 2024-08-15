package org.dweb_browser.helper

/**
 * 比较版本的大小
 */
public fun String.isGreaterThan(compare: String): Boolean {
  if (this.isEmpty()) return false
  if (compare.isEmpty()) return true
  val thisSplit = this.split(".")
  val compareSplit = compare.split(".")
  val minLength = minOf(thisSplit.size, compareSplit.size)
  try {
    for (index in 0 until minLength) {
      val source = thisSplit[index].toInt()
      val target = compareSplit[index].toInt()
      if (source == target) continue
      return source > target // 除非一样，否则直接返回结果
    }
  } catch (e: Exception) {
    printError("isGreaterThan", "fail to compare $this and $compare", e)
    return false
  }
  // 按照最小的长度判断，都相同时，则根据长度直接返回
  return thisSplit.size > compareSplit.size
}