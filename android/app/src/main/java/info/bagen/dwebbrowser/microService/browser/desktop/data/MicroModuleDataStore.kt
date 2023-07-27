package info.bagen.dwebbrowser.microService.browser.desktop.data

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
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.gson

object MicroModuleDataStore {
  private const val PREFERENCE_NAME = "MicroModule"
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

  fun queryAppInfo(key: String): Flow<MicroModuleManifest?> {
    return BrowserUIApp.Instance.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      gson.fromJson(pref[stringPreferencesKey(key)], MicroModuleManifest::class.java)
    }
  }

  suspend fun queryAppInfoList(): Flow<MutableList<MicroModuleManifest>> {
    return BrowserUIApp.Instance.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableListOf<MicroModuleManifest>()
      pref.asMap().forEach { (key, value) ->
        list.add(gson.fromJson(value as String, MicroModuleManifest::class.java))
      }
      list
    }
  }

  fun saveAppInfo(mmid: MMID, MicroModuleManifest: MicroModuleManifest) = runBlocking(
    Dispatchers.IO) {
    // edit 函数需要在挂起环境中执行
    BrowserUIApp.Instance.appContext.dataStore.edit { pref ->
      pref[stringPreferencesKey(mmid)] = gson.toJson(MicroModuleManifest)
    }
  }

  suspend fun saveAppInfoList(list: MutableMap<MMID, MicroModuleManifest>) = runBlocking(
    Dispatchers.IO) {
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