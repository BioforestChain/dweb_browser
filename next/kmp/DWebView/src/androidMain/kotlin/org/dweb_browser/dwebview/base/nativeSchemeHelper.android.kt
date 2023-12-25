package org.dweb_browser.dwebview.base

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build

fun isSchemeAppInstalled(mContext: Context, uri: Uri): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val list: List<ResolveInfo> = mContext.packageManager.queryIntentActivities(intent, 0)
    val possibleBrowserIntents: List<ResolveInfo> = mContext.packageManager.queryIntentActivities(
      Intent(
        Intent.ACTION_VIEW,
        Uri.parse("http://example.com/")
      ), 0
    )
    val excludeIntents: MutableSet<String> = HashSet()
    for (eachPossibleBrowserIntent: ResolveInfo in possibleBrowserIntents) {
      excludeIntents.add(eachPossibleBrowserIntent.activityInfo.name)
    }
    //Check for non browser application
    for (resolveInfo: ResolveInfo in list) {
      if (!excludeIntents.contains(resolveInfo.activityInfo.name)) {
        intent.setPackage(resolveInfo.activityInfo.packageName)
        return true
      }
    }
  } else {
    try {
      // In order for this intent to be invoked, the system must directly launch a non-browser app.
      // Ref: https://developer.android.com/training/package-visibility/use-cases#avoid-a-disambiguation-dialog
      val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER or Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT
      )
      if (intent.resolveActivity(mContext.packageManager) != null) {
        return true
      }
    } catch (e: ActivityNotFoundException) {
      // This code executes in one of the following cases:
      // 1. Only browser apps can handle the intent.
      // 2. The user has set a browser app as the default app.
      // 3. The user hasn't set any app as the default for handling this URL.
      return false
    }
  }
  return false
}