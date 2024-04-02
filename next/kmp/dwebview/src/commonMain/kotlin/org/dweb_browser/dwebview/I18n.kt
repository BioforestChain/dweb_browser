package org.dweb_browser.dwebview

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource


object DwebViewI18nResource {
  val alert_action_ok = SimpleI18nResource(Language.ZH to "确定", Language.EN to "OK")
  val confirm_action_confirm = SimpleI18nResource(Language.ZH to "确定", Language.EN to "Confirm")
  val confirm_action_cancel = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")
  val prompt_action_confirm = SimpleI18nResource(Language.ZH to "确定", Language.EN to "Confirm")
  val prompt_action_cancel = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")

  val permission_tip_camera_title = SimpleI18nResource(
    Language.ZH to "相机权限使用说明",
    Language.EN to "Camera Permission Instructions"
  )
  val permission_tip_camera_message = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“相机”权限，同意后，将用于为您提供拍照、录像服务",
    Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with photo and video services"
  )
  val permission_tip_microphone_title = SimpleI18nResource(
    Language.ZH to "麦克风权限使用说明",
    Language.EN to "Microphone Permission Instructions"
  )
  val permission_tip_microphone_message = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“麦克风”权限，同意后，将用于为您提供语音相关的服务",
    Language.EN to "DwebBrowser is asking you for \"Microphone\" permissions, and if you agree, it will be used to provide you with voice-related services"
  )
  val popup_menu_devtool = SimpleI18nResource(
    Language.EN to "Devtool",
    Language.ZH to "开发者工具",
  )
  val popup_menu_copy = SimpleI18nResource(
    Language.EN to "Copy",
    Language.ZH to "复制",
  )
  val popup_menu_paste = SimpleI18nResource(
    Language.EN to "Paste",
    Language.ZH to "粘贴",
  )
  val popup_menu_select_all = SimpleI18nResource(
    Language.EN to "Select All",
    Language.ZH to "选择全部",
  )
  val popup_menu_reset = SimpleI18nResource(
    Language.EN to "Actual Size",
    Language.ZH to "重置",
  )
  val popup_menu_zoomIn = SimpleI18nResource(
    Language.EN to "Zoom In",
    Language.ZH to "放大",
  )
  val popup_menu_zoomOut = SimpleI18nResource(
    Language.EN to "Zoom Out",
    Language.ZH to "缩小",
  )
}