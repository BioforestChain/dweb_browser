package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.intl.Locale
import org.dweb_browser.helper.Debugger

val debugI18n = Debugger("i18n")

@Composable
private fun i18nResource(res: SimpleI18nResource): String {
  val language = Locale.current.language
  return res.valuesMap[Language.getLanguage(language)]
    ?: res.i18nValues.firstOrNull()?.second ?: "Undefined"
}

@Composable
private fun <T> i18nResource(res: OneParamI18nResource<T>, param: T): String {
  val language = Locale.current.language
  return (res.valuesMap[Language.getLanguage(language)]
    ?: res.i18nValues.firstOrNull()?.second)?.invoke(param) ?: "Undefined"
}

enum class Language(val code: String) {
  EN("en"), ZH("zh"),
  ;

  companion object {
    /**
     * 跟随操作系统的设置，初始化语言
     */
    val current get() = Language.getLanguage(Locale.current.language)
    private val languageMap = entries.associateBy { it.code }
    fun getLanguage(language: String) = languageMap[language] ?: ZH
  }
}

class SimpleI18nResource(
  internal val i18nValues: List<Pair<Language, String>>, ignoreWarn: Boolean = false,
) {
  constructor(vararg i18nValues: Pair<Language, String>, ignoreWarn: Boolean = false) : this(
    i18nValues = i18nValues.toList(),
    ignoreWarn = ignoreWarn,
  )

  init {
    if (!ignoreWarn && i18nValues.size == 1) {
      debugI18n("i18n is missing", i18nValues.first().second)
    }
  }

  internal val valuesMap = i18nValues.toMap()

  @Composable
  operator fun invoke() = i18nResource(this)

  /**
   * 这个值不能用于Compose界面显示，目前仅用于实时获取的文本。
   */
  val text get() = valuesMap[Language.current] ?: i18nValues.firstOrNull()?.second ?: "Undefined"
}

open class I18n {
  companion object {
    fun zh(zh: String, en: String) = SimpleI18nResource(Language.ZH to zh, Language.EN to en)
    fun zh1(zh: State<String>.() -> String, en: State<String>.() -> String) =
      OneParamI18nResource({ mutableStateOf("") }, Language.ZH to zh, Language.EN to en)

    data class Zh2(var value1: String, var value2: String)

    fun zh2(zh: Zh2.() -> String, en: Zh2.() -> String) =
      OneParamI18nResource({ Zh2("", "") }, Language.ZH to zh, Language.EN to en)
  }
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


  /**
   * 这个值不能用于Compose界面显示，目前仅用于实时获取的文本。
   */
  fun text(param: T) =
    (valuesMap[Language.current] ?: i18nValues.firstOrNull()?.second)?.invoke(param) ?: "Undefined"

  fun text(buildParam: T.() -> Unit) = paramBuilder().let {
    it.buildParam()
    text(it)
  }
}