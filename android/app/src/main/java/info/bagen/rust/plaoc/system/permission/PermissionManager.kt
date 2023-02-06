package info.bagen.rust.plaoc.system.permission

import android.app.Activity
import androidx.fragment.app.Fragment
import info.bagen.libappmgr.system.permission.PermissionManager
import info.bagen.libappmgr.system.permission.PermissionUtil

/**
 * 用于请求网络
 */
object PermissionManager {
    fun requestPermissions(activity: Activity, list: ArrayList<String>) {
        PermissionManager(activity)
            .requestPermissions(PermissionUtil.getActualPermissions(list))
    }

    fun requestPermissions(activity: Activity, permission: String) {
        PermissionManager(activity)
            .requestPermissions(PermissionUtil.getActualPermissions(permission))
    }

    fun requestPermissions(fragment: Fragment, list: ArrayList<String>) {
        PermissionManager(fragment)
            .requestPermissions(PermissionUtil.getActualPermissions(list))
    }

    fun requestPermissions(fragment: Fragment, permission: String) {
        PermissionManager(fragment)
            .requestPermissions(PermissionUtil.getActualPermissions(permission))
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        activity: Activity,
        onPermissionCallback: PermissionManager.PermissionCallback? = null
    ) {
        PermissionManager(activity).onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onPermissionCallback
        )
    }
}
