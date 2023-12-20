package org.dweb_browser.core.std.file.ext

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import dev.whyoleg.cryptography.algorithms.symmetric.AES
import dev.whyoleg.cryptography.operations.cipher.AuthenticatedCipher
import io.ktor.http.URLBuilder
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureClientRequest
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.toUtf8ByteArray

fun MicroModule.createStore(storeName: String, encrypt: Boolean) =
  MicroModuleStore(this, storeName, encrypt)

private val defaultSimpleStoreCache by atomic(WeakHashMap<MicroModule, MicroModuleStore>())

val MicroModule.store: MicroModuleStore
  get() = defaultSimpleStoreCache.getOrPut(this) {
    createStore("default", true)
  }

class MicroModuleStore(
  private val mm: MicroModule, private val storeName: String, private val encrypt: Boolean
) {
  private val taskQueues = Channel<Task<*>>(onBufferOverflow = BufferOverflow.SUSPEND)
  private val storeMutex = Mutex()

  init {
    mm.ioAsyncScope.launch {
      for (task in taskQueues) {
        // 防止并发修改异常
        storeMutex.withLock {
          try {
            @Suppress("UNCHECKED_CAST")
            (task.deferred as CompletableDeferred<Any>).complete(task.action() as Any)
          } catch (e: Throwable) {
            task.deferred.completeExceptionally(e)
          }
        }
      }
    }
  }

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

  private val queryPath =
    "/data/store/$storeName${if (encrypt) ".ebor" else ".cbor"}".encodeURIComponent()

  @OptIn(ExperimentalSerializationApi::class)
  private var _store = exec<MutableMap<String, ByteArray>> {
    cipher = if (encrypt) getCipher(cipherPlainKey) else null

    try {
      val readRequest = mm.nativeFetch("file://file.std.dweb/read?path=$queryPath&create=true")
      val data = readRequest.binary().let {
        if (it.isEmpty()) it else cipher?.decrypt(it) ?: it
      }

      if (data.isEmpty()) mutableMapOf()
      else Cbor.decodeFromByteArray(data)
    } catch (e: Throwable) {
      // debugger(e)
      mutableMapOf()
    }
  }

  suspend fun getStore() = _store.await()
  internal class Task<T>(val deferred: CompletableDeferred<T>, val action: suspend () -> T) {}

  private fun <T> exec(action: suspend () -> T): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    mm.ioAsyncScope.launch {
      taskQueues.send(Task(deferred, action))
    }
    return deferred
  }

  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> getAll(): MutableMap<String, T> {
    val data = mutableMapOf<String, T>()
    for (item in getStore()) {
      data[item.key] = Cbor.decodeFromByteArray<T>(item.value)
    }
    return data
  }

  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun delete(key: String): Boolean {
    val res = getStore().remove(key)
    save()
    return res !== null
  }

  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> getOrNull(key: String) =
    getStore()[key]?.let { Cbor.decodeFromByteArray<T>(it) }

  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> getOrPut(key: String, put: () -> T): T {
    val obj = getStore()
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

  @OptIn(ExperimentalSerializationApi::class)
  suspend fun save() {
    exec {
      val map = getStore()
      mm.nativeFetch(
        PureClientRequest(
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

  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> set(key: String, value: T) {
    val store = getStore()
    val newValue = Cbor.encodeToByteArray(value)
    if (!newValue.contentEquals(store[key])) {
      store[key] = newValue
      save()
    }
  }
}