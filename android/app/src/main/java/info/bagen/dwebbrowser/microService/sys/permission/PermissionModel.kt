package info.bagen.dwebbrowser.microService.sys.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.R

/** 系统调用函数*/
enum class EPermission(val type: String) {
  PERMISSION_CALENDAR("PERMISSION_CALENDAR"),
  PERMISSION_CAMERA("PERMISSION_CAMERA"),
  PERMISSION_CONTACTS("PERMISSION_CONTACTS"),
  PERMISSION_LOCATION("PERMISSION_LOCATION"),
  PERMISSION_RECORD_AUDIO("PERMISSION_RECORD_AUDIO"),
  PERMISSION_BODY_SENSORS("PERMISSION_BODY_SENSORS"),
  PERMISSION_STORAGE("PERMISSION_STORAGE"),
  PERMISSION_SMS("PERMISSION_SMS"),
  PERMISSION_CALL("PERMISSION_CALL"),
  PERMISSION_DEVICE("PERMISSION_DEVICE")
}

internal fun getActualPermissions(permission: String): ArrayList<String> {
  val actualPermissions = arrayListOf<String>()
  if (permission.contains(",")) {
    permission.split(",").forEach {
      actualPermissions.addAll(getActualPermissions(it))
    }
    return actualPermissions
  }
  when (permission) {
    EPermission.PERMISSION_CAMERA.type -> actualPermissions.add(Manifest.permission.CAMERA)
    EPermission.PERMISSION_RECORD_AUDIO.type -> actualPermissions.add(Manifest.permission.RECORD_AUDIO)
    EPermission.PERMISSION_BODY_SENSORS.type -> actualPermissions.add(Manifest.permission.BODY_SENSORS)
    EPermission.PERMISSION_DEVICE.type -> actualPermissions.add(Manifest.permission.READ_PHONE_STATE)
    EPermission.PERMISSION_CALENDAR.type -> {
      actualPermissions.add(Manifest.permission.READ_CALENDAR)
      actualPermissions.add(Manifest.permission.WRITE_CALENDAR)
    }

    EPermission.PERMISSION_CONTACTS.type -> {
      actualPermissions.add(Manifest.permission.READ_CONTACTS)
      actualPermissions.add(Manifest.permission.WRITE_CONTACTS)
      actualPermissions.add(Manifest.permission.GET_ACCOUNTS)
    }

    EPermission.PERMISSION_LOCATION.type -> {
      actualPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
      actualPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    EPermission.PERMISSION_STORAGE.type -> {
      actualPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
      actualPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    EPermission.PERMISSION_SMS.type -> {
      actualPermissions.add(Manifest.permission.SEND_SMS)
      actualPermissions.add(Manifest.permission.RECEIVE_SMS)
      actualPermissions.add(Manifest.permission.READ_SMS)
      actualPermissions.add(Manifest.permission.RECEIVE_WAP_PUSH)
      actualPermissions.add(Manifest.permission.RECEIVE_MMS)
    }

    EPermission.PERMISSION_CALL.type -> {
      actualPermissions.add(Manifest.permission.CALL_PHONE)
      actualPermissions.add(Manifest.permission.USE_SIP)
      actualPermissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS)
      actualPermissions.add(Manifest.permission.ADD_VOICEMAIL)
      actualPermissions.add(Manifest.permission.READ_CALL_LOG)
      actualPermissions.add(Manifest.permission.WRITE_CALL_LOG)
    }

    else -> {} // actualPermissions.add(permission) // 如果都不匹配，直接将请求的权限填充
  }
  return actualPermissions
}

internal fun getDenyDialogText(permission: String): String {
  return when (permission) {
    Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR -> {
      App.appContext.getString(R.string.permission_deny_calendar)
    }

    Manifest.permission.CAMERA -> {
      App.appContext.getString(R.string.permission_deny_camera)
    }

    Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
    Manifest.permission.GET_ACCOUNTS -> {
      App.appContext.getString(R.string.permission_deny_contacts)
    }

    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> {
      App.appContext.getString(R.string.permission_deny_location)
    }

    Manifest.permission.RECORD_AUDIO -> {
      App.appContext.getString(R.string.permission_deny_record_audio)
    }

    Manifest.permission.READ_PHONE_STATE -> {
      App.appContext.getString(R.string.permission_deny_device)
    }

    Manifest.permission.BODY_SENSORS -> {
      App.appContext.getString(R.string.permission_deny_sensor)
    }

    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
      App.appContext.getString(R.string.permission_deny_storage)
    }

    Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG,
    Manifest.permission.WRITE_CALL_LOG, Manifest.permission.ADD_VOICEMAIL,
    Manifest.permission.USE_SIP, Manifest.permission.PROCESS_OUTGOING_CALLS -> {
      App.appContext.getString(R.string.permission_deny_call)
    }

    Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
    Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_WAP_PUSH,
    Manifest.permission.RECEIVE_MMS -> {
      App.appContext.getString(R.string.permission_deny_sms)
    }

    else -> {
      App.appContext.getString(R.string.permission_deny_text)
    }
  }
}

internal fun openAppSettings() {
  val i = Intent()
  i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
  i.addCategory(Intent.CATEGORY_DEFAULT)
  i.data = Uri.parse("package:" + App.appContext.packageName)
  i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
  i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
  App.appContext.startActivity(i)
}