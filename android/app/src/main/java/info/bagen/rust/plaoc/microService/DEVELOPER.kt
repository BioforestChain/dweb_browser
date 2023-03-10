package info.bagen.rust.plaoc.microService

import android.os.Build

enum class DEVELOPER(val deviceName: String) {
    GAUBEE("Xiaomi/M2006J10C"), ANONYMOUS("*");

    companion object {
        fun find(deviceName: String) = values().find { it.deviceName == deviceName } ?: ANONYMOUS
        val CURRENT = find(Build.MANUFACTURER + "/" + Build.MODEL)

        init {
            println("Hi Developer! ${CURRENT.deviceName}")
        }
    }
}