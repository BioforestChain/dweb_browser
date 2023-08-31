package org.dweb_browser.browserUI.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest
import org.dweb_browser.microservice.help.types.MMID

object JsMicroModuleStore {
  private const val PREFERENCE_NAME = "AppInfo"
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

  fun queryAppInfo(key: String): Flow<JmmAppInstallManifest?> {
    return BrowserUIApp.Instance.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      pref[stringPreferencesKey(key)]?.let {
        Json.decodeFromString(JmmAppInstallManifest.serializer(), it)
      }
    }
  }

  suspend fun queryAppInfoList(): Flow<MutableList<JmmAppInstallManifest>> {
    return BrowserUIApp.Instance.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableListOf<JmmAppInstallManifest>()
      pref.asMap().forEach { (key, value) ->
        list.add(Json.decodeFromString<JmmAppInstallManifest>(value as String))
      }
      list
    }
  }

  fun saveAppInfo(mmid: MMID, jmmAppInstallManifest: JmmAppInstallManifest) =
    runBlocking(Dispatchers.IO) {
      // edit 函数需要在挂起环境中执行
      BrowserUIApp.Instance.appContext.dataStore.edit { pref ->
        pref[stringPreferencesKey(mmid)] = Json.encodeToString(jmmAppInstallManifest)
      }
    }

  suspend fun saveAppInfoList(list: MutableMap<MMID, JmmAppInstallManifest>) =
    runBlocking(Dispatchers.IO) {
      // edit 函数需要在挂起环境中执行
      BrowserUIApp.Instance.appContext.dataStore.edit { pref ->
        list.forEach { (key, appMetaData) ->
          pref[stringPreferencesKey(key)] = appMetaData.toString()
        }
      }
    }

  suspend fun deleteAppInfo(mmid: MMID) {
    BrowserUIApp.Instance.appContext.dataStore.edit { pref ->
      pref.remove(stringPreferencesKey(mmid))
    }
  }
}