package info.bagen.libappmgr.ui.test

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import info.bagen.libappmgr.R

class TestViewModel : ViewModel() {
    val showWebView = mutableStateOf(false)
    val showFavPop = mutableStateOf(false)
    val showFavOrHistory = mutableStateOf(0)
    val showListType = mutableStateOf("favorite")
    val url = mutableStateOf("")
    var progress = mutableStateOf(-1)

    var bottomNavController = rememberNavController()
    var favoritePopController = rememberPopController()

    val hotWebList: ArrayList<TestAppInfo> = rememberHotWebList()
    val hotSearchList: MutableList<TestAppInfo> = mutableStateListOf()
    val favoriteList: MutableList<TestAppInfo> = mutableStateListOf()
    val historyList: MutableList<TestAppInfo> = mutableStateListOf()

    val navUIType = mutableStateOf(NavUIType.HOME)

    val deleteList = arrayListOf<TestAppInfo>()
    var curAppInfo: TestAppInfo? = null
}

enum class NavUIType {
    HOME, LIST, WEBVIEW
}

data class NavController(
    val iconRes: Int,
    val type: NavControllerType,
    val clickable: MutableState<Boolean> = mutableStateOf(false),
    val name: String? = null
)

fun rememberPopController(): ArrayList<NavController> {
    var tags = arrayListOf(
        NavControllerType.Book, NavControllerType.History, NavControllerType.Share
    )
    var icons = arrayListOf(
        R.drawable.icon_book, R.drawable.icon_history, R.drawable.icon_share,
    )
    var names = arrayListOf(
        "书签", "历史", "分享"
    )
    var list = arrayListOf<NavController>()
    icons.forEach {
        var index = icons.indexOf(it)
        var type = tags[index]
        list.add(NavController(it, type = type, mutableStateOf(true), names[index]))
    }
    return list
}

fun rememberNavController(): ArrayList<NavController> {
    var tags = arrayListOf(
        NavControllerType.Back, NavControllerType.Next, NavControllerType.Book,
        NavControllerType.Favorite, NavControllerType.Home
    )
    var icons = arrayListOf(
        R.drawable.icon_left, R.drawable.icon_right, R.drawable.icon_book,
        R.drawable.icon_list, R.drawable.icon_home
    )
    var list = arrayListOf<NavController>()
    icons.forEach {
        var type = tags[icons.indexOf(it)]
        list.add(NavController(it, type = type, mutableStateOf(type == NavControllerType.Favorite)))
    }
    return list
}

enum class NavControllerType() {
    Back, Next, Book, Favorite, Home, History, Share
}

fun rememberHotWebList(): ArrayList<TestAppInfo> {
    var list = arrayListOf<TestAppInfo>()
    list.add(TestAppInfo("斗鱼", "http://linge.plaoc.com/douyu.png", "https://www.douyu.com/"))
    list.add(TestAppInfo("网易", "http://linge.plaoc.com/163.png", "https://www.163.com/"))
    list.add(TestAppInfo("微博", "http://linge.plaoc.com/weibo.png", "https://weibo.com/"))
    list.add(TestAppInfo("豆瓣", "http://linge.plaoc.com/douban.png", "https://movie.douban.com/"))
    list.add(TestAppInfo("知乎", "http://linge.plaoc.com/zhihu.png", "https://www.zhihu.com/"))
    list.add(
        TestAppInfo(
            "哔哩哔哩",
            "http://linge.plaoc.com/bilibili.png",
            "https://www.bilibili.com/"
        )
    )
    list.add(TestAppInfo("腾讯新闻", "http://linge.plaoc.com/tencent.png", "https://www.qq.com/"))
    list.add(TestAppInfo("京东", "http://linge.plaoc.com/jingdong.png", "https://www.jd.com/"))
    return list
}
