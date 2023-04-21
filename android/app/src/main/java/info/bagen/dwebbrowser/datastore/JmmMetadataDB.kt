package info.bagen.dwebbrowser.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.gson
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.defaultJmmMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object JmmMetadataDB {
  private const val PREFERENCE_NAME = "JmmMetadata"
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

  fun queryJmmMetadata(
    key: String, defaultValue: JmmMetadata = defaultJmmMetadata
  ): Flow<JmmMetadata> {
    return App.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      gson.fromJson(pref[stringPreferencesKey(key)], JmmMetadata::class.java)
        ?: defaultValue       // stringPreferencesKey 生成一个读取string 类型的key
    }
  }

  fun queryJmmMetadataList(): Flow<MutableMap<Mmid, JmmMetadata>> {
    return App.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableMapOf<Mmid, JmmMetadata>()
      pref.asMap().forEach { (key, value) ->
        list[key.name] = gson.fromJson((value as String), JmmMetadata::class.java)
      }
      list
    }
  }

  fun saveJmmMetadata(mmid: Mmid, JmmMetadata: JmmMetadata) = runBlocking(Dispatchers.IO) {
    // edit 函数需要在挂起环境中执行
    App.appContext.dataStore.edit { pref ->
      pref[stringPreferencesKey(mmid)] = gson.toJson(JmmMetadata)
    }
  }

  fun saveAllJmmMetadata(list: MutableMap<Mmid, JmmMetadata>) = runBlocking(Dispatchers.IO) {
    // edit 函数需要在挂起环境中执行
    App.appContext.dataStore.edit { pref ->
      list.forEach { (key, value) ->
        pref[stringPreferencesKey(key)] = gson.toJson(value)
      }
    }
  }
}
