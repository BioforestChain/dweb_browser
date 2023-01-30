package info.bagen.rust.plaoc.system.dcim

import android.app.Activity
import android.content.Intent
import info.bagen.libappmgr.database.MediaDBManager
import info.bagen.libappmgr.system.media.MediaInfo
import info.bagen.libappmgr.system.media.MediaType
import info.bagen.libappmgr.ui.dcim.DCIMActivity
import info.bagen.libappmgr.utils.FilesUtil
import info.bagen.rust.plaoc.App

class DCIMManager {
    companion object {
        const val REQUEST_DCIM_CODE = DCIMActivity.REQUEST_DCIM_CODE
        const val RESPONSE_DCIM_VALUE = DCIMActivity.RESPONSE_DCIM_VALUE
    }

    /**
     * 启动DCIMActivity界面，并且接受返回信息
     * @param activity 启动相册的activity，使用onActivityResult可以收到返回内容
     * @return 在activity中实现 #onActivityResult# 接收返回的MediaInfo列表
     */
    fun startDCIMActivityForResult(activity: Activity) {
        val intent = Intent(App.appContext, DCIMActivity::class.java)
        activity.startActivityForResult(intent, REQUEST_DCIM_CODE)
    }

    /**
     * 根据page和count获取列表数据
     * @param page 分页中的页数
     * @param count 分页中的个数
     */
    fun getDCIMInfoByPage(
        page: Int = 0,
        count: Int = 50,
        type: MediaType = MediaType.All,
        originSupport: Boolean = false
    ): ArrayList<MediaInfo> {
        return MediaDBManager.queryMediaData(
            type = type, from = page * count, to = (page + 1) * count, isOrigin = originSupport
        )
    }

    /**
     * 根据id获取原图或者原视频信息
     */
    fun getDCIMOriginInfo(id: Int): String? {
        val mediaInfo = MediaDBManager.queryMediaData(id)
        return mediaInfo?.path?.let {
            FilesUtil.fileToBase64(it)
        } ?: null
    }
}
