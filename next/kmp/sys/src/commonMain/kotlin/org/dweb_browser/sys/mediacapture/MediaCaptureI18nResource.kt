package org.dweb_browser.sys.mediacapture

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object MediaCaptureI18nResource {
  val request_permission_title_camera = SimpleI18nResource(
    Language.ZH to "相机权限使用说明",
    Language.EN to "Camera permission instructions"
  )
  val request_permission_message_take_picture = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“相机”权限，同意后，将用于为您提供拍照服务",
    Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with photo services"
  )
  val request_permission_message_capture_video = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“相机”权限，同意后，将用于为您提供录像服务",
    Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with video services"
  )

  val permission_denied_take_picture = SimpleI18nResource(
    Language.ZH to "权限被拒绝，无法提供拍照服务",
    Language.EN to "The permission is denied, and the photo service cannot be provided"
  )
  val permission_denied_capture_video = SimpleI18nResource(
    Language.ZH to "权限被拒绝，无法提供录像服务",
    Language.EN to "The permission is denied, and the video service cannot be provided"
  )
  val permission_denied_record_audio = SimpleI18nResource(
    Language.ZH to "权限被拒绝，无法提供录音服务",
    Language.EN to "The permission is denied,  and recording services cannot be provided. Procedure"
  )
  val data_is_null = SimpleI18nResource(
    Language.ZH to "数据为空",
    Language.EN to "The data is null"
  )
  val type_issue = SimpleI18nResource(
    Language.ZH to "类型异常(类型:image,video,audio)",
    Language.EN to "Type Error(type:image,video,audio)"
  )

  val request_permission_title_audio = SimpleI18nResource(
    Language.ZH to "麦克风权限使用说明",
    Language.EN to "MicroPhone permission instructions"
  )
  val request_permission_message_audio = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“麦克风”权限，同意后，将用于为您提供拍照服务",
    Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with photo services"
  )

  val capture_no_found_picture = SimpleI18nResource(
    Language.ZH to "未发现相册信息", Language.EN to "no album found"
  )
  val capture_no_found_video = SimpleI18nResource(
    Language.ZH to "未发现视频信息", Language.EN to "no video found"
  )
  val capture_no_found_audio = SimpleI18nResource(
    Language.ZH to "未发现录音信息", Language.EN to "no audio found"
  )
}