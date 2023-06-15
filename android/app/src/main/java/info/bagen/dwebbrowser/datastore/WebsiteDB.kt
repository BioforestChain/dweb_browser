package info.bagen.dwebbrowser.datastore

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.database.WebSiteInfo
import org.dweb_browser.helper.*
import io.ktor.util.date.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.dweb_browser.microservice.help.gson
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

object WebsiteDB {
  private const val PREFERENCE_NAME_HISTORY = "Website-history"
  private const val PREFERENCE_NAME_BOOK = "Website-book"

  enum class PreferenceType() {
    WebsiteHistory, WebsiteBook;

    val preferenceKey = stringPreferencesKey(this.name)

    val data: DataStore<Preferences>?
      get() = when (this.name) {
        WebsiteHistory.name -> {
          App.appContext.dataStoreHistory
        }
        WebsiteBook.name -> {
          App.appContext.dataStoreBook
        }
        else -> null
      }
  }

  private val Context.dataStoreHistory: DataStore<Preferences> by preferencesDataStore(
    PreferenceType.WebsiteHistory.name
  )
  private val Context.dataStoreBook: DataStore<Preferences> by preferencesDataStore(
    PreferenceType.WebsiteBook.name
  )

  /**
   * 历史记录部分
   */
  suspend fun queryHistoryWebsiteInfoMap(): Flow<MutableMap<String, MutableList<WebSiteInfo>>> {
    return App.appContext.dataStoreHistory.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val map = mutableMapOf<String, MutableList<WebSiteInfo>>()
      pref.asMap().forEach { (key, value) ->
        // map[key.name] = gson.fromJson((value as String), object : TypeToken<MutableList<WebSiteInfo>>() {}.type)
        map.getOrPut(key.name.toLong().toMapKey()) {
          mutableListOf()
        }.also {
          it.add(0, gson.fromJson((value as String), WebSiteInfo::class.java))
        }
      }
      map
    }
  }

  fun saveHistoryWebsiteInfo(webSiteInfo: WebSiteInfo) = runBlocking(ioAsyncExceptionHandler) {
    // edit 函数需要在挂起环境中执行
    if (webSiteInfo.url.startsWith("file:///android_asset")) return@runBlocking
    /*App.appContext.dataStoreHistory.edit { pref ->
      val timeMillis =
        webSiteInfo.timeMillis.takeIf { it.isNotEmpty() } ?: getTimeMillis().toString()
      webSiteInfo.timeMillis = timeMillis
      pref[stringPreferencesKey(timeMillis)] = gson.toJson(webSiteInfo)
    }*/
  }

  fun deleteHistoryWebsiteInfo(webSiteInfo: WebSiteInfo) = runBlocking(ioAsyncExceptionHandler) {
    /*App.appContext.dataStoreHistory.edit { pref ->
      pref.remove(stringPreferencesKey(webSiteInfo.timeMillis))
    }*/
  }

  fun clearHistoryWebsiteInfo() = runBlocking(ioAsyncExceptionHandler) {
    App.appContext.dataStoreHistory.edit { pref ->
      pref.clear()
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
      val list = mutableListOf<WebSiteInfo>()
      pref.asMap().forEach { (key, value) ->
        val webSiteInfo = gson.fromJson((value as String), WebSiteInfo::class.java)
        list.add(webSiteInfo)
      }
      list
    }
  }

  fun saveBookWebsiteInfo(webSiteInfo: WebSiteInfo) = runBlocking(ioAsyncExceptionHandler) {
    // edit 函数需要在挂起环境中执行
    if (webSiteInfo.url.startsWith("file:///android_asset")) return@runBlocking
    /*App.appContext.dataStoreBook.edit { pref ->
      val timeMillis =
        webSiteInfo.timeMillis.takeIf { it.isNotEmpty() } ?: getTimeMillis().toString()
      webSiteInfo.timeMillis = timeMillis
      pref[stringPreferencesKey(timeMillis)] = gson.toJson(webSiteInfo)
    }*/
  }

  fun deleteBookWebsiteInfo(webSiteInfo: WebSiteInfo) = runBlocking(ioAsyncExceptionHandler) {
    /*App.appContext.dataStoreBook.edit { pref ->
      pref.remove(stringPreferencesKey(webSiteInfo.timeMillis))
    }*/
  }

  fun clearBookWebsiteInfo() = runBlocking(ioAsyncExceptionHandler) {
    App.appContext.dataStoreBook.edit { pref ->
      pref.clear()
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

  @SuppressLint("SimpleDateFormat")
  fun Long.toMapKey() = SimpleDateFormat("yyyy-MM-dd EEEE").format(Date(this))
}
