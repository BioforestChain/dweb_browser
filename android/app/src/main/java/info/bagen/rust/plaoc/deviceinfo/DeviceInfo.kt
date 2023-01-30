package info.bagen.rust.plaoc.deviceinfo


/**
 * 获取 Android 设备的系统信息。包含系统、屏幕、内存、IMEI、存储、传感器、MAC 地址、CPU 等信息。
 */
class DeviceInfo {
    private val LINE_SEP = System.getProperty("line.separator")
    private val SYS_EMUI = "sys_emui"
    private val SYS_MIUI = "sys_miui"
    private val SYS_FLYME = "sys_flyme"
    private val KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code"
    private val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"
    private val KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage"
    private val KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level"
    private val KEY_EMUI_VERSION = "ro.build.version.emui"
    private val KEY_EMUI_CONFIG_HW_SYS_VERSION = "ro.confg.hw_systemversion"


}
