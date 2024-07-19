package org.dweb_browser.sys.keychain


import org.dweb_browser.helper.compose.I18n

object KeychainI18nResource : I18n() {
  val name = zh("钥匙串访问", "keychain store")
  val short_name = zh("钥匙串", "keychain")
  val require_auth_subtitle =
    zh("使用钥匙串访问需要您的授权", "Using Keychain store requires your authorization.")
  val no_select_detail = zh("未选择要展示的详情", "select item to show details")

  val password_mode_label_utf8 = zh("文本", "Utf8 Text")
  val password_mode_label_base64 = zh("Base64编码", "Base64 Encoding")

  val password_mode_label_binary = zh("二进制", "Binary Code")

  val password_action_view = zh("查看", "View")
  val password_action_edit = zh("修改", "Edit")
  val password_action_delete = zh("删除", "Delete")


  val password_copy = zh("复制密码", "Copy Password")
  val password_save = zh("更新密码", "Update Password")
  val password_base64_url_mode = zh("Url模式", "Url Mode")
  val password_binary_hex_mode = zh("Hex模式", "Hex Mode")

  val password_delete_tip =
    zh1({ "确定要删除密码项： $value" }, { "Confirm the deletion of the password entry: $value" })
  val password_encode_error = zh("密码的编码错误", "Failed password encoding")
  val password_decode_error = zh("密码的解码失败", "Failed password decoding")


  val keychain_get_title =
    zh("应用想要访问您的钥匙串中保存的密钥", "The app will read your keychain.")
  val keychain_get_description = zh1({ "读取钥匙: $value" }, { "read by key: $value" })
  val keychain_set_title = zh("应用想要使用您的钥匙串保存密钥", "The app will write your keychain.")
  val keychain_set_description = zh1({ "保存钥匙: $value" }, { "write by key: $value" })
  val keychain_delete_title =
    zh("应用想要删除您的钥匙串保存密钥", "The app will delete your keychain.")
  val keychain_delete_description = zh1({ "删除钥匙: $value" }, { "delete by key: $value" })

  val biometrics_verify_submit = zh("同意并开始验证身份", "Agree and start authentication")
}