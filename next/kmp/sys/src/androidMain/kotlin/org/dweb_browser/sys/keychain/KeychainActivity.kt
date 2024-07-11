package org.dweb_browser.sys.keychain

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ContactSupport
import androidx.compose.material.icons.twotone.Password
import androidx.compose.material.icons.twotone.Pattern
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.crypto.hash.jvmSha256

class KeychainActivity : ComponentActivity() {
  companion object {
    private val creates = mutableMapOf<String, CompletableDeferred<KeychainActivity>>()
    suspend fun create(runtime: MicroModule.Runtime): KeychainActivity {
      val uid = randomUUID()
      val deferred = CompletableDeferred<KeychainActivity>()
      creates[uid] = deferred
      deferred.invokeOnCompletion { creates.remove(uid) }

      runtime.startAppActivity(KeychainActivity::class.java) { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("uid", uid)
      }
      return deferred.await()
    }

    val isRegistered
      get() = deviceKeyStore.getItem("root-key-method")?.let { KeychainMethod.ALL[it] } != null
  }

  private var startMode by mutableStateOf<Pair<KeychainMode, CompletableDeferred<ByteArray>>?>(null)

  suspend fun start(): ByteArray {
    val task = CompletableDeferred<ByteArray>()
    if (isRegistered) {
      startMode = KeychainMode.Verify to task
    } else {
      startMode = KeychainMode.Register to task
    }
    return task.await().also { finish() }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent.getStringExtra("uid")?.also {
      creates[it]?.complete(this)
    }
    setContent {
      Box(Modifier.fillMaxSize().padding(WindowInsets.safeGestures.asPaddingValues())) {
        ElevatedCard(
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          modifier = Modifier.padding(24.dp).align(Alignment.BottomCenter),
        ) {
          val navController = rememberNavController()
          var modeTask by remember { mutableStateOf<CompletableDeferred<ByteArray>?>(null); }
          LaunchedEffect(startMode) {
            startMode?.also { (mode, task) ->
              navController.navigate(mode.mode)
              modeTask?.completeExceptionally(CancellationException())
              modeTask = task
            }
          }
          DisposableEffect(modeTask) {
            onDispose {
              modeTask?.completeExceptionally(CancellationException())
            }
          }

          Column(Modifier.padding(16.dp).animateContentSize()) {
            NavHost(navController, "init") {
              composable("init") {
                Text(
                  "Loading...",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.alpha(0.8f)
                )
              }
              composable("register") {
                Column {
                  CardTitle("在${Build.BRAND}(power by Android) 设备上使用钥匙串管理，需要为您的设备初始化一个根密码，从而保护您的数据安全。")
                  CardSubTitle("请选择一种生成根密码的方式")
                  var registerMethod by remember { mutableStateOf(KeychainMethod.Password) }
                  Column(Modifier.selectableGroup()) {
                    KeychainMethod.entries.forEach { method ->
                      val selected = registerMethod == method
                      Row(
                        Modifier.fillMaxWidth().height(56.dp).selectable(
                          selected = selected,
                          onClick = { registerMethod = method },
                          role = Role.RadioButton
                        ).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        RadioButton(
                          selected = selected,
                          onClick = null // null recommended for accessibility with screenreaders
                        )
                        Text(
                          text = method.label,
                          style = MaterialTheme.typography.bodyLarge,
                          modifier = Modifier.padding(start = 16.dp)
                        )
                      }
                    }
                  }

                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton({
                      navController.navigate("register/${registerMethod.method}")
                    }) {
                      Text("下一步")
                    }
                  }
                }
              }
              composable("register/${KeychainMethod.Password.method}") {
                modeTask?.also { modeTask ->
                  val viewModel = remember(modeTask) { RegisterPasswordViewModel(modeTask) }
                  RegisterPassword(viewModel)
                }
              }
              composable("register/${KeychainMethod.Pattern.method}") {
                modeTask?.also { modeTask ->
                  val viewModel = remember(modeTask) { RegisterPatternViewModel(modeTask) }
                  RegisterPattern(viewModel)
                }
              }
              composable("register/${KeychainMethod.Question.method}") {
                modeTask?.also { modeTask ->
                  val viewModel = remember(modeTask) { RegisterQuestionViewModel(modeTask) }
                  RegisterQuestion(viewModel)
                }
              }
            }
          }
        }
      }
    }
  }

}

enum class KeychainMode(val mode: String) {
  Register("register"), Verify("verify"),
  ;
}

enum class KeychainMethod(val method: String, val label: String, val icon: ImageVector) {
  Password("password", "文本密码", Icons.TwoTone.Password), Pattern(
    "pattern", "图像密码", Icons.TwoTone.Pattern
  ),
  Question("question", "自问答", Icons.AutoMirrored.TwoTone.ContactSupport),
  ;

  companion object {
    val ALL = entries.associateBy { it.method }
  }
}


sealed interface ViewModelTask {
  val method: KeychainMethod
  val task: CompletableDeferred<ByteArray>
  fun finish(): Boolean
}

abstract class RegisterViewModelTask : ViewModelTask {
  protected abstract fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray
  override fun finish(): Boolean {
    if (task.isCompleted) {
      return true
    }
    deviceKeyStore.setItem("root-key-method", method.method)
    val keyRawData = doFinish { keyTip ->
      deviceKeyStore.setRawItem("root-key-tip".utf8Binary, keyTip)
    }
    deviceKeyStore.setRawItem("root-key-verify".utf8Binary, jvmSha256(keyRawData))
    task.complete(keyRawData)
    return true
  }
}

abstract class VerifyViewModelTask : ViewModelTask {
  abstract fun keyTipCallback(keyTip: ByteArray?)

  private val keyVerify = deviceKeyStore.getRawItem("root-key-verify".utf8Binary)
    ?: throw Exception("no found root-key verify")

  init {
    deviceKeyStore.getItem("root-key-method").also { m ->
      if (m != method.method) {
        throw Exception("invalid root-key method, expect=${method.method} actual=$m")
      }
    }
    deviceKeyStore.getRawItem("root-key-tip".utf8Binary).also {
      keyTipCallback(it)
    }
  }

  protected abstract fun doFinish(): ByteArray
  override fun finish(): Boolean {
    if (task.isCompleted) {
      return true
    }
    val keyRawData = doFinish()
    return jvmSha256(keyRawData).contentEquals(keyVerify).trueAlso {
      task.complete(keyRawData)
    }
  }
}

@Composable
internal fun CardTitle(text: String, modifier: Modifier = Modifier, style: TextStyle? = null) {
  Text(
    text,
    style = MaterialTheme.typography.titleMedium.merge(style),
    modifier = modifier.padding(bottom = 8.dp)
  )
}

@Composable
internal fun CardSubTitle(text: String, modifier: Modifier = Modifier, style: TextStyle? = null) {
  Text(
    text,
    style = MaterialTheme.typography.labelMedium.merge(style),
    modifier = modifier.padding(bottom = 8.dp)
  )
}