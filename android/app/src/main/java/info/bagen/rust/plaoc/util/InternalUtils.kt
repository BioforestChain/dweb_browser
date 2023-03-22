package info.bagen.rust.plaoc.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

 class InternalUtils {
   companion object{
       @Throws(PackageManager.NameNotFoundException::class)
       fun getPackageInfo(pm: PackageManager?, packageName: String?): PackageInfo? {
           return getPackageInfo(pm!!, packageName!!, 0)
       }

       @Throws(PackageManager.NameNotFoundException::class)
       fun getPackageInfo(pm: PackageManager, packageName: String?, flags: Long): PackageInfo? {
           return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
               pm.getPackageInfo(packageName!!, PackageManager.PackageInfoFlags.of(flags))
           } else {
               getPackageInfoLegacy(pm, packageName!!, flags.toInt().toLong())
           }
       }

       @Throws(PackageManager.NameNotFoundException::class)
       private fun getPackageInfoLegacy(
           pm: PackageManager,
           packageName: String,
           flags: Long
       ): PackageInfo? {
           return pm.getPackageInfo(packageName, flags.toInt())
       }
   }
}