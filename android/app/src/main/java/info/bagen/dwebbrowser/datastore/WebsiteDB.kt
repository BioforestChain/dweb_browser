package info.bagen.dwebbrowser.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.helper.gson
import info.bagen.dwebbrowser.ui.entity.WebSiteInfo
import io.ktor.util.date.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object WebsiteDB {
  private const val PREFERENCE_NAME = "Website"

  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

  fun queryWebsiteInfoList(): Flow<MutableMap<Long, WebSiteInfo>> {
    return App.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableMapOf<Long, WebSiteInfo>()
      pref.asMap().forEach { (key, value) ->
        list[key.name.toLong()] = gson.fromJson((value as String), WebSiteInfo::class.java)
      }
      list
    }
  }

  fun saveWebsiteInfo(webSiteInfo: WebSiteInfo) = runBlocking(Dispatchers.IO) {
    // edit 函数需要在挂起环境中执行
    App.appContext.dataStore.edit { pref ->
      pref[stringPreferencesKey(getTimeMillis().toString())] = gson.toJson(webSiteInfo)
    }
  }
}
