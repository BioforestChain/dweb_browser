package org.dweb_browser.core.ipc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.randomUUID

val debugIpcPool = Debugger("ipcPool")

val kotlinIpcPool = IpcPool()

/**
 * IpcPool跟上下文对应，跟body流对应
 * Context(Kotlin,Worker,Front)
 * */
open class IpcPool {
  companion object {
    private fun randomPoolId() = "kotlin-${randomUUID()}"
  }

  val scope = globalDefaultScope + CoroutineName("ipc-pool-kotlin")

  /**每一个ipcPool都会绑定一个body流池,只有当不在同一个IpcPool的时候才需要互相拉取*/
  val poolId: UUID = randomPoolId()

  override fun toString() = "IpcPool@poolId=$poolId<uid=$poolId>"

  /**
   * 所有的ipc对象实例集合
   */
  private val ipcSet = SafeHashSet<Ipc>()

  suspend fun createIpc(
    endpoint: IpcEndpoint,
    pid: Int,
    locale: MicroModuleManifest,
    remote: MicroModuleManifest,
    autoStart: Boolean = false,
    startReason: String? = null,
  ) = Ipc(
    pid = pid,
    endpoint = endpoint,
    locale = locale,
    remote = remote,
    pool = this,
  ).also { ipc ->
    safeCreatedIpc(ipc, autoStart, startReason)
  }

  internal suspend fun safeCreatedIpc(
    ipc: Ipc,
    autoStart: Boolean,
    startReason: String?,
  ) {
    /// 保存ipc，并且根据它的生命周期做自动删除
    debugIpcPool("createIpc") {
      "ipc=$ipc start=$autoStart reason=$startReason"
    }
    ipcSet.add(ipc)
    /// 自动启动
    if (autoStart) {
      ipc.start(await = false, reason = startReason ?: "autoStart")
    }
    ipc.onClosed {
      ipcSet.remove(ipc)
      debugIpcPool("removeIpc", ipc)
    }
  }

  /**关闭信号*/
  suspend fun awaitDestroyed() = runCatching {
    scope.coroutineContext[Job]!!.join();
    null
  }.getOrElse { it }

  val isDestroyed get() = scope.coroutineContext[Job]!!.isCancelled

  val destroy = SuspendOnce1 { cause: CancellationException ->
    val oldSet = ipcSet.toSet()
    ipcSet.clear()
    oldSet.forEach { ipc ->
      ipc.close()
    }
    scope.cancel(cause)
  }

}