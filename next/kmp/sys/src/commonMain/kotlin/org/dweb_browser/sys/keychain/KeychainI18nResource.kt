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
  val no_select_detail = SimpleI18nResource(
    Language.ZH to "未选择要展示的详情",
    Language.EN to "select item to show details",
  )

  val password_mode_label_utf8 =
    SimpleI18nResource(Language.ZH to "文本", Language.EN to "Utf8 Text")
  val password_mode_label_base64 =
    SimpleI18nResource(Language.ZH to "Base64编码", Language.EN to "Base64 Encoding")

  val password_mode_label_binary =
    SimpleI18nResource(Language.ZH to "二进制", Language.EN to "Binary Code")

  val password_copy =
    SimpleI18nResource(Language.ZH to "复制密码", Language.EN to "Copy Password")
  val password_base64_url_mode =
    SimpleI18nResource(Language.ZH to "Url模式", Language.EN to "Url Mode")
  val password_binary_hex_mode =
    SimpleI18nResource(Language.ZH to "Hex模式", Language.EN to "Hex Mode")
}