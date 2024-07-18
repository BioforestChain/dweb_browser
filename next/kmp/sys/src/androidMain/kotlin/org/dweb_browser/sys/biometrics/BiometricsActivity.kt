package org.dweb_browser.sys.biometrics

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.authenticateWithClass3Biometrics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.withMainContext
import java.util.concurrent.Executor

typealias StartAppBiometricsActivity = suspend (cls: Class<BiometricsActivity>, onIntent: (intent: Intent) -> Unit) -> Unit

class BiometricsActivity : FragmentActivity() {
  companion object {
    private val creates = mutableMapOf<String, CompletableDeferred<BiometricsActivity>>()
    suspend fun create(startAppActivity: StartAppBiometricsActivity) =
      CompletableDeferred<BiometricsActivity>().also {
        val uid = randomUUID()
        creates[uid] = it
        it.invokeOnCompletion { creates.remove(uid) }
        startAppActivity(BiometricsActivity::class.java) { intent ->
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
          intent.putExtras(Bundle().apply {
            putString("uid", uid)
          })
        }
      }.await()

    suspend fun create(mmRuntime: MicroModule.Runtime) = create { cls, onIntent ->
      mmRuntime.startAppActivity(cls, onIntent)
    }

    private const val SONY = "sony";
    private const val OPPO = "oppo";
    private const val HUAWEI = "huawei";
    private const val HONOR = "honor";
    private const val XIAOMI = "xiaomi";
    private const val VIVO = "vivo";
    private const val OnePlus = "OnePlus";
    private const val samsung = "samsung";
    private const val meizu = "meizu";
    private const val ZTE = "zte";

    /** * 获得当前手机品牌 * @return 例如：HONOR */
    private val brand = android.os.Build.BRAND.uppercase()

    /** * 对比两个字符串，并且比较字符串是否包含在其中的，并且忽略大小写 * @param value * @return */
    private fun compareTextSame(value: String) = value.uppercase().contains(brand)
    private val fingerprintComponentName by lazy {
      val pcgName = "com.android.settings"
      val clsName = when {
        compareTextSame(SONY) -> "com.android.settings.Settings\$FingerprintEnrollSuggestionActivity"
        compareTextSame(OPPO) -> "com.coloros.settings.feature.fingerprint.ColorFingerprintSettings"
        compareTextSame(HUAWEI) -> "com.android.settings.fingerprint.FingerprintSettingsActivity"
        compareTextSame(HONOR) -> "com.android.settings.fingerprint.FingerprintSettingsActivity"
        compareTextSame(XIAOMI) -> "com.android.settings.NewFingerprintActivity"
        compareTextSame(VIVO) -> "com.android.settings.Settings\$FingerpintAndFaceSettingsActivity"
        compareTextSame(OnePlus) -> "com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction"
        compareTextSame(samsung) -> "com.samsung.android.settings.biometrics.BiometricsDisclaimerActivity"
        compareTextSame(meizu) -> "com.android.settings.Settings.SecurityDashboardActivity"
        compareTextSame(ZTE) -> "com.android.settings.ChooseLockGeneric"
        else -> null
      }
      clsName?.let { className -> ComponentName(pcgName, className) }
    }

    /** * 跳转到指纹页面 或 通知用户去指纹录入 */
    fun startFingerprintActivity(startActivity: (Intent) -> Unit) {
      when (val componentName = fingerprintComponentName) {
        null -> startSettingsActivity(startActivity)
        else -> runCatching {
          val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = Intent.ACTION_VIEW
            component = componentName
          }
          startActivity(intent);
        }.getOrElse {
          startSettingsActivity(startActivity)
        }
      }
    }

    private fun startSettingsActivity(startActivity: (Intent) -> Unit) {
      val pcgNamePlace = "com.android.settings";
      val clsNamePlace = "com.android.settings.Settings";
      val intent = Intent().apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        action = Intent.ACTION_VIEW
        component = ComponentName(pcgNamePlace, clsNamePlace)
      }
      startActivity(intent);
    }
  }

  var support by mutableStateOf(false)
  var settingsRequired by mutableStateOf(false)

  private fun checkSupport() {
    val status = BiometricsManage.checkSupportBiometricsSync(this)
    when (status) {
      BiometricCheckResult.BIOMETRIC_ERROR_NONE_ENROLLED -> {
        support = true
        settingsRequired = true
      }

      BiometricCheckResult.BIOMETRIC_SUCCESS -> {
        support = true
        settingsRequired = false
        supportDeferred.complete(true)
      }

      else -> {
        support = false
        settingsRequired = false
      }
    }
  }

  private var supportDeferred = CompletableDeferred<Boolean>()
  suspend fun waitSupport() = supportDeferred.await()
  suspend fun waitSupportOrThrow(
    finishOnThrow: Boolean = true,
    cause: (Exception) -> Exception = { it },
  ) {
    supportDeferred.await().falseAlso {
      if (finishOnThrow) {
        finish()
      }
      throw cause(authenticationError ?: Exception("authenticateWithClass3Biometrics error"))
    }
  }

  override fun onResume() {
    super.onResume()
    checkSupport()
  }

  override fun onDestroy() {
    super.onDestroy()
    supportDeferred.complete(false)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ElevatedCard(Modifier.padding(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
          when {
            settingsRequired -> {
              Text("您需要设置生物识别")
              Spacer(Modifier.size(16.dp))
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ElevatedButton({
                  authenticationError = CancellationException("user cancel biometrics settings")
                  finish()
                }) {
                  Text("取消")
                }
                Spacer(Modifier.size(8.dp))
                ElevatedButton({ startFingerprintActivity { startActivity(it) } }) {
                  Text("去设置")
                }
              }
            }

            !support -> {
              Text("您的设备不支持生物识别技术")
              Spacer(Modifier.size(16.dp))
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ElevatedButton({
                  authenticationError = CancellationException("user cancel biometrics settings")
                  finish()
                }) {
                  Text("取消")
                }
              }
            }

            else -> {
              Text("生物识别技术正在保护您的隐私安全", modifier = Modifier)
            }
          }
        }
      }
    }
    intent.getStringExtra("uid")?.also {
      creates[it]?.complete(this)
    }
  }

  var authenticationError: Exception? = null
  private var authArguments: AuthArguments? = null

  private data class AuthArguments(
    val crypto: BiometricPrompt.CryptoObject?,
    val title: CharSequence,
    val negativeButtonText: CharSequence,
    val subtitle: CharSequence?,
    val description: CharSequence?,
    val confirmationRequired: Boolean,
    val executor: Executor,
    val deferred: CompletableDeferred<BiometricPrompt.AuthenticationResult>,
  )

  private val authCaller = {
    authArguments?.also { args ->
      lifecycleScope.launch {
        authenticateWithClass3BiometricsDeferred(
          crypto = args.crypto,
          title = args.title,
          negativeButtonText = args.negativeButtonText,
          subtitle = args.subtitle,
          description = args.description,
          confirmationRequired = args.confirmationRequired,
          executor = args.executor,
          deferred = args.deferred,
        )
      }
    }
  }

  suspend fun authenticateWithClass3BiometricsDeferred(
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    negativeButtonText: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor = ContextCompat.getMainExecutor(this),
    finishOnCompletion: Boolean = true,
    deferred: CompletableDeferred<BiometricPrompt.AuthenticationResult> = CompletableDeferred(),
  ): BiometricPrompt.AuthenticationResult = withMainContext {
    if (finishOnCompletion) {
      deferred.invokeOnCompletion {
        if (!isFinishing && lifecycleScope.isActive) {
          finish()
        }
      }
    }
    authArguments = AuthArguments(
      crypto,
      title,
      negativeButtonText,
      subtitle,
      description,
      confirmationRequired,
      executor,
      deferred,
    )
    authenticateWithClass3Biometrics(crypto,
      title,
      negativeButtonText,
      subtitle,
      description,
      confirmationRequired,
      executor,
      callback = object : AuthPromptCallback() {
        // 这个地方不需要resultDeferred.complete，因为失败可以进行多次尝试，不应该直接返回，而应该在多次失败之后直接error再返回
        override fun onAuthenticationFailed(activity: FragmentActivity?) {
          super.onAuthenticationFailed(activity)
        }

        override fun onAuthenticationError(
          activity: FragmentActivity?, errorCode: Int, errString: CharSequence,
        ) {
          super.onAuthenticationError(activity, errorCode, errString)
          Exception("[$errorCode] $errString").also { err ->
            authenticationError = err
            if (errorCode == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED || errorCode == 5/*已经取消*/) {
              settingsRequired = true
              authCaller()
            } else {
              deferred.completeExceptionally(err)
            }
          }
        }

        override fun onAuthenticationSucceeded(
          activity: FragmentActivity?, result: BiometricPrompt.AuthenticationResult,
        ) {
          super.onAuthenticationSucceeded(activity, result)
          deferred.complete(result)
        }
      })
    deferred.await()
  }
}
