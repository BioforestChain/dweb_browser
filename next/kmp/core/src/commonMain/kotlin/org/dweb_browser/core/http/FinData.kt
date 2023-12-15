package org.dweb_browser.core.http

class FinData<T : Any>(val concat: (List<T>) -> T) {
  private val chunks = mutableListOf<T>();
  fun append(chunk: T, fin: Boolean): T? = when {
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