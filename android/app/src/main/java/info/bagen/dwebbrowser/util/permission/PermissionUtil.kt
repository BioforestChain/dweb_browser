package info.bagen.dwebbrowser.util.permission

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import info.bagen.dwebbrowser.App
import org.dweb_browser.microservice.help.gson

object PermissionUtil {
  /*const val PERMISSION_CALENDAR = "info.bagen.rust.plaoc.CALENDAR"
  const val PERMISSION_CAMERA = "info.bagen.rust.plaoc.CAMERA"
  const val PERMISSION_CONTACTS = "info.bagen.rust.plaoc.CONTACTS"
  const val PERMISSION_LOCATION = "info.bagen.rust.plaoc.LOCATION" // 位置
  const val PERMISSION_RECORD_AUDIO = "info.bagen.rust.plaoc.RECORD_AUDIO" // 录音
  const val PERMISSION_BODY_SENSORS = "info.bagen.rust.plaoc.BODY_SENSORS" // 传感器（重力，陀螺仪）
  const val PERMISSION_STORAGE = "info.bagen.rust.plaoc.STORAGE" // 存储
  const val PERMISSION_SMS = "info.bagen.rust.plaoc.SMS" // 短信
  const val PERMISSION_CALL = "info.bagen.rust.plaoc.CALL" // 电话
  const val PERMISSION_DEVICE = "info.bagen.rust.plaoc.DEVICE" // （手机状态）

  const val PERMISSION_PHOTO = "info.bagen.rust.plaoc.photo" // 相册
  const val PERMISSION_MEDIA = "info.bagen.rust.plaoc.MEDIA" // 媒体库
  const val PERMISSION_NETWORK = "info.bagen.rust.plaoc.NETWORK" // 网络
  const val PERMISSION_NOTIFICATION = "info.bagen.rust.plaoc.NOTIFICATION" // 通知
  const val PERMISSION_BLUETOOTH = "info.bagen.rust.plaoc.BLUETOOTH" // 蓝牙*/
  /**
   * 判断是否申请过系统权限
   */
  @Synchronized
  fun isPermissionsGranted(permission: String): Boolean {
    var permissionList = getActualPermissions(permission)
    permissionList.forEach { pm ->
      if (!isPermissionGranted(pm)) return false
    }
    return true
  }

  @Synchronized
  fun isPermissionsGranted(permissions: ArrayList<String>): Boolean {
    val permissionList = getActualPermissions(permissions)
    permissionList.forEach { pm ->
      if (!isPermissionGranted(pm)) return false
    }
    return true
  }

  /**
   * 打开App设置界面
   */
  fun openAppSettings() {
    val i = Intent()
    i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    i.addCategory(Intent.CATEGORY_DEFAULT)
    i.data = Uri.parse("package:" + App.appContext.packageName)
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    App.appContext.startActivity(i)
  }

  @Synchronized
  private fun isPermissionGranted(permission: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
      return App.appContext.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    val hasPermission =
      ContextCompat.checkSelfPermission(App.appContext, permission)
    Manifest.permission.INSTALL_PACKAGES
    return hasPermission == PackageManager.PERMISSION_GRANTED
  }

  private data class PermissionData(
    val permissions: String
  )

  fun getActualPermissions(permission: String): ArrayList<String> {
    val actualPermissions = arrayListOf<String>()
    if (permission.contains("{")) {
      val permissions = gson.fromJson(permission, PermissionData::class.java)
      permissions?.permissions?.split(",")?.forEach {
        actualPermissions.addAll(getActualPermissions(it))
      }
      return actualPermissions
    } else if (permission.contains(",")) {
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

  fun getActualPermissions(permissions: ArrayList<String>): ArrayList<String> {
    val actualPermissions = arrayListOf<String>()
    permissions.forEach { permission ->
      val temp = getActualPermissions(permission)
      if (temp.size > 0) {
        actualPermissions.addAll(temp)
      } else {
        actualPermissions.add(permission)
      }
    }
    return actualPermissions
  }

  fun testRequestAllPermissions(activity: Activity) {
    val permissions = arrayListOf<String>()
    permissions.add(Manifest.permission.READ_CALENDAR)
    permissions.add(Manifest.permission.WRITE_CALENDAR)
    permissions.add(Manifest.permission.CAMERA)
    permissions.add(Manifest.permission.READ_CONTACTS)
    permissions.add(Manifest.permission.WRITE_CONTACTS)
    permissions.add(Manifest.permission.GET_ACCOUNTS)
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    permissions.add(Manifest.permission.RECORD_AUDIO)
    permissions.add(Manifest.permission.READ_PHONE_STATE)
    permissions.add(Manifest.permission.CALL_PHONE)
    permissions.add(Manifest.permission.READ_CALL_LOG)
    permissions.add(Manifest.permission.WRITE_CALL_LOG)
    permissions.add(Manifest.permission.ADD_VOICEMAIL)
    permissions.add(Manifest.permission.USE_SIP)
    permissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS)
    permissions.add(Manifest.permission.BODY_SENSORS)
    permissions.add(Manifest.permission.SEND_SMS)
    permissions.add(Manifest.permission.RECEIVE_SMS)
    permissions.add(Manifest.permission.READ_SMS)
    permissions.add(Manifest.permission.RECEIVE_WAP_PUSH)
    permissions.add(Manifest.permission.RECEIVE_MMS)
    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    ActivityCompat.requestPermissions(
      activity, permissions.toTypedArray(), PermissionManager.MY_PERMISSIONS
    )
  }

  /**
   * 判断悬浮窗权限权限
   */
  private fun commonROMPermissionCheck(activity: Activity): Boolean {
    var result = true
    // version > 23
    try {
      val clazz: Class<*> = Settings::class.java
      val canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context::class.java)
      result = canDrawOverlays.invoke(null, activity) as Boolean
    } catch (e: Exception) {
      Log.e("commonROMPermissionCheck", Log.getStackTraceString(e))
    }
    return result
  }

  /**
   * 检查悬浮窗权限是否开启
   */
  fun checkSuspendedWindowPermission(activity: Activity, block: () -> Unit) {
    if (commonROMPermissionCheck(activity)) {
      block()
    } else {
      val build = AlertDialog.Builder(activity)
      build.setTitle("申请权限")
      build.setMessage("请开启悬浮窗权限")
      build.setCancelable(false)
      build.setPositiveButton("设置") { p0, p1 ->
        activity.startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
          data = Uri.parse("package:${activity.packageName}")
        }, 999)
      }
      build.show()
    }
  }
}
