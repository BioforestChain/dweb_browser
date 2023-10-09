package org.dweb_browser.microservice.std.file.ext

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import dev.whyoleg.cryptography.algorithms.symmetric.AES
import dev.whyoleg.cryptography.operations.cipher.AuthenticatedCipher
import io.ktor.http.URLBuilder
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.http.IPureBody
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.std.dns.nativeFetch
fun MicroModule.createStore(storeName: String, encrypt: Boolean) =
  MicroModuleStore(this, storeName, encrypt)

private val defaultSimpleStoreCache by atomic(WeakHashMap<MicroModule, MicroModuleStore>())

val MicroModule.store: MicroModuleStore
  get() = defaultSimpleStoreCache.getOrPut(this) {
    createStore("default", true)
  }

class MicroModuleStore(
  private val mm: MicroModule, val storeName: String, val encrypt: Boolean
) {
  private val cipherPlainKey = mm.mmid + "/" + storeName
  private var cipher: AuthenticatedCipher? = null

  companion object {
    // 使用 SymmetricKeySize.B256 的长度作为key
    private val sha256 = CryptographyProvider.Default.get(SHA256)
    private val aesGcm = CryptographyProvider.Default.get(AES.GCM)
    private val keyDecoder = aesGcm.keyDecoder()

    /**
     * 将指定字符串，通过 sha256 转成合法的 AES.GCM 的 key
     */
    suspend fun getCipher(plainKey: String) = keyDecoder.decodeFrom(
      AES.Key.Format.RAW, sha256.hasher().hash(plainKey.toUtf8ByteArray())
    ).cipher()
  }

  private var _store: Deferred<MutableMap<String, ByteArray>> = mm.ioAsyncScope.async {
    val map = mutableMapOf<String, ByteArray>()
    // 计算map的值
    map
  }
  val store get() = _store
  private val queryPath =
    "/data/store/$storeName${if (encrypt) ".ebor" else ".cbor"}".encodeURIComponent()

  init {
    exec {
      cipher = if (encrypt) getCipher(cipherPlainKey) else null

      val data =
        mm.nativeFetch("file://file.std.dweb/read?path=$queryPath&create=true").binary().let {
          cipher?.decrypt(it) ?: it
        }

      Cbor.decodeFromByteArray<Map<String, ByteArray>>(data)
    }
  }

  internal class Task<T>(val deferred: CompletableDeferred<T>, val action: suspend () -> T) {}

  private val taskQueues = Channel<Task<*>>(onBufferOverflow = BufferOverflow.SUSPEND)

  init {
    mm.ioAsyncScope.launch {
      for (task in taskQueues) {
        try {
          (task.deferred as CompletableDeferred<Any>).complete(task.action() as Any)
        }catch (e:Throwable){
          task.deferred.completeExceptionally(e)
        }
      }
    }
  }
  private fun <T> exec(action: suspend () -> T): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    mm.ioAsyncScope.launch {
      taskQueues.send(Task(deferred, action))
    }
    return deferred
  }

  suspend inline fun <reified T> getOrNull(key: String) =
    store.await()[key]?.let { Cbor.decodeFromByteArray<T>(it) }

  suspend inline fun <reified T> getOrPut(key: String, put: () -> T): T {
    val obj = store.await()
    return obj[key].let { it ->
      if (it != null) Cbor.decodeFromByteArray<T>(it)
      else put().also {
        obj[key] = Cbor.encodeToByteArray<T>(it)
        save()
      }
    }
  }


  suspend inline fun <reified T> get(key: String) = getOrPut<T>(key) {
    throw Exception("no found data for key: $key")
  }

  suspend fun save() {
    exec {
      val map = store.await()
      mm.nativeFetch(
        PureRequest(
          URLBuilder("file://file.std.dweb/write").apply {
            parameters["path"] = queryPath
            parameters["create"] = "true"
          }.buildUnsafeString(),
          IpcMethod.POST,
          body = IPureBody.from(
            Cbor.encodeToByteArray(map).let { cipher?.encrypt(it) ?: it })
        )
      )
    }.await()
  }

  suspend inline fun <reified T> set(key: String, value: T) {
    store.await()[key] = Cbor.encodeToByteArray(value)
    save()
  }
}