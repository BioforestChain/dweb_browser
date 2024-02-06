package org.dweb_browser.core.std.file.ext

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
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.debugMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.pure.crypto.cipher.cipher_aes_256_gcm
import org.dweb_browser.pure.crypto.decipher.decipher_aes_256_gcm
import org.dweb_browser.pure.crypto.hash.sha256
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

fun MicroModule.createStore(storeName: String, encrypt: Boolean) =
  MicroModuleStore(this, storeName, encrypt)

fun MicroModule.createStore(storeName: String, cipherChunkKey: ByteArray, encrypt: Boolean) =
  MicroModuleStore(this, storeName, cipherChunkKey, encrypt)

private val defaultSimpleStoreCache by atomic(WeakHashMap<MicroModule, MicroModuleStore>())

val MicroModule.store: MicroModuleStore
  get() = defaultSimpleStoreCache.getOrPut(this) {
    createStore("default", true)
  }

class MicroModuleStore(
  private val mm: MicroModule,
  private val storeName: String,
  private val cipherChunkKey: ByteArray?,
  private val encrypt: Boolean,
  ) {
  constructor(
    mm: MicroModule, storeName: String, encrypt: Boolean
  ) : this(mm, storeName, null, encrypt)

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
  private var cipherKey: ByteArray? = null

  private val queryPath =
    "/data/store/$storeName${if (encrypt) ".ebor" else ".cbor"}".encodeURIComponent()

  @OptIn(ExperimentalSerializationApi::class)
  private var _store = exec<MutableMap<String, ByteArray>> {
    if (encrypt) {
      cipherKey = if (cipherChunkKey != null) {
        sha256(cipherChunkKey)
      } else {
        sha256(cipherPlainKey)
      }
    }

    try {
      val readRequest = mm.readFile(queryPath, true)
      val data = readRequest.binary().let {
        if (it.isEmpty()) it else cipherKey?.let { key -> decipher_aes_256_gcm(key, it) } ?: it
      }

      if (data.isEmpty()) mutableMapOf()
      else Cbor.decodeFromByteArray(data)
    } catch (e: Throwable) {
      // debugger(e)
      debugMicroModule("store/init", "e->${e.message}")
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

  suspend inline fun clear() {
    getStore().clear()
    save()
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
      mm.writeFile(
        path = queryPath,
        body = IPureBody.from(
          Cbor.encodeToByteArray(map).let {
            cipherKey?.let { key -> cipher_aes_256_gcm(key, it) } ?: it
          }
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