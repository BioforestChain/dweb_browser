package info.bagen.dwebbrowser.microService.sys.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import info.bagen.dwebbrowser.util.InternalUtils
import info.bagen.dwebbrowser.util.permission.PermissionManager
import info.bagen.dwebbrowser.util.permission.PermissionUtil

/**
 * 用于请求网络
 */
object PermissionManager {

    /**
     * 检查给定权限列表是否全部由用户授予
     *
     * @since 3.0.0
     * @param permissions Permissions to check.
     * @return True if all permissions are granted, false if at least one is not.
     */
    fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context == null) return false
        for (perm in permissions) {
            val ctx = ActivityCompat.checkSelfPermission(
                context,
                perm
            )
            if (ctx != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Check whether the given permission has been defined in the AndroidManifest.xml
     *
     * @since 3.0.0
     * @param permission A permission to check.
     * @return True if the permission has been defined in the Manifest, false if not.
     */
    fun hasDefinedPermission(context: Context, permission: String): Boolean {
        var hasPermission = false
        val requestedPermissions: Array<String>? = getManifestPermissions(context)
        if (requestedPermissions != null && requestedPermissions.isNotEmpty()) {
            val requestedPermissionsList: Array<String> = requestedPermissions
            if (requestedPermissionsList.contains(permission)) {
                hasPermission = true
            }
        }
        return hasPermission
    }

    /**
     * Get the permissions defined in AndroidManifest.xml
     *
     * @since 3.0.0
     * @return The permissions defined in AndroidManifest.xml
     */
    fun getManifestPermissions(context: Context): Array<String>? {
        var requestedPermissions: Array<String> ? = null
        try {
            val pm = context.packageManager
            val packageInfo: PackageInfo? = InternalUtils.getPackageInfo(
                pm,
                context.packageName,
                PackageManager.GET_PERMISSIONS.toLong()
            )
            requestedPermissions = packageInfo?.requestedPermissions
        } catch (ex: Exception) {
        }
        return requestedPermissions
    }

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
