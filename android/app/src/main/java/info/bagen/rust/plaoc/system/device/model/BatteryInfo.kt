package info.bagen.rust.plaoc.system.device.model

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.google.gson.Gson
import info.bagen.rust.plaoc.App

data class Battery(
    var batteryPercent: Int = 0,
    var isPhoneCharging: Boolean = false,
    var batteryHealth: String? = null,
    var batteryTechnology: String? = null,
    var batteryTemperature: Float = 0f,
    var batteryVoltage: Int = 0,
    var chargingSource: String? = null,
    var isBatteryPresent: Boolean = false
)

class BatteryInfo {
    private val BATTERY_HEALTH_COLD = "cold"
    private val BATTERY_HEALTH_DEAD = "dead"
    private val BATTERY_HEALTH_GOOD = "good"
    private val BATTERY_HEALTH_OVERHEAT = "Over Heat"
    private val BATTERY_HEALTH_OVER_VOLTAGE = "Over Voltage"
    private val BATTERY_HEALTH_UNKNOWN = "Unknown"
    private val BATTERY_HEALTH_UNSPECIFIED_FAILURE = "Unspecified failure"


    private val BATTERY_PLUGGED_AC = "Charging via AC"
    private val BATTERY_PLUGGED_USB = "Charging via USB"
    private val BATTERY_PLUGGED_WIRELESS = "Wireless"
    private val BATTERY_PLUGGED_UNKNOWN = "Unknown Source"

    fun getBatteryInfo(): String {
        var battery = Battery(
            batteryPercent,
            isPhoneCharging,
            batteryHealth,
            batteryTechnology,
            batteryTemperature,
            batteryVoltage,
            chargingSource,
            isBatteryPresent
        )
        return Gson().toJson(battery)
    }

    /* Battery Info:
        * battery percentage
        * is phone charging at the moment
        * Battery Health
        * Battery Technology
        * Battery Temperature
        * Battery Voltage
        * Charging Source
        * Check if battery is present */
    private val batteryStatusIntent: Intent?
        get() {
            val batFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            return App.appContext.registerReceiver(null, batFilter)
        }

    val batteryPercent: Int
        get() {
            val intent = batteryStatusIntent
            val rawlevel = intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            var level = -1
            if (rawlevel >= 0 && scale > 0) {
                level = rawlevel * 100 / scale
            }
            return level
        }

    val isPhoneCharging: Boolean
        get() {
            val intent = batteryStatusIntent
            val plugged = intent!!.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
        }

    val batteryHealth: String
        get() {
            var health = BATTERY_HEALTH_UNKNOWN
            val intent = batteryStatusIntent
            val status = intent!!.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
            when (status) {
                BatteryManager.BATTERY_HEALTH_COLD -> health = BATTERY_HEALTH_COLD

                BatteryManager.BATTERY_HEALTH_DEAD -> health = BATTERY_HEALTH_DEAD

                BatteryManager.BATTERY_HEALTH_GOOD -> health = BATTERY_HEALTH_GOOD

                BatteryManager.BATTERY_HEALTH_OVERHEAT -> health = BATTERY_HEALTH_OVERHEAT

                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> health = BATTERY_HEALTH_OVER_VOLTAGE

                BatteryManager.BATTERY_HEALTH_UNKNOWN -> health = BATTERY_HEALTH_UNKNOWN

                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> health =
                    BATTERY_HEALTH_UNSPECIFIED_FAILURE
            }
            return health
        }

    val batteryTechnology: String?
        get() {
            val intent = batteryStatusIntent
            return intent!!.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        }

    val batteryTemperature: Float
        get() {
            val intent = batteryStatusIntent
            val temperature = intent!!.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            return (temperature / 10.0).toFloat()
        }

    val batteryVoltage: Int
        get() {
            val intent = batteryStatusIntent
            return intent!!.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        }
    val chargingSource: String
        get() {
            val intent = batteryStatusIntent
            val plugged = intent!!.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> return BATTERY_PLUGGED_AC
                BatteryManager.BATTERY_PLUGGED_USB -> return BATTERY_PLUGGED_USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> return BATTERY_PLUGGED_WIRELESS
                else -> return BATTERY_PLUGGED_UNKNOWN
            }
        }

    val isBatteryPresent: Boolean
        get() {
            val intent = batteryStatusIntent
            return intent!!.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)
        }
}
