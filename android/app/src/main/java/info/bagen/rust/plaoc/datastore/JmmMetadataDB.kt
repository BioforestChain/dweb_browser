package info.bagen.rust.plaoc.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.ProtocolStringList
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.Mmid.JmmMetaDataMapPreferences
import info.bagen.rust.plaoc.microService.helper.Mmid.JmmMetadataPreferences
import info.bagen.rust.plaoc.microService.helper.Mmid.MainServerPreferences
import info.bagen.rust.plaoc.microService.helper.Mmid.OpenWebViewPreferences
import info.bagen.rust.plaoc.microService.helper.Mmid.StaticWebServerPreferences
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

object JmmMetadataDB {
  private val Context.jmmMetaDataStore: DataStore<JmmMetaDataMapPreferences> by dataStore(
    fileName = "jmmMetadata.pb", serializer = JmmMetadataSerializer
  )

  fun queryJmmMetadata() = runBlocking(Dispatchers.IO) {
    App.appContext.jmmMetaDataStore.data.map { data ->
      data.dataMap.forEach { (key, value) ->
        JmmNMM.apps[key] = value.toJsMicroModule()
      }
    }
  }

  fun saveJmmMetadata(jmmMetadata: JmmMetadata? = null) = runBlocking(Dispatchers.IO) {
    App.appContext.jmmMetaDataStore.updateData { jmmMetaDataMapPreferences ->
      jmmMetaDataMapPreferences.toBuilder().apply {
        jmmMetadata?.let { item ->
          putData(item.id, item.toJmmMetadataPreferences())
        } ?: {
          JmmNMM.apps.forEach { (key, value) ->
            putData(key, value.metadata.toJmmMetadataPreferences())
          }
        }
      }.build()
    }
  }
}

object JmmMetadataSerializer : Serializer<JmmMetaDataMapPreferences> {
  override val defaultValue: JmmMetaDataMapPreferences =
    JmmMetaDataMapPreferences.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): JmmMetaDataMapPreferences {
    try {
      return JmmMetaDataMapPreferences.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read JmmMetadataPreferences.", exception)
    }
  }

  override suspend fun writeTo(
    t: JmmMetaDataMapPreferences, output: OutputStream
  ) = t.writeTo(output)
}

fun JmmMetadataPreferences.toJsMicroModule(): JsMicroModule {
  return JsMicroModule(
    JmmMetadata(
      id = this.id,
      server = this.server.toMainServer(),
      title = this.title,
      subtitle = this.subtitle,
      icon = this.icon,
      downloadUrl = this.downloadUrl,
      images = this.imagesList,
      introduction = this.introduction,
      author = this.authorList,
      version = this.version,
      keywords = this.keywordsList,
      home = this.home,
      size = this.size,
      fileHash = this.fileHash,
      permissions = this.permissionsList,
      plugins = this.pluginsList,
      releaseDate = this.releaseDate,
      staticWebServers = this.staticWebServersList.listTransform { it.toStaticWebServer() },
      openWebViewList = this.openWebViewListList.listTransform { it.toOpenWebView() }
    )
  )
}

fun JmmMetadata.toJmmMetadataPreferences(): JmmMetadataPreferences {
  val build = JmmMetadataPreferences.newBuilder()
  build.id = this.id
  build.server = this.server.toMainServerPreferences()
  build.title = this.title
  build.subtitle = this.subtitle
  build.icon = this.icon
  build.downloadUrl = this.downloadUrl
  build.imagesList.addAll(this.images)
  build.introduction = this.introduction
  build.authorList.addAll(this.author)
  build.version = this.version
  build.keywordsList.addAll(this.keywords)
  build.home = this.home
  build.size = this.size
  build.fileHash = this.fileHash
  build.permissionsList.addAll(this.permissions)
  build.pluginsList.addAll(this.plugins)
  build.releaseDate = this.releaseDate
  build.staticWebServersList.addAll(
    this.staticWebServers.listTransform { it.toStaticWebServerPreferences() }
  )
  build.openWebViewListList.addAll(
    this.openWebViewList.listTransform { it.toOpenWebViewPreferences() }
  )
  return build.build()
}

fun MainServerPreferences.toMainServer(): JmmMetadata.MainServer {
  return JmmMetadata.MainServer(root = this.root, entry = this.entry)
}

fun StaticWebServerPreferences.toStaticWebServer(): JmmMetadata.StaticWebServer {
  return JmmMetadata.StaticWebServer(
    root = this.root, entry = this.entry, subdomain = this.subdomain, port = this.port.toInt()
  )
}

fun OpenWebViewPreferences.toOpenWebView(): JmmMetadata.OpenWebView {
  return JmmMetadata.OpenWebView(url = this.url)
}

fun JmmMetadata.MainServer.toMainServerPreferences(): MainServerPreferences {
  val builder = MainServerPreferences.newBuilder()
  builder.root = this.root
  builder.entry = this.entry
  return builder.build()
}

fun JmmMetadata.StaticWebServer.toStaticWebServerPreferences(): StaticWebServerPreferences {
  val builder = StaticWebServerPreferences.newBuilder()
  builder.root = this.root
  builder.entry = this.entry
  builder.subdomain = this.subdomain
  builder.port = this.port.toLong()
  return builder.build()
}

fun JmmMetadata.OpenWebView.toOpenWebViewPreferences(): OpenWebViewPreferences {
  val builder = OpenWebViewPreferences.newBuilder()
  builder.url = this.url
  return builder.build()
}

inline fun <T, R> List<T>.listTransform(call: (T) -> R): List<R> {
  val list = mutableListOf<R>()
  forEach { list.add(call(it)) }
  return list
}

fun ProtocolStringList.addAll(list: List<String>?) {
  list?.let { list ->
    this.clear()
    this.addAll(list)
  }
}
