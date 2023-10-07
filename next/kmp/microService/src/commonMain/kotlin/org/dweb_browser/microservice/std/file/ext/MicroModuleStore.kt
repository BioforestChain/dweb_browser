package org.dweb_browser.microservice.std.file.ext

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import dev.whyoleg.cryptography.algorithms.symmetric.AES
import dev.whyoleg.cryptography.operations.cipher.AuthenticatedCipher
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.http.IPureBody
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.std.dns.nativeFetch


@ExperimentalSerializationApi
fun MicroModule.createSimpleStore(storeName: String = "default") = MicroModuleStore(this, storeName)

@ExperimentalSerializationApi
private val defaultSimpleStoreCache by atomic(WeakHashMap<MicroModule, MicroModuleStore>())

@ExperimentalSerializationApi
val MicroModule.store: MicroModuleStore
  get() = defaultSimpleStoreCache.getOrPut(this) {
    createSimpleStore()
  }

@ExperimentalSerializationApi
class MicroModuleStore(
  private val mm: MicroModule,
  val storeName: String,
  val encrypt: Boolean = true
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

  val store = mutableMapOf<String, ByteArray>()
  private val queryPath =
    "/data/store/$storeName${if (encrypt) ".ebor" else ".cbor"}".encodeURIComponent()

  init {
    runBlocking {
      exec {
        cipher = if (encrypt) getCipher(cipherPlainKey) else null

        val data =
          mm.nativeFetch("file://file.std.dweb/read?path=$queryPath&create=true").binary().let {
            cipher?.decrypt(it) ?: it
          }

        store += Cbor.decodeFromByteArray<Map<String, ByteArray>>(data)
      }
    }
  }

  internal class Task<T>(val deferred: CompletableDeferred<T>, val action: suspend () -> T) {}

  private val taskQueues = Channel<Task<*>>(onBufferOverflow = BufferOverflow.SUSPEND)
  private suspend fun <T> exec(action: suspend () -> T): T {
    val deferred = CompletableDeferred<T>()
    taskQueues.send(Task(deferred, action))
    return deferred.await()
  }

  inline fun <reified T> getOrNull(key: String) =
    store[key]?.let { Cbor.decodeFromByteArray<T>(it) }

  inline fun <reified T> get(key: String) =
    getOrNull<T>(key) ?: throw Exception("no found data for key: $key")

  suspend fun save() {
    exec {
      mm.nativeFetch(
        PureRequest(
          "file://file.std.dweb/write?path=$queryPath&create=true",
          IpcMethod.POST,
          body = IPureBody.from(Cbor.encodeToByteArray(store).let { cipher?.encrypt(it) ?: it })
        )
      )
    }
  }

  suspend inline fun <reified T> set(key: String, value: T) {
    store[key] = Cbor.encodeToByteArray(value)
    save()
  }
}