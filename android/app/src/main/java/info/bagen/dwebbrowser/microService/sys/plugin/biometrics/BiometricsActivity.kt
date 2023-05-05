package info.bagen.dwebbrowser.microService.sys.plugin.biometrics

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_BIOMETRIC_ENROLL
import android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import info.bagen.dwebbrowser.microService.helper.PromiseOut
import info.bagen.dwebbrowser.microService.sys.plugin.biometrics.BiometricsController.Companion.biometricsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

data class BiometricsResult(val success:Boolean,val message:String)


class BiometricsActivity :FragmentActivity() {

    companion object {
        val biometrics_promise_out = PromiseOut<BiometricsResult>()
    }

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val BIOMETRIC_ERROR_NONE_ENROLLED_REQUEST_RESULT_CODE = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                // 没有设置识别验证（没有设置密码）
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    debugBiometrics("onAuthenticationError","errString:$errString,errorCode:$errorCode")
//                    Toast.makeText(applicationContext,
//                        "Authentication error: $errString", Toast.LENGTH_SHORT)
//                        .show()
                    biometrics_promise_out.resolve(BiometricsResult(false,errString.toString()))
                    this@BiometricsActivity.finish()
                }
                // 识别成功
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    debugBiometrics("onAuthenticationSucceeded","result:${result.authenticationType}")
                    val message  = when(result.authenticationType) {
                        AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "AUTHENTICATION_RESULT_TYPE_BIOMETRIC"
                        AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL"
                        else -> "AUTHENTICATION_RESULT_TYPE_UNKNOWN"
                    }
                    biometrics_promise_out.resolve(BiometricsResult(true,message))
                    this@BiometricsActivity.finish()
                }
                // 识别失败
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    debugBiometrics("onAuthenticationFailed","Authentication failed")
                    this@BiometricsActivity.finish()
                }
            })
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()
        biometricsController.activity = this
    }

    suspend fun biometrics() {
        withContext(Dispatchers.Main) {
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e:Throwable) {
                this@BiometricsActivity.finish()
                debugBiometrics("biometricPrompt error",e.message)
                biometrics_promise_out.resolve(BiometricsResult(false,"biometrics has been destroyed"))
            }
        }
    }

     fun chuck():Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->{
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
                // 提示用户创建您的应用接受的凭据。
                val enrollIntent = Intent(ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                }
                 startActivityForResult(enrollIntent, BIOMETRIC_ERROR_NONE_ENROLLED_REQUEST_RESULT_CODE)
                 false
            }
            else -> { false }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        debugBiometrics("onDestroy", "Biometrics onDestroy .")
        biometricsController.activity = null
        if (!biometrics_promise_out.isFinished) {
            biometrics_promise_out.resolve(BiometricsResult(false,"Authentication failed"))
        }
    }

}