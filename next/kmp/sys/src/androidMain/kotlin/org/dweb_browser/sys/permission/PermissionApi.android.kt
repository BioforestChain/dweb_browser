package org.dweb_browser.sys.permission

import android.content.Intent
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.permission.PermissionType
import org.dweb_browser.helper.PromiseOut
import java.io.Serializable

actual suspend fun requestPermission(type: PermissionType): Boolean {
  return requestPermissions(mutableListOf(type))[type] ?: false
}

actual suspend fun requestPermissions(types: MutableList<PermissionType>): MutableMap<PermissionType, Boolean> {
  return openPermissionActivity(types)
}

private suspend fun openPermissionActivity(types: MutableList<PermissionType>): MutableMap<PermissionType, Boolean> {
  PermissionActivity.activityPromiseOut = PromiseOut()
  val context = NativeMicroModule.getAppContext()
  val intent = Intent(context, PermissionActivity::class.java)
  intent.`package` = context.packageName
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  intent.putExtra(PermissionActivity.EXTRA_PERMISSION, types as Serializable)
  context.startActivity(intent)
  return PermissionActivity.activityPromiseOut.waitPromise()
}