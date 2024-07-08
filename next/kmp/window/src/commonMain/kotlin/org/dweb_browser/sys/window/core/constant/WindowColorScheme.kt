package org.dweb_browser.sys.window.core.constant

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.getOrDefault

object WindowColorSchemeSerializer : StringEnumSerializer<WindowColorScheme>(
  "WindowColorScheme",
  WindowColorScheme.ALL_VALUES,
  { scheme })

@Serializable(with = WindowColorSchemeSerializer::class)
enum class WindowColorScheme(val scheme: String) : Comparator<WindowColorScheme> {
  Normal("normal"),
  Light("light"),
  Dark("dark"),
  ;

  companion object {
    private val ALL_VALUE_LIST = entries.toTypedArray()
    val ALL_VALUES = ALL_VALUE_LIST.associateBy { it.scheme }
    fun from(themeName: String) = ALL_VALUES.getOrDefault(themeName, Normal)
  }

  fun next() = ALL_VALUE_LIST[(ALL_VALUE_LIST.indexOf(this) + 1) % ALL_VALUE_LIST.size]

  override fun compare(a: WindowColorScheme, b: WindowColorScheme): Int {
    return a.scheme.compareTo(b.scheme)
  }

  val isLightOrNull
    get() = when (this) {
      Normal -> null
      Light -> true
      Dark -> false
    }
  val isDarkOrNull
    get() = when (this) {
      Normal -> null
      Light -> false
      Dark -> true
    }
}
