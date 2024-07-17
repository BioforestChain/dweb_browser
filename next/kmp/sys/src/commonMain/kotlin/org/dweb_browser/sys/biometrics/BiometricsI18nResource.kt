package org.dweb_browser.sys.biometrics

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object BiometricsI18nResource {
  val default_title = SimpleI18nResource(Language.ZH to "生物识别", Language.EN to "Biometric")
  val default_subtitle = SimpleI18nResource(
    Language.ZH to "验证您的生物识别凭证",
    Language.EN to "verify your biometric credential"
  )
  val cancel_button = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")
  val authentication_failed =
    SimpleI18nResource(Language.ZH to "认证失败", Language.EN to "Authentication failed")
  val authentication_success =
    SimpleI18nResource(Language.ZH to "认证成功", Language.EN to "Authentication success")
}