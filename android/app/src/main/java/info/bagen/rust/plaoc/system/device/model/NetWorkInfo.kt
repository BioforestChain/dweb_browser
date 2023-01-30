package info.bagen.rust.plaoc.system.device.model

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.regex.Pattern

data class NetWorkData(
    var enableInternet: Boolean = false,
    var enableWifi: Boolean = false,
    var enableBluetooth: Boolean = false,
    var macAddressInternet: String = "",
    var macAddressWifi: String = "",
    var macAddressBluetooth: String = "",
    var ipAddressInternet: String = "",
    var ipAddressWifi: String = ""
)

class NetWorkInfo {

    fun getNetWorkInfo(): String {
        return JsonUtil.toJson(netWorkData)
    }

    val netWorkData: NetWorkData
        get() {
            var data = NetWorkData()
            data.enableBluetooth = switchOfBluetooth
            data.enableInternet = switchOfInternet
            data.enableWifi = switchOfWifi

            if (data.enableWifi) {
                data.macAddressWifi = macAddrOfWifi
                data.ipAddressWifi = ipAddrOfWifi
            }

            // 本地网络需要增加判断wifi是否有正常连接，如果有，本地网络的地址无法获取，就不取了
            if (data.enableInternet && data.ipAddressWifi.isEmpty()) {
                data.macAddressInternet = macAddrOfInternet
                data.ipAddressInternet = ipAddrOfInternet
            }

            if (data.enableBluetooth) {
                data.macAddressBluetooth = macAddrOfBluetooth
            }

            return data
        }

    val switchOfInternet: Boolean
        get() {
            val cm =
                App.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo
            return ni?.isAvailable ?: false
        }

    val switchOfWifi: Boolean
        get() {
            val wifi = App.appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifi.isWifiEnabled
        }

    val switchOfBluetooth: Boolean
        get() {
            val bluetooth = BluetoothAdapter.getDefaultAdapter()
            return bluetooth.isEnabled
        }

    val macAddrOfInternet: String
        get() {
            return getMacFromHardware(NetType.INTERNET)
        }

    val macAddrOfWifi: String
        get() {
            val wifi = App.appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return if (wifi.isWifiEnabled) {
                getWifiMacAddress()
            } else {
                "WIFI需要打开状态才能获取MAC信息"
            }
        }

    val macAddrOfBluetooth: String
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.Secure.getString(App.appContext.contentResolver, "bluetooth_address")
                    ?: "01"
            } else {
                val bta = BluetoothAdapter.getDefaultAdapter()
                bta?.address ?: "02"
            }
        }

    val ipAddrOfInternet: String
        get() {
            return getIPFromHardware(NetType.INTERNET)
        }
    val ipAddrOfWifi: String
        get() {
            return getIPFromHardware(NetType.WIFI)
        }

    // 获取本地
    private fun getEthMacAddress(): String {
        var mac = "02:00:00:00:00:00"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // android 6.0 以下 基础Mac获取方法
            val wm = App.appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            mac = wm.connectionInfo.macAddress
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N
        ) {
            // Android 6.0（包括） - Android 7.0（不包括） 通过读取系统文件获取Mac
            try {
                mac = BufferedReader(FileReader(File("/sys/class/net/eth0/address"))).readLine()
                    .substring(0, 17)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mac = getMacFromHardware(NetType.INTERNET)
        }
        return mac
    }

    // 获取 wifi的mac地址，
    private fun getWifiMacAddress(): String {
        var mac = "02:00:00:00:00:00"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // android 6.0 以下 基础Mac获取方法
            val wm = App.appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            mac = wm.connectionInfo.macAddress
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N
        ) {
            // Android 6.0（包括） - Android 7.0（不包括） 通过读取系统文件获取Mac
            try {
                mac = BufferedReader(FileReader(File("/sys/class/net/wlan0/address"))).readLine()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mac = getMacFromHardware(NetType.WIFI)
        }
        return mac
    }

    private enum class NetType(val typeName: String) {
        WIFI(typeName = "wlan0"),
        INTERNET(typeName = "eth0|rmnet0") // huawei的本地网络是rmnet0
    }

    /**
     * Android 7.0之后获取Mac地址 需要通过遍历网络接口获取Ip 否则将永远返回02:00:00:00:00
     * 遍历循环所有的网络接口，找到接口是 wlan0
     * 必须的权限 <uses-permission android:name="android.permission.INTERNET" />
     *
     * @return
     */
    private fun getMacFromHardware(netType: NetType): String {
        try {
            val niList: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (ni in niList) {
                if (!Pattern.matches(netType.typeName, ni.name)) continue
                val macBytes = ni.hardwareAddress ?: return "02:00:00:00:00:00"
                return macBytes.toMacAddress()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return "02:00:00:00:00:00"
    }

    private fun getIPFromHardware(netType: NetType): String {
        try {
            val niList: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (ni in niList) {
                if (!Pattern.matches(netType.typeName, ni.name)) continue
                val iaList: List<InetAddress> = Collections.list(ni.inetAddresses)
                for (ia in iaList) {
                    if (ia.hostAddress.isIPv4()) {
                        return ia.hostAddress
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return "0.0.0.0"
    }

    private fun getIPAndMacFromHardware(
        netType: NetType,
        callback: (ip: String, mac: String) -> Unit
    ) {
        try {
            val niList: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (ni in niList) {
                if (!Pattern.matches(netType.typeName, ni.name)) continue
                val iaList: List<InetAddress> = Collections.list(ni.inetAddresses)
                for (ia in iaList) {
                    if (ia.hostAddress.isIPv4()) {
                        callback(
                            ia.hostAddress,
                            ni.hardwareAddress?.toMacAddress() ?: "02:00:00:00:00:00"
                        )
                        return
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        callback("127.0.0.1", "02:00:00:00:00:00")
    }
}

fun String.isIPv4(): Boolean {
    // 1.判断长度，7~15
    if (length < 7 || length > 15) {
        //LogUtils.d("isIPv4 -> length in 7~15")
        return false
    }
    // 2.判断首尾是否有'.'字符
    val c = toCharArray()
    if (c[0] == '.' || c[c.size - 1] == '.') {
        //LogUtils.d("isIPv4 -> start/end not '.'")
        return false
    }
    for (cc in c) { // 如果非数字和'.'组合即为非法
        if (!cc.isDigit() && cc != '.') {
            //LogUtils.d("isIPv4 -> invalid char")
            return false
        }
    }
    // 3.按照'.'分隔，数组为4
    val s = split(".")
    if (s.size != 4) {
        //LogUtils.d("isIPv4 -> split array size=${s.size}")
        return false
    }
    // 4.判断数组第一位在1~255之间，后面三个在0~255
    for (i in 0..3) {
        val value = s[i].toInt()
        if (i == 0 && (value <= 0 || value > 255)) {
            //LogUtils.d("isIPv4 -> first value in 1~255 :$value")
            return false
        }
        if (value < 0 || value > 255) {
            //LogUtils.d("isIPv4 -> value in 0~255 :$value")
            return false
        }
    }
    //LogUtils.d("isIPv4 -> true : $this")
    return true
}

fun String.isIPv6(): Boolean {
    // 1.判断长度 >7
    if (length < 7) return false
    // 2.判断首尾是否有':'字符
    val c = toCharArray()
    if (c[0] == ':' || c[c.size - 1] == ':') return false
    for (cc in c) { // 如果非数字、字母和':'组合即为非法
        if (!cc.isDigit() || !cc.isLetter() || cc != ':') return false
    }
    // 3.按照':'分隔，数组为8
    val s = split(":")
    if (s.size != 8) return false
    // 4.判断数组中每一位都不能大于8位
    for (value in s) {
        if (value.length > 4) return false
    }
    return true
}

fun ByteArray.toMacAddress(): String {
    val res1 = StringBuilder()
    for (b in this) {
        res1.append(String.format("%02X:", b))
    }
    if (res1.isNotEmpty()) {
        res1.deleteCharAt(res1.length - 1)
    }
    return res1.toString()
}
