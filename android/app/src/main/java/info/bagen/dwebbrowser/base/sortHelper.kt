package info.bagen.dwebbrowser.base

class ComparableWrapper<T>(val value: T, val getScore: (T) -> Map<String, Int>) :
  Comparable<ComparableWrapper<T>> {
  companion object {
    fun <T> Builder(getScore: (T) -> Map<String, Int>) = ComparableWrapperBuilder(getScore)

    class ComparableWrapperBuilder<T>(val getScore: (T) -> Map<String, Int>) {
      fun build(value: T) = ComparableWrapper(value, getScore)
    }
  }

  private var _score: Map<String, Int>? = null
  val score: Map<String, Int>
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

fun <T> enumToComparable(enumValue: T, enumList: List<T>) = enumList.indexOf(enumValue)
fun <T> enumToComparable(enumValues: Iterable<T>, enumList: List<T>) =
  enumValues.map { enumList.indexOf(it) }.sorted()
