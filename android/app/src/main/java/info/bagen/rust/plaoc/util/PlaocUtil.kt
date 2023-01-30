package info.bagen.rust.plaoc.util

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.system.barcode.QRCodeScanningActivity

object PlaocUtil {

    /**
     * 增加桌面长按图标时显示的快捷列表
     */
    fun addShortcut(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutInfoList = arrayListOf<ShortcutInfoCompat>()
            val builder = ShortcutInfoCompat.Builder(context, "plaoc")
            builder.setShortLabel("扫一扫")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_scan))
                .setIntent(
                    Intent(
                        "info.bagen.rust.plaoc.qrcodescan",
                        null,
                        context,
                        QRCodeScanningActivity::class.java
                    )
                )
            shortcutInfoList.add(builder.build())
            ShortcutManagerCompat.addDynamicShortcuts(context, shortcutInfoList)
        }
    }

    /**生成零拷贝key*/
    fun saveZeroBuffKey(req_id: ByteArray): String {
        return "${req_id[0]}-${req_id[1]}"
    }

    /** 获取零拷贝key(因为是前后生成的，所以需要减1)*/
    fun getZeroBuffKey(req_id: ByteArray): String {
        return "${req_id[0]}-${req_id[1]}"
    }
}


object PlaocToString {
    /**二进制解析成字符串*/
    fun transByteArray(bytes: ByteArray): String {
        return String(bytes)
    }

    /** hex String 解析成字符串*/
    fun transHexString(stringHex: String): String {
        return String(hexStrToByteArray(stringHex))
    }
}

/**
 * 十六进制String转Byte数组
 *
 * @param str
 * @return
 */
fun hexStrToByteArray(str: String): ByteArray {
    if (str.isEmpty()) {
        return ByteArray(0)
    }
    val currentStr = str.split(",")
    val byteArray = ByteArray(currentStr.size)
    for (i in byteArray.indices) {
        byteArray[i] = currentStr[i].toInt().toByte()
    }
    return byteArray
}
