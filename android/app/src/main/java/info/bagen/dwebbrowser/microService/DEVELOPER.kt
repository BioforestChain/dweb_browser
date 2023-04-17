package info.bagen.dwebbrowser.microService

import android.os.Build

enum class DEVELOPER(val deviceName: String) {
    GAUBEE("Xiaomi/M2006J10C"),
    HuangLin("HUAWEI/ELE-AL00"),
    HLVirtual("Google/sdk_gphone64_x86_64"),
    WaterBang("samsung/SM-A9080"),
    ANONYMOUS("*");


    companion object {
        fun find(deviceName: String) = values().find { it.deviceName == deviceName } ?: ANONYMOUS
        val CURRENT = find(Build.MANUFACTURER + "/" + Build.MODEL)

        init {
            println("Hi Developer! ${CURRENT.deviceName} => ${Build.MANUFACTURER}/${Build.MODEL}")
        }
    }
}