package info.bagen.libappmgr.ui.app

import info.bagen.libappmgr.entity.AppInfo
import info.bagen.libappmgr.entity.DAppInfoUI
import info.bagen.libappmgr.network.ApiService
import info.bagen.libappmgr.network.base.fold
import info.bagen.libappmgr.utils.APP_DIR_TYPE
import info.bagen.libappmgr.utils.FilesUtil
import kotlinx.coroutines.flow.flow

class AppRepository(private val api: ApiService = ApiService.instance) {

    suspend fun loadAppInfoList() = FilesUtil.getAppInfoList() // loadAppInfoListTest()

    suspend fun loadDAppUrl(appInfo: AppInfo): DAppInfoUI? {
        //return loadDAppUrlTest(appInfo)
        val dAppInfo = FilesUtil.getDAppInfo(appInfo.bfsAppId)
        dAppInfo?.let { info ->
            val dAppUrl = FilesUtil.getAppDenoUrl(appInfo, info)
            val url = info.manifest.url ?: ""
            val isDWeb = info.manifest.appType != "web"
            val name = dAppInfo.manifest.name
            val iconPath = FilesUtil.getAppIconPathName(appInfo)
            val version = dAppInfo.manifest.version
            return DAppInfoUI(dAppUrl, name, iconPath, url, version, isDWeb)
        }
        return null
    }

    suspend fun loadAppNewVersion(path: String) = flow {
        val a = api.getAppVersion(path)
        a.fold(
            onSuccess = { emit(it.data) },
            onFailure = {},
            onLoading = {},
            onPrepare = {}
        )
    }

    private fun loadAppInfoListTest(): List<AppInfo> {
        val list = arrayListOf<AppInfo>()
        list.add(
            createTemp(
                "douyu",
                "斗鱼",
                "http://linge.plaoc.com/douyu.png",
                "https://www.douyu.com/",
                "https://m.douyu.com/"
            )
        )
        list.add(
            createTemp(
                "wangyi",
                "网易",
                "http://linge.plaoc.com/163.png",
                "https://www.163.com/",
                "https://3g.163.com/"
            )
        )
        list.add(
            createTemp(
                "weibo",
                "微博",
                "http://linge.plaoc.com/weibo.png",
                "https://weibo.com/",
                "https://m.weibo.cn/"
            )
        )
        list.add(
            createTemp(
                "douban",
                "豆瓣",
                "http://linge.plaoc.com/douban.png",
                "https://movie.douban.com/",
                "https://m.douban.com/movie/"
            )
        )
        list.add(
            createTemp(
                "zhihu",
                "知乎",
                "http://linge.plaoc.com/zhihu.png",
                "https://www.zhihu.com/",
                "https://www.zhihu.com/"
            )
        )
        list.add(
            createTemp(
                "bilibili",
                "哔哩哔哩",
                "http://linge.plaoc.com/bilibili.png",
                "https://www.bilibili.com/",
                "https://m.bilibili.com/"
            )
        )
        list.add(
            createTemp(
                "tencent",
                "腾讯新闻",
                "http://linge.plaoc.com/tencent.png",
                "https://www.qq.com/",
                "https://xw.qq.com/?f=qqcom"
            )
        )
        //list.add(createTemp("jingdong","京东", "http://linge.plaoc.com/jingdong.png", "https://www.jd.com/", "https://m.jd.com/"))
        return list
    }

    private fun loadDAppUrlTest(appInfo: AppInfo): DAppInfoUI {
        return DAppInfoUI(
            appInfo.dAppUrl!!,
            appInfo.name,
            appInfo.iconPath,
            appInfo.dAppUrl!!,
            "1.1.1",
            false
        )
    }

    private fun createTemp(
        id: String,
        name: String,
        icon: String,
        url: String,
        appUrl: String
    ): AppInfo {
        return AppInfo(
            version = "1.0.0",
            bfsAppId = id,
            name = name,
            icon = icon,
            isRecommendApp = true,
            appDirType = APP_DIR_TYPE.SystemApp,
            iconPath = icon,
            dAppUrl = appUrl
        )
    }
}
