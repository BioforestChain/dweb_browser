package info.bagen.rust.plaoc.webView.network

import android.graphics.Color


/**将ARGB color int 转化为RGBA 十六进制*/
fun getColorHex(color: Int): String {
    return ("#" + Integer.toHexString(color))
}

/** hex to Color Int*/
fun hexToIntColor(hexColor: String): Int {
    val hex = hexColor.replace("#", "")
    val len = hex.length
//  Log.i("hexToIntColor:","len=$len,hexColor=$hexColor");
    // #RRGGBBAA
    if (len == 8) {
        val rgb = hex.substring(0, len - 2)
        val alpha = hex.substring(len - 2, len)
//    Log.i("hexToIntColor:","len=$len,rgb=$rgb,alpha=$alpha")
        return Color.parseColor("#$alpha$rgb") // transform RGBA to ARGB
    }
    // #RRGGBB
    if (len == 6) {
        return Color.parseColor(hexColor)
    }
    val rgb = hex.replace("(.)".toRegex(), "$1$1")
//  Log.i("hexToIntColor:","rgbxx=$rgb")
    return Color.parseColor("#$rgb")
}


