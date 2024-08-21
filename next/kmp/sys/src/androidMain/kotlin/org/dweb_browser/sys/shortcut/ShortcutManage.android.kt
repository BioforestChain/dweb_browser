package org.dweb_browser.sys.shortcut

import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.platform.toAndroidBitmap
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.saveString
import org.dweb_browser.sys.R

actual class ShortcutManage {
  private val context = getAppContextUnsafe()
  private val MaxCount = 4

  actual suspend fun initShortcut(microModule: MicroModule.Runtime) {
    setDynamicShortcuts(emptyList())
  }

  actual suspend fun registryShortcut(shortcutList: List<SystemShortcut>) =
    setDynamicShortcuts(shortcutList)

  private fun setDynamicShortcuts(shortcutList: List<SystemShortcut>): Boolean {
    val list = mutableListOf<ShortcutInfoCompat>()
    list.addAll(getDefaultShortcuts())
    debugShortcut("setDynamicShortcuts", "size=${shortcutList.size}")
    shortcutList.forEach { shortcutItem ->
      val build = ShortcutInfoCompat.Builder(context, randomUUID())
      build.setShortLabel(shortcutItem.title)
      shortcutItem.icon?.toAndroidBitmap()?.let { bitmap ->
        build.setIcon(IconCompat.createWithBitmap(bitmap))
      } ?: run {
        build.setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
      }
      build.setIntent(Intent().apply {
        action = Intent.ACTION_VIEW
        `package` = context.packageName
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.parse(buildUrlString("dweb://shortcutopen") {
          parameters["mmid"] = shortcutItem.mmid
          parameters["data"] = shortcutItem.data
        })
      })
      list.add(build.build())
    }
    context.saveString("shortcuts", Json.encodeToString(shortcutList))
    if (list.isEmpty()) return false
    if (list.size <= MaxCount) {
      ShortcutManagerCompat.setDynamicShortcuts(context, list)
    } else {
      // 如果数量大于 MaxCount 说明最终显示会不全，只能保留前面几个，最后一个使用 More 替换
      val tempList = list.subList(0, 3)
      tempList.add(getMoreShortcut())
      ShortcutManagerCompat.setDynamicShortcuts(context, tempList)
    }
    return true
  }

  private fun getMaxShortcutCount(): Int {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    return shortcutManager.maxShortcutCountPerActivity.also {
      debugShortcut("MaxCount", it)
    }
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
        action = Intent.ACTION_VIEW // "info.bagen.dwebbrowser.scan"
        `package` = context.packageName
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.parse(buildUrlString("dweb://shortcutopen") {
          parameters["mmid"] = "scan.browser.dweb"
          parameters["data"] = "null"
        })
      })
      .build()
    return mutableListOf(qrcodeShortcut)
  }

  private fun getMoreShortcut(): ShortcutInfoCompat {
    return ShortcutInfoCompat.Builder(context, "more")
      .setShortLabel(ShortcutI18nResource.more_title.text)
      .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shutcut_more))
//      .setIntent(Intent(context, ShortcutManageActivity::class.java).apply {
//        action = "${context.packageName}.shortcut.more"
//        `package` = context.packageName
//        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//      })
      .setIntent(Intent().apply {
        action = Intent.ACTION_VIEW
        `package` = context.packageName
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.parse(buildUrlString("dweb://shortcutopen") {
          parameters["mmid"] = "shortcut.sys.dweb"
          parameters["data"] = "null"
        })
      })
      .build()
  }

  actual suspend fun getValidIcon(
    microModule: MicroModule.Runtime,
    resource: ImageResource,
  ): ByteArray? {
    return microModule.nativeFetch(resource.src).body.toPureBinary()
  }
}