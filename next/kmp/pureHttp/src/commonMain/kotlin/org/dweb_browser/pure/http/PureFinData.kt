package org.dweb_browser.pure.http

class PureFinData<T : Any>(val concat: (List<T>) -> T) {
  private val chunks = mutableListOf<T>();
  fun append(chunk: T, fin: Boolean): T? = run {
    when {
      fin -> when {
        chunks.isEmpty() -> chunk
        else -> concat(chunks + chunk).also {
          chunks.clear()
        }
      }

      else -> {
        chunks += chunk
        null
      }
    }
  }

  companion object {
    fun text() = PureFinData<String> { list -> list.reduce { acc, bytes -> acc + bytes } }
    fun binary() = PureFinData<ByteArray> { list ->
      list.reduce { acc, bytes -> acc + bytes }
    }
  }
}