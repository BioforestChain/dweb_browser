package org.dweb_browser.browserUI.database

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.dweb_browser.browserUI.R
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.now
import org.dweb_browser.microservice.help.gson

/**
 * 该文件主要定义搜索引擎和引擎默认值，以及配置存储
 */

data class WebEngine(
  val name: String,
  val host: String,
  val format: String,
  var timeMillis: String = "",
  @DrawableRes val iconRes: Int = R.drawable.ic_web,
) {
  fun fit(url: String): Boolean {
    val current = Uri.parse(String.format(format, "test"))
    val query = current.queryParameterNames.first()
    val uri = Uri.parse(url)
    return uri.host == current.host && uri.path == current.path && uri.getQueryParameter(query) != null
  }

  fun queryName(): String {
    val current = Uri.parse(String.format(format, "test"))
    return current.queryParameterNames.first()
  }
}

val DefaultSearchWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(
      name = "百度",
      host = "m.baidu.com",
      iconRes = R.drawable.ic_engine_baidu,
      format = "https://m.baidu.com/s?word=%s"
    ),
    WebEngine(
      name = "搜狗",
      host = "wap.sogou.com",
      iconRes = R.drawable.ic_engine_sougou,
      format = "https://wap.sogou.com/web/searchList.jsp?keyword=%s"
    ),
    WebEngine(
      name = "360",
      host = "m.so.com",
      iconRes = R.drawable.ic_engine_360,
      format = "https://m.so.com/s?q=%s"
    ),
  )

val DefaultAllWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(name = "必应", host = "cn.bing.com", format = "https://cn.bing.com/search?q=%s"),
    WebEngine(name = "百度", host = "m.baidu.com", format = "https://m.baidu.com/s?word=%s"),
    WebEngine(name = "百度", host = "www.baidu.com", format = "https://www.baidu.com/s?wd=%s"),
    WebEngine(
      name = "谷歌",
      host = "www.google.com",
      format = "https://www.google.com/search?q=%s"
    ),
    WebEngine(
      name = "搜狗",
      host = "wap.sogou.com",
      format = "https://wap.sogou.com/web/searchList.jsp?keyword=%s"
    ),
    WebEngine(name = "搜狗", host = "www.sogou.com", format = "https://www.sogou.com/web?query=%s"),
    WebEngine(name = "360搜索", host = "m.so.com", format = "https://m.so.com/s?q=%s"),
    WebEngine(name = "360搜索", host = "www.so.com", format = "https://www.so.com/s?q=%s"),
    WebEngine(
      name = "雅虎",
      host = "search.yahoo.com",
      format = "https://search.yahoo.com/search?p=%s"
    )
  )

object WebEngineDB {
  private const val PREFERENCE_NAME_WebEngine = "WebEngine"
  private val Context.dataStoreWebEngine: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME_WebEngine)

  suspend fun queryBookWebsiteInfoList(): Flow<MutableList<WebEngine>> {
    return BrowserUIApp.Instance.appContext.dataStoreWebEngine.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableListOf<WebEngine>()
      pref.asMap().forEach { (_, value) ->
        val webSiteInfo = gson.fromJson((value as String), WebEngine::class.java)
        list.add(webSiteInfo)
      }
      list
    }
  }

  fun saveBookWebsiteInfo(webEngine: WebEngine) = runBlocking(ioAsyncExceptionHandler) {
    // edit 函数需要在挂起环境中执行
    BrowserUIApp.Instance.appContext.dataStoreWebEngine.edit { pref ->
      val timeMillis = webEngine.timeMillis.takeIf { it.isNotEmpty() } ?: now()
      pref[stringPreferencesKey(timeMillis)] = gson.toJson(webEngine)
    }
  }

  fun deleteBookWebsiteInfo(webEngine: WebEngine) = runBlocking(ioAsyncExceptionHandler) {
    BrowserUIApp.Instance.appContext.dataStoreWebEngine.edit { pref ->
      pref.remove(stringPreferencesKey(webEngine.timeMillis))
    }
  }

  fun clearBookWebsiteInfo() = runBlocking(ioAsyncExceptionHandler) {
    BrowserUIApp.Instance.appContext.dataStoreWebEngine.edit { pref ->
      pref.clear()
    }
  }
}