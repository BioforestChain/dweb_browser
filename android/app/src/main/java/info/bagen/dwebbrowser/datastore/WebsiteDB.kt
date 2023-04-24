package info.bagen.dwebbrowser.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.reflect.TypeToken
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.helper.gson
import info.bagen.dwebbrowser.ui.entity.WebSiteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object WebsiteDB {
  private const val PREFERENCE_NAME_HISTORY = "Website-history"
  private const val PREFERENCE_NAME_BOOK = "Website-book"

  private val KEY_PREFER_BOOK = stringPreferencesKey(PREFERENCE_NAME_BOOK)

  private val Context.dataStoreHistory: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME_HISTORY)
  private val Context.dataStoreBook: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME_BOOK)

  /**
   * 历史记录部分
   */
  suspend fun queryHistoryWebsiteInfoList(): Flow<MutableMap<String, MutableList<WebSiteInfo>>> {
    return App.appContext.dataStoreHistory.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableMapOf<String, MutableList<WebSiteInfo>>()
      pref.asMap().forEach { (key, value) ->
        list[key.name] =
          gson.fromJson((value as String), object : TypeToken<MutableList<WebSiteInfo>>() {}.type)
      }
      list
    }
  }

  fun saveHistoryWebsiteInfo(webSiteInfo: WebSiteInfo, list: MutableList<WebSiteInfo>?) =
    runBlocking(Dispatchers.IO) {
      // edit 函数需要在挂起环境中执行
      App.appContext.dataStoreHistory.edit { pref ->
        val webSiteList = list ?: arrayListOf()
        webSiteList.add(0, webSiteInfo)
        pref[stringPreferencesKey(currentLocalTime)] = gson.toJson(webSiteList)
      }
    }

  /**
   * 书签列表部分
   */
  suspend fun queryBookWebsiteInfoList(): Flow<MutableList<WebSiteInfo>> {
    return App.appContext.dataStoreBook.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      gson.fromJson(pref[KEY_PREFER_BOOK], object : TypeToken<MutableList<WebSiteInfo>>() {}.type)
    }
  }

  fun saveBookWebsiteInfo(webSiteInfo: WebSiteInfo, list: MutableList<WebSiteInfo>?) =
    runBlocking(Dispatchers.IO) {
      // edit 函数需要在挂起环境中执行
      App.appContext.dataStoreBook.edit { pref ->
        val webSiteList = list ?: arrayListOf()
        webSiteList.add(webSiteInfo)
        pref[KEY_PREFER_BOOK] = gson.toJson(webSiteList)
      }
    }

  fun compareWithLocalTime(time: String): String {
    val local = LocalDate.now()
    val compare = LocalDate.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE"))
    return if (compare >= local) "今天"
    else if (compare >= local.minusDays(1)) "昨天"
    else time
  }

  val currentLocalTime: String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE"))
}
