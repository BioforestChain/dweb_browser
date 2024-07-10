package org.dweb_browser.sys.keychain


import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object KeychainI18nResource {
  val name = SimpleI18nResource(Language.ZH to "钥匙串访问", Language.EN to "keychain store")
  val short_name = SimpleI18nResource(Language.ZH to "钥匙串", Language.EN to "keychain")
  val require_auth_subtitle = SimpleI18nResource(
    Language.ZH to "使用钥匙串访问需要您的授权",
    Language.EN to "Using Keychain store requires your authorization."
  )
}