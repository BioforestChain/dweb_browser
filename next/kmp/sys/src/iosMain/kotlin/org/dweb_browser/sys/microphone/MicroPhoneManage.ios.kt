package org.dweb_browser.sys.microphone

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted

actual class MicroPhoneManage {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.MICROPHONE) {
        microphoneAuthorizationStatus()
      } else null
    }
  }

  private suspend fun microphoneAuthorizationStatus() : AuthorizationStatus {
    val status = when (AVAudioApplication.sharedInstance.recordPermission) {
      AVAudioApplicationRecordPermissionDenied -> AuthorizationStatus.DENIED
      AVAudioApplicationRecordPermissionGranted -> AuthorizationStatus.GRANTED
      else -> {
        val result = CompletableDeferred<AuthorizationStatus>()
        AVAudioApplication.requestRecordPermissionWithCompletionHandler { success ->
          if (success) {
            result.complete(AuthorizationStatus.GRANTED)
          } else {
            result.complete(AuthorizationStatus.DENIED)
          }
        }
        return result.await()
      }
    }
    return status
  }

  actual suspend fun recordSound(microModule: MicroModule): String {
    WARNING("Not yet Implements recordSound")
    return ""

//    val microPhone = MicroPhoneHandler()
//    return microPhone.recordPath
  }
}