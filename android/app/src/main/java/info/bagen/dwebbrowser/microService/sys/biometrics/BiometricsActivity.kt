package info.bagen.dwebbrowser.microService.sys.biometrics

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_BIOMETRIC_ENROLL
import android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import info.bagen.dwebbrowser.microService.helper.PromiseOut
import info.bagen.dwebbrowser.microService.sys.biometrics.BiometricsController.Companion.biometricsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

data class BiometricsResult(val success: Boolean, val message: String)


class BiometricsActivity : FragmentActivity() {

  companion object {
    val biometrics_promise_out = PromiseOut<BiometricsResult>()
  }

  private lateinit var executor: Executor
  private lateinit var biometricPrompt: BiometricPrompt
  private lateinit var promptInfo: BiometricPrompt.PromptInfo

  private val mBiometricLaunch =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      /*if (it.resultCode == RESULT_OK) {
      }*/
    }
  private var mBiometricsData: BiometricsData? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mBiometricsData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getSerializableExtra("data", BiometricsData::class.java)
    } else {
      @Suppress("DEPRECATION") intent.getSerializableExtra("data")?.let {
        it as BiometricsData
      }
    }
    executor = ContextCompat.getMainExecutor(this)
    biometricPrompt = BiometricPrompt(this, executor,
      object : BiometricPrompt.AuthenticationCallback() {
        // 没有设置识别验证（没有设置密码）
        override fun onAuthenticationError(
          errorCode: Int,
          errString: CharSequence
        ) {
          super.onAuthenticationError(errorCode, errString)
          debugBiometrics("onAuthenticationError", "errString:$errString,errorCode:$errorCode")
//                    Toast.makeText(applicationContext,
//                        "Authentication error: $errString", Toast.LENGTH_SHORT)
//                        .show()
          biometrics_promise_out.resolve(BiometricsResult(false, errString.toString()))
          this@BiometricsActivity.finish()
        }

        // 识别成功
        override fun onAuthenticationSucceeded(
          result: BiometricPrompt.AuthenticationResult
        ) {
          super.onAuthenticationSucceeded(result)
          val message = when (result.authenticationType) {
            AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "AUTHENTICATION_RESULT_TYPE_BIOMETRIC"
            AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL"
            else -> "AUTHENTICATION_RESULT_TYPE_UNKNOWN"
          }
          debugBiometrics(
            "onAuthenticationSucceeded",
            "result:${result.authenticationType}, msg:$message"
          )
          biometrics_promise_out.resolve(BiometricsResult(true, message))
          this@BiometricsActivity.finish()
        }

        // 识别失败
        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          debugBiometrics("onAuthenticationFailed", "Authentication failed")
          this@BiometricsActivity.finish()
        }
      })
    /**
     * setAllowedAuthenticators 单独设置 BIOMETRIC_STRONG/BIOMETRIC_WEAK 必须再设置 setNegativeButtonText
     * setAllowedAuthenticators 设置包含 DEVICE_CREDENTIAL               不能再设置 setNegativeButtonText
     *
     * BIOMETRIC_STRONG   3 类生物识别技术进行身份验证，简单来说就是支持加密密钥使用。
     * BIOMETRIC_WEAK     2 类生物识别技术进行身份验证，简单来说就是普通的验证，不需要进行加密。
     * DEVICE_CREDENTIAL 密码，图形，PIN
     *
     */
    promptInfo = BiometricPrompt.PromptInfo.Builder().also {
      it.setTitle(mBiometricsData?.title ?: "Biometric login for my app")
      it.setSubtitle(mBiometricsData?.subtitle ?: "Log in using your biometric credential")
      it.setDescription(mBiometricsData?.description ?: "Biometric description")
      if (mBiometricsData?.useFallback == true) {
        it.setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
      } else {
        it.setAllowedAuthenticators(BIOMETRIC_WEAK)
        it.setNegativeButtonText(mBiometricsData?.negativeButtonText ?: "Cancel")
      }
    }.build()
    biometricsController.activity = this
    // biometricPrompt.authenticate(promptInfo)
  }

  suspend fun biometrics() {
    withContext(Dispatchers.Main) {
      try {
        biometricPrompt.authenticate(promptInfo)
      } catch (e: Throwable) {
        this@BiometricsActivity.finish()
        debugBiometrics("biometricPrompt error", e.message)
        biometrics_promise_out.resolve(BiometricsResult(false, "biometrics has been destroyed"))
      }
    }
  }

  fun chuck(): Boolean {
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
      val biometricManager = BiometricManager.from(this)
      when (val rType = biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
          debugBiometrics("MY_APP_TAG", "应用程序可以使用生物识别技术进行身份验证.")
          true
        }

        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
          debugBiometrics("MY_APP_TAG", "此设备上没有可用的生物识别功能.")
          false
        }

        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
          debugBiometrics("MY_APP_TAG", "生物识别功能目前不可用.")
          false
        }

        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
          debugBiometrics("MY_APP_TAG", "请设置锁屏密码，并同步开启生物识别功能.")
          // 提示用户创建您的应用接受的凭据。
          mBiometricLaunch.launch(Intent(ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
              EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_WEAK
            )
          })
          false
        }

        else -> {
          debugBiometrics("MY_APP_TAG", "未处理的异常 -> $rType.")
          false
        }
      }
    } else { // Build.VERSION.SDK_INT > Build.VERSION_CODES.M
      val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
      keyguardManager.isDeviceSecure
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    debugBiometrics("onDestroy", "Biometrics onDestroy .")
    biometricsController.activity = null
    if (!biometrics_promise_out.isFinished) {
      biometrics_promise_out.resolve(BiometricsResult(false, "Authentication failed"))
    }
  }

}