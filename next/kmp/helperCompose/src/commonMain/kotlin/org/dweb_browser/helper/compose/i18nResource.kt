package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun LanguageWatch() {
  val language = LocalLanguage.current
  val config = LocalConfiguration.current
  LaunchedEffect(config) {
    snapshotFlow { config.locales[0] }.collect { locale ->
      val currentLanguage = locale.language // language zh, toString() zh_CN/zh_HK_#Hant/zh_TW_#Hant
      language.value = Language.values().find { currentLanguage == it.code } ?: Language.EN
    }
  }
}

@Composable
fun i18nResource(res: SimpleI18nResource): String {
  val language = LocalLanguage.current
  return res.valuesMap[language.value] ?: res.i18nValues.first().second
}

enum class Language(val code: String) {
  EN("en"),
  ZH("zh"),
  ;

  infix fun by(value: String) = Pair(this, value)
}

val LocalLanguage = compositionLocalOf { mutableStateOf(Language.ZH) }

class SimpleI18nResource(
  internal val i18nValues: List<Pair<Language, String>>,
) {
  constructor(vararg i18nValues: Pair<Language, String>) : this(
    i18nValues = i18nValues.toList()
  )

  internal val valuesMap = i18nValues.toMap()

  val text
    @Composable
    get() = i18nResource(this)
}
