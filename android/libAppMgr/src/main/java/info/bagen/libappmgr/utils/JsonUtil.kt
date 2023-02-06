package info.bagen.libappmgr.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import info.bagen.libappmgr.entity.AppInfo
import info.bagen.libappmgr.entity.DAppInfo

object JsonUtil {
    const val TAG: String = "JsonUtil"

    /**
     * 通过 link.json 文件获取的字符串，解析成AppInfo
     */
    fun getAppInfoFromLinkJson(content: String, type: APP_DIR_TYPE): AppInfo? {
        try {
            val appInfo: AppInfo = Gson().fromJson(content, AppInfo::class.java)
            appInfo.appDirType = type
            appInfo.iconPath = FilesUtil.getAppIconPathName(appInfo)
            return appInfo
        } catch (e: Exception) {
            Log.d(TAG, "getAppInfoFromLinkJson e->$e")
        }
        return null
    }

    /**
     * 获取数组列表
     */
    fun getAppInfoListFromLinkJson(content: String, type: APP_DIR_TYPE): List<AppInfo>? {
        try {
            var appInfos: List<AppInfo> =
                Gson().fromJson(content, object : TypeToken<List<AppInfo>>() {}.type)
            appInfos.forEach {
                it.isRecommendApp = when (type) {
                    APP_DIR_TYPE.RecommendApp -> true
                    else -> false
                }
            }
            return appInfos
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
        return null
    }

    fun getDAppInfoFromBFSA(content: String?): DAppInfo? { // bfsa-metadata.json
        try {
            content?.let { return Gson().fromJson(it, DAppInfo::class.java) }
        } catch (e: Exception) {
            Log.d(TAG, "getDAppInfoFromBFSA e->$e")
        }
        return null
    }

    fun <T> toJson(value: T?): String {
        return Gson().toJson(value)
    }

    fun <T> fromJson(type: Class<T>, value: String): T? {
        return Gson().fromJson(value, type)
    }

    fun <T> jsonToList(value: String, type: Class<T>): List<T>? {
        return Gson().fromJson(value, object : TypeToken<List<T>>() {}.type)
    }
}
