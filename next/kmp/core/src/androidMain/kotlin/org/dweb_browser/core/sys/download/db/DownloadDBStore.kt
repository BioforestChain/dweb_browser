package org.dweb_browser.core.sys.download.db

import android.content.Context
import android.graphics.Bitmap
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.BitmapUtil
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID

enum class AppType(val type:String) {
  Jmm("jmm"),
  Link("link"),
  ;

  companion object{
    private val sha256 = CryptographyProvider.Default.get(SHA256)

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun createLinkId(url: String) = "${
      sha256.hasher().hash(url.toUtf8ByteArray()).toHexString(0, 4, HexFormat.Default)
    }.link.dweb"
  }
 }
@Serializable
data class DeskWebLink(
  val id: String,
  val title: String,
  val url: String,
  val icon: ImageResource
)
@Serializable
data class DeskAppInfo(
  val appType: AppType,
  val metadata: JmmAppInstallManifest? = null,
  val weblink: DeskWebLink? = null,
)

private const val DeskWebLinkStart = "file:///web_icons/"

suspend fun createDeskWebLink(context: Context, title: String, url: String, bitmap: Bitmap?): DeskWebLink {
  val imageResource = bitmap?.let {
    BitmapUtil.saveBitmapToIcons(context, it)?.let { src ->
      ImageResource(src = "$DeskWebLinkStart$src")
    }
  }
  return DeskWebLink(
    id = AppType.createLinkId(url),
    title = title,
    url = url,
    icon = imageResource ?: ImageResource(src = "file:///sys/browser/web/logo.svg")
  )
}

object DownloadDBStore {
  private const val PREFERENCE_NAME = "DownloadDBStore"
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

  suspend fun saveAppInfo(context: Context, mmid: MMID, metadata: JmmAppInstallManifest) =
    runBlocking(Dispatchers.IO) {
      // edit 函数需要在挂起环境中执行
      context.dataStore.edit { pref ->
        pref[stringPreferencesKey("${AppType.Jmm}$mmid")] = Json.encodeToString(metadata)
      }
    }

  suspend fun saveWebLink(context: Context, item: DeskWebLink) =
    runBlocking(Dispatchers.IO) {
      // edit 函数需要在挂起环境中执行
      context.dataStore.edit { pref ->
        pref[stringPreferencesKey(item.id)] = Json.encodeToString(item)
      }
    }

  suspend fun deleteDeskAppInfo(context: Context, mmid: MMID) {
    context.dataStore.edit { pref ->
      val name = if (mmid.endsWith("${AppType.Link.type}.dweb")) mmid else "${AppType.Jmm}$mmid"
      pref.remove(stringPreferencesKey(name))
    }
  }

  suspend fun queryDeskAppInfoList(context: Context): Flow<MutableList<DeskAppInfo>> {
    return context.dataStore.data.catch { e ->  // Flow 中发生异常可使用这种方式捕获，catch 块是可选的
      if (e is IOException) {
        e.printStackTrace()
        emit(emptyPreferences())
      } else {
        throw e
      }
    }.map { pref ->
      val list = mutableListOf<DeskAppInfo>()
      pref.asMap().forEach { (key, value) ->
        val item = if (key.name.startsWith(AppType.Jmm.name)) {
          DeskAppInfo(
            AppType.Jmm,
            metadata = Json.decodeFromString<JmmAppInstallManifest>(value as String)
          )
        } else if (key.name.endsWith("${AppType.Link.type}.dweb")) {
          DeskAppInfo(AppType.Link, weblink = Json.decodeFromString<DeskWebLink>(value as String))
        } else {
          null
        }
        item?.let { list.add(it) }
      }
      list
    }
  }
}