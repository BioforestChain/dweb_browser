package org.dweb_browser.browser.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.browser.data.render.Render
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.ProfileName
import org.dweb_browser.dwebview.getDwebProfileStoreInstance
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager

class DataController(val storeNMM: DataNMM.DataRuntime) {
  val dWebProfileStore = getDwebProfileStoreInstance()
  val refreshFlow = MutableStateFlow(0f)
  fun refresh() {
    refreshFlow.value += 1
  }

  val profileDetailFlow = MutableStateFlow<ProfileDetail?>(null)
  fun goToDetail(profileDetail: ProfileDetail) {
    profileDetailFlow.value = profileDetail
  }

  fun backToList() {
    profileDetailFlow.value = null
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

  class ProfileDetail(override val profileName: ProfileName, val mm: MicroModule) : ProfileInfo,
    IMicroModuleManifest by mm

  class ProfileBase(override val profileName: ProfileName, override val mmid: MMID) : ProfileInfo

  suspend fun loadProfileInfos() = dWebProfileStore.getAllProfileNames().mapNotNull { profileName ->
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
  }

  val deleteJobFlow = MutableStateFlow<Job?>(null)
  suspend fun deleteProfile(info: ProfileInfo) {
    dWebProfileStore.deleteProfile(info.profileName)
    refresh()
//    if (deleteProfileDetailFlow.value == detail) {
//      backToList()
//    }
  }
}