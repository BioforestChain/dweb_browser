package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale

@Composable
fun i18nResource(res: SimpleI18nResource): String {
  val language = Locale.current.language
  return res.valuesMap[Language.getLanguage(language)] ?: res.i18nValues.first().second
}

@Composable
fun <T> i18nResource(res: OneParamI18nResource<T>, param: T): String {
  val language = Locale.current.language
  return (res.valuesMap[Language.getLanguage(language)] ?: res.i18nValues.first().second).invoke(param)
}

enum class Language(val code: String) {
  EN("en"), ZH("zh"), ;

  companion object {
    fun getLanguage(language: String) = Language.values().find { it.code == language } ?: ZH
  }
}

class SimpleI18nResource(
  internal val i18nValues: List<Pair<Language, String>>,
) {
  constructor(vararg i18nValues: Pair<Language, String>) : this(
    i18nValues = i18nValues.toList()
  )

  internal val valuesMap = i18nValues.toMap()

  @Composable
  operator fun invoke() = i18nResource(this)
}

typealias OneParam<T> = T.() -> String

class OneParamI18nResource<T>(
  val paramBuilder: () -> T,
  val i18nValues: List<Pair<Language, OneParam<T>>>,
) {
  constructor(paramBuilder: () -> T, vararg i18nValues: Pair<Language, OneParam<T>>) : this(
    paramBuilder, i18nValues.toList()
  )

  internal val valuesMap = mutableMapOf(*i18nValues.toTypedArray())
  fun define(builder: ResourceDefiner<T>.() -> Unit) = this.also {
    ResourceDefiner(it).builder()
  }

  class ResourceDefiner<T>(private val resource: OneParamI18nResource<T>) {
    infix fun Language.to(param: OneParam<T>) {
      resource.valuesMap[this] = param
    }
  }

  @Composable
  operator fun invoke(buildParam: T.() -> Unit) = paramBuilder().let {
    it.buildParam()
    i18nResource(this, it)
  }

  @Composable
  operator fun invoke(param: T) = i18nResource(this, param)
}