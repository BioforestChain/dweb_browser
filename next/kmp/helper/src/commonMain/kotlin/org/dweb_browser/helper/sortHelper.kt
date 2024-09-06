package org.dweb_browser.helper

public class ComparableWrapper<T>(
  public val value: T,
  public val getScore: (T) -> Map<String, Int>,
) :
  Comparable<ComparableWrapper<T>> {
  public companion object {
    public fun <T> builder(getScore: (T) -> Map<String, Int>): ComparableWrapperBuilder<T> =
      ComparableWrapperBuilder(getScore)

    public class ComparableWrapperBuilder<T>(private val getScore: (T) -> Map<String, Int>) {
      public fun build(value: T): ComparableWrapper<T> = ComparableWrapper(value, getScore)
    }
  }

  private var _score: Map<String, Int>? = null
  public val score: Map<String, Int>
    get() {
      if (_score == null) {
        _score = getScore(value)
      }
      return _score!!
    }

  private val _scopeKeys
    get() = score.keys

  /**
   * 等价于 `(this as a) - b`
   */
  override fun compareTo(other: ComparableWrapper<T>): Int {
    val aScore = score
    val bScore = other.score
    _scopeKeys.forEach { key ->
      val aValue = aScore[key] ?: 0
      val bValue = bScore[key] ?: 0
      if (aValue != bValue) {
        return aValue - bValue
      }
    }
    return 0
  }
}

public fun <T> enumToComparable(enumValue: T, enumList: List<T>): Int = enumList.indexOf(enumValue)
public fun <T> enumToComparable(enumValues: Iterable<T>, enumList: List<T>): List<Int> =
  enumValues.map { enumList.indexOf(it) }.sorted()
