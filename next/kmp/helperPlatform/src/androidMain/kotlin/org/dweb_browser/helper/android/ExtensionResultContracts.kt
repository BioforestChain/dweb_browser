package org.dweb_browser.helper.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper

class ExtensionResultContracts {

  open class RecordSound : ActivityResultContract<Uri, Boolean>() {
    @CallSuper
    override fun createIntent(context: Context, input: Uri): Intent {
      return Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
        .putExtra(MediaStore.EXTRA_OUTPUT, input)
    }

    final override fun getSynchronousResult(
      context: Context,
      input: Uri
    ): SynchronousResult<Boolean>? = null

    @Suppress("AutoBoxing")
    final override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
      return resultCode == Activity.RESULT_OK
    }
  }

}