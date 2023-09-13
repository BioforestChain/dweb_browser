package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

@Composable
fun i18nResource(res: SimpleI18nResource): String {
  val lang = LocalLang.current;
  return res.valuesMap[lang] ?: res.i18nValues.first().second
}

enum class Lang(val code: String) {
  EN("en"),
  ZH_CN("zh-cn"),
  ;

  infix fun by(value: String) = Pair(this, value)
}

val LocalLang = compositionLocalOf { Lang.ZH_CN }

class SimpleI18nResource(
  internal val i18nValues: List<Pair<Lang, String>>,
) {
  constructor(vararg i18nValues: Pair<Lang, String>) : this(
    i18nValues = i18nValues.toList()
  )

  internal val valuesMap = i18nValues.toMap()

  val text
    @Composable
    get() = i18nResource(this)
}
