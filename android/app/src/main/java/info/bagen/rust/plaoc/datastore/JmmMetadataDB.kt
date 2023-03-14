package info.bagen.rust.plaoc.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule
import info.bagen.rust.plaoc.microService.sys.jmm.defaultJmmMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object JmmMetadataDB {
  private const val PREFERENCE_NAME = "JmmMetadata"
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

  fun queryJsMicroModule(
    key: String, defaultValue: JsMicroModule = JsMicroModule(defaultJmmMetadata)
  ): Flow<JsMicroModule> {
    return App.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      Gson().fromJson(pref[stringPreferencesKey(key)], JsMicroModule::class.java)
        ?: defaultValue       // stringPreferencesKey 生成一个读取string 类型的key
    }
  }

  fun queryJsMicroModuleList(): Flow<MutableMap<Mmid, JsMicroModule>> {
    return App.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableMapOf<Mmid, JsMicroModule>()
      pref.asMap().forEach { (key, value) ->
        list[key.name] = Gson().fromJson((value as String), JsMicroModule::class.java)
      }
      list
    }
  }

  fun saveJsMicroModule(mmid: Mmid, jsMicroModule: JsMicroModule) = runBlocking(Dispatchers.IO) {
    // edit 函数需要在挂起环境中执行
    App.appContext.dataStore.edit { pref ->
      pref[stringPreferencesKey(mmid)] = Gson().toJson(jsMicroModule)
    }
  }

  fun saveAllJsMicroModule(list: MutableMap<Mmid, JsMicroModule>) = runBlocking(Dispatchers.IO) {
    // edit 函数需要在挂起环境中执行
    App.appContext.dataStore.edit { pref ->
      list.forEach { (key, value) ->
        pref[stringPreferencesKey(key)] = Gson().toJson(value)
      }
    }
  }
}
