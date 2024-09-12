package org.dweb_browser.sys.shareReceiver.render

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import org.dweb_browser.sys.shareReceiver.debugShareReceiver

class ShareReceiverActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Intent.ACTION_SEND == intent.action) {
      debugShareReceiver("ShareReceiverActivity", "${intent.action} ${intent.type}")

      handleReceiver(intent, intent.type!!)
    } else if (Intent.ACTION_SEND_MULTIPLE == intent.action) {
      debugShareReceiver("ShareReceiverActivity", "${intent.action} ${intent.type}")

      handleReceiverMultiple(intent)

    }


    // TODO: 还需要使用ContentResolver对接收到的Uri进行处理
    // TODO: 创建视图用于对dweb应用或内部应用进行分享

    finish()
  }

  private fun handleReceiver(intent: Intent, intentType: String) {
    when {
      intentType == "text/html" -> intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
      intentType.startsWith("text/") -> intent.getStringExtra(Intent.EXTRA_TEXT)
      intent.hasExtra(Intent.EXTRA_STREAM) -> intent.getParcelable<Uri>(Intent.EXTRA_STREAM)
      else -> {}
    }
  }

  private fun handleReceiverMultiple(intent: Intent) {
    val fileList = mutableListOf<String>()
    var textSummary = ""

    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
      @Suppress("DEPRECATION")
      when (val extraStream = intent.extras?.get(Intent.EXTRA_STREAM)) {
        is ArrayList<*> -> (extraStream as ArrayList<Uri>).let {
          it.forEach { uri ->
            fileList.add(uri.toString())
          }
        }

        is Uri -> extraStream.let {
          fileList.add(it.toString())
        }

        else -> {}
      }
    }

    if (intent.dataString != null) {
      fileList.add(intent.dataString!!)
    }

    if (intent.hasExtra(Intent.EXTRA_TITLE)) {
      val title = intent.getStringExtra(Intent.EXTRA_TITLE)
      textSummary += "$title \n"
    }

    if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
      val subtitle = intent.getStringExtra(Intent.EXTRA_SUBJECT)
      textSummary += "$subtitle \n"
    }

    if (intent.hasExtra(Intent.EXTRA_TEXT)) {
      @Suppress("DEPRECATION")
      when (val extraText = intent.extras?.get(Intent.EXTRA_TEXT)) {
        is ArrayList<*> -> (extraText as ArrayList<String>).let {
          it.forEach { text ->
            textSummary += "$text \n"
          }
        }

        is String -> extraText.let {
          textSummary += intent.getStringExtra(Intent.EXTRA_TEXT)
        }
      }
    }
  }
}

inline fun <reified T : Parcelable> Intent.getParcelable(key: String): T? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableExtra(key, T::class.java)
  } else {
    @Suppress("DEPRECATION") getParcelableExtra(key)
  }
}

inline fun <reified T : Parcelable> Intent.getParcelableArrayList(key: String): ArrayList<T>? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableArrayListExtra(key, T::class.java)
  } else {
    @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
  }
}