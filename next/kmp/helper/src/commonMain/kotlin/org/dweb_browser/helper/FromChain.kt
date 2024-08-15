package org.dweb_browser.helper

public interface IFrom {
  public val from: Any?
  public fun <T> findFrom(filter: (Any) -> T?): T? {
    var current = from;
    while (current != null) {
      val found = filter(current)
      if (found != null) {
        return found
      }
      if (current is IFrom) {
        current = current.from
      } else {
        break
      }
    }
    return null
  }
}