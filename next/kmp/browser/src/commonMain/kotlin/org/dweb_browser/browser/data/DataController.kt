package org.dweb_browser.browser.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.data.render.Render
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.dwebview.ProfileName
import org.dweb_browser.dwebview.getDwebProfileStoreInstance
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager

class DataController(val storeNMM: DataNMM.DataRuntime) {
  private val dWebProfileStore = SuspendOnce { getDwebProfileStoreInstance() }
  val refreshFlow = MutableStateFlow(0f)
  fun refresh() {
    refreshFlow.value += 1
  }

  fun openRender(win: WindowController) {
    windowAdapterManager.provideRender(win.id) { modifier ->
      Render(modifier = modifier, windowRenderScope = this)
    }
  }

  sealed interface ProfileInfo {
    val profileName: ProfileName
    val mmid: MMID
  }

  @Serializable
  class ProfileDetail(override val profileName: ProfileName, val mm: IMicroModuleManifest) :
    ProfileInfo,
    IMicroModuleManifest by mm

  class ProfileBase(override val profileName: ProfileName, override val mmid: MMID) : ProfileInfo

  suspend fun loadProfileInfos() =
    dWebProfileStore().getAllProfileNames().mapNotNull { profileName ->
      profileName.mmid?.let { mmid ->
        storeNMM.bootstrapContext.dns.queryAll(mmid).firstOrNull { it.mmid == mmid }.let { mm ->
          when (mm) {
            null -> ProfileBase(profileName, mmid)
            else -> ProfileDetail(profileName, mm)
          }
        }
      }
    }

  val deleteProfileFlow = MutableStateFlow<ProfileInfo?>(null)
  var isRunningFlow = MutableStateFlow(false)
  suspend fun openDeleteDialog(info: ProfileInfo) {
    isRunningFlow.value = storeNMM.bootstrapContext.dns.isRunning(info.mmid)
    deleteProfileFlow.value = info
  }

  fun closeDeleteDialog() {
    deleteProfileFlow.value = null
    deleteJobFlow.value?.invokeOnCompletion {
      deleteJobFlow.value = null
    }
  }

  val deleteJobFlow = MutableStateFlow<Job?>(null)
  suspend fun deleteProfile(info: ProfileInfo) {
    dWebProfileStore().deleteProfile(info.profileName)
    refresh()
  }
}