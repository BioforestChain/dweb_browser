package org.dweb_browser.browserUI.microService.browser.types

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.helper.ImageResource
import java.io.IOException

@Serializable
data class DeskLinkMetaData(
  val title: String, val icon: ImageResource? = null, val url: String, val id: Long
)

object DeskLinkMetaDataStore {
  private const val PREFERENCE_NAME = "DeskLink"
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

  suspend fun queryDeskLinkList(): Flow<MutableList<DeskLinkMetaData>> {
    return BrowserUIApp.Instance.appContext.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableListOf<DeskLinkMetaData>()
      pref.asMap().forEach { (key, value) ->
        list.add(Json.decodeFromString<DeskLinkMetaData>(value as String))
      }
      list
    }
  }

  suspend fun saveDeskLink(item: DeskLinkMetaData) = // edit 函数需要在挂起环境中执行
    BrowserUIApp.Instance.appContext.dataStore.edit { pref ->
      pref[stringPreferencesKey(item.id.toString())] = Json.encodeToString(item)
    }

  suspend fun deleteDeskLink(item: DeskLinkMetaData) {
    BrowserUIApp.Instance.appContext.dataStore.edit { pref ->
      pref.remove(stringPreferencesKey(item.toString()))
    }
  }
}