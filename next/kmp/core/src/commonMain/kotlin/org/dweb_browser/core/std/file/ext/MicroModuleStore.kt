package org.dweb_browser.core.std.file.ext

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.OrderInvoker
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.pure.crypto.cipher.cipher_aes_256_gcm
import org.dweb_browser.pure.crypto.decipher.decipher_aes_256_gcm
import org.dweb_browser.pure.crypto.hash.sha256
import org.dweb_browser.pure.http.IPureBody

fun MicroModule.Runtime.createStore(storeName: String, encrypt: Boolean) =
  MicroModuleStore(this, storeName, encrypt)

fun MicroModule.Runtime.createStore(
  storeName: String,
  cipherChunkKey: ByteArray,
  encrypt: Boolean,
) = MicroModuleStore(this, storeName, cipherChunkKey, encrypt)

private val defaultSimpleStoreCache by atomic(WeakHashMap<MicroModule.Runtime, MicroModuleStore>())

val MicroModule.Runtime.store: MicroModuleStore
  get() = defaultSimpleStoreCache.getOrPut(this) {
    createStore("default", true)
  }

class MicroModuleStore(
  val mm: MicroModule.Runtime,
  private val storeName: String,
  private val cipherChunkKey: ByteArray?,
  private val encrypt: Boolean,
) {
  constructor(
    mm: MicroModule.Runtime, storeName: String, encrypt: Boolean,
  ) : this(mm, storeName, null, encrypt)

  private val orderInvoker = OrderInvoker()
  private suspend fun <T> exec(action: suspend () -> T) =
    orderInvoker.tryInvoke(0, invoker = action)

  private fun <T> execDeferred(action: suspend () -> T): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    mm.scopeLaunch(cancelable = true) {
      runCatching {
        deferred.complete(exec(action))
      }.getOrElse {
        deferred.completeExceptionally(it)
      }
    }
    return deferred
  }

  private val cipherPlainKey = mm.mmid + "/" + storeName
  private var cipherKey: ByteArray? = null

  private val queryPath =
    "/data/store/$storeName${if (encrypt) ".ebor" else ".cbor"}".encodeURIComponent()

  @OptIn(ExperimentalSerializationApi::class)
  private var _store = execDeferred {
    if (encrypt) {
      cipherKey = if (cipherChunkKey != null) {
        sha256(cipherChunkKey)
      } else {
        sha256(cipherPlainKey)
      }
    }

    try {
      mm.debugMM("store-init", queryPath)
      val readRequest = mm.readFile(queryPath, true)
      val data = readRequest.binary().let {
        if (it.isEmpty()) it else cipherKey?.let { key -> decipher_aes_256_gcm(key, it) } ?: it
      }

      val result: MutableMap<String, ByteArray> = when {
        data.isEmpty() -> SafeHashMap()
        else -> SafeHashMap(Cbor.decodeFromByteArray(data))
      }
      mm.debugMM("store-init-data", result)
      result
    } catch (e: Throwable) {
      // debugger(e)
      mm.debugMM("store-init-error", null, e)
      SafeHashMap()
    }
  }

  suspend fun getStore() = _store.await()

  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> getAll(): MutableMap<String, T> {
    val data = mutableMapOf<String, T>()
    for (item in getStore()) {
      try { // 使用try的目的是为了保证后面对象字段变更后，存储了新的内容。但由于存在旧数据解析失败导致的所有数据无法获取问题
        data[item.key] =
          Cbor { ignoreUnknownKeys = true }.decodeFromByteArray<T>(item.value) // 忽略未知的字段
      } catch (e: Throwable) {
        mm.debugMM("store/getAll", item.key, e)
      }
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
  suspend fun save() = exec {
    val map = getStore()
    mm.writeFile(
      path = queryPath,
      body = IPureBody.from(Cbor.encodeToByteArray(map).let {
        cipherKey?.let { key -> cipher_aes_256_gcm(key, it) } ?: it
      }),
      backup = true,
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  suspend inline fun <reified T> set(key: String, value: T) {
//    mm.debugMM("store-set") { "key=$key value=$value" }
    val store = getStore()
    val newValue = Cbor.encodeToByteArray(value)
    if (!newValue.contentEquals(store[key])) {
      store[key] = newValue
      save()
    }
  }
}