package org.dweb_browser.sys.shortcut

import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.platform.toAndroidBitmap
import org.dweb_browser.sys.R

actual class ShortcutManage {
  private val context = getAppContext()

  actual suspend fun initShortcut() {
    setDynamicShortcuts(emptyList())
  }

  actual suspend fun registryShortcut(shortcutList: List<SystemShortcut>) =
    setDynamicShortcuts(shortcutList)

  private fun setDynamicShortcuts(shortcutList: List<SystemShortcut>): Boolean {
    val list = mutableListOf<ShortcutInfoCompat>()
    list.addAll(getDefaultShortcuts())
    shortcutList.forEach { shortcutItem ->
      val build = ShortcutInfoCompat.Builder(context, shortcutItem.uri)
      build.setShortLabel(shortcutItem.title)
      shortcutItem.icon?.let { icon ->
        build.setIcon(IconCompat.createWithBitmap(icon.toAndroidBitmap()))
      }
      build.setIntent(Intent().apply {
        action = Intent.ACTION_VIEW
        `package` = context.packageName
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.parse(shortcutItem.uri)
      })
      list.add(build.build())
    }
    if (list.isNotEmpty()) {
      ShortcutManagerCompat.setDynamicShortcuts(context, list)
    }
    return list.isNotEmpty()
  }

  private fun removeAllShortcuts() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      val shortcutManager = context.getSystemService(ShortcutManager::class.java)
      shortcutManager?.removeAllDynamicShortcuts()
    }
  }

  private fun getDefaultShortcuts(): MutableList<ShortcutInfoCompat> {
    // 1. 扫一扫
    val qrcodeShortcut = ShortcutInfoCompat.Builder(context, "dweb_qrcode")
      .setShortLabel(ShortcutI18nResource.default_qrcode_title.text)
      .setIcon(IconCompat.createWithResource(context, R.drawable.ic_main_qrcode_scan))
      .setIntent(Intent().apply {
        action = "info.bagen.dwebbrowser.scan"
        `package` = context.packageName
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      })
      .build()
    return mutableListOf(qrcodeShortcut)
  }
}