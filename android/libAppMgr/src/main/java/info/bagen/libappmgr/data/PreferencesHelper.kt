package info.bagen.libappmgr.data

import info.bagen.libappmgr.utils.AppContextUtil
import info.bagen.libappmgr.utils.getBoolean
import info.bagen.libappmgr.utils.saveBoolean

object PreferencesHelper {
    private const val STATE_FIRST_LOADING = "plaoc.state.first.in" // 判断是否第一次运行程序
    private const val STATE_MEDIA_LOADING = "plaoc.state.media.loading" // 判断media数据是否已经加载过了
    /*private const val STATE_KEY_FIRST_INT = "wan.state.first.in"
    private const val USER_KEY_ID = "wan.user.id"
    private const val USER_KEY_NAME = "wan.user.name"
    private const val USER_KEY_COOKIE = "wan.user.cookie"
    private const val CACHE_KEY_BANNER = "wan.cache.banner"*/

    fun saveFirstState(isFirst: Boolean) =
        AppContextUtil.sInstance!!.saveBoolean(STATE_FIRST_LOADING, isFirst)

    fun isFirstIn() = AppContextUtil.sInstance!!.getBoolean(STATE_FIRST_LOADING, true)

    fun saveMediaLoading(loading: Boolean) =
        AppContextUtil.sInstance!!.saveBoolean(STATE_MEDIA_LOADING, loading)

    fun isMediaLoading() = AppContextUtil.sInstance!!.getBoolean(STATE_MEDIA_LOADING, false)

    /*fun saveUserId(context: Context, id: Int) = context.saveInteger(USER_KEY_ID, id)

    fun hasLogin(context: Context) = context.getInteger(USER_KEY_ID) > 0

    fun saveUserName(context: Context, name: String) = context.saveString(USER_KEY_NAME, name)

    fun fetchUserName(context: Context) = context.getString(USER_KEY_NAME)

    fun saveCookie(context: Context, cookie: String) = context.saveString(USER_KEY_COOKIE, cookie)

    fun fetchCookie(context: Context) = context.getString(USER_KEY_COOKIE)

    // =======================> LOCAL CACHES <=================================

    fun saveBannerCache(context: Context, bannerJson: String) = context.saveString(CACHE_KEY_BANNER, bannerJson)

    fun fetchBannerCache(context: Context) = context.getString(CACHE_KEY_BANNER)*/
}
