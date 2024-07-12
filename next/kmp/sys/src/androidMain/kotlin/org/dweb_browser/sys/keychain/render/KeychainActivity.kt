package org.dweb_browser.sys.keychain.render

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ContactSupport
import androidx.compose.material.icons.twotone.Password
import androidx.compose.material.icons.twotone.Pattern
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.helper.platform.theme.dimens
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.keychain.deviceKeyStore

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

    val registeredMethod
      get() = deviceKeyStore.getItem("root-key-method")?.let { KeychainMethod.ALL[it] }
    val isRegistered
      get() = registeredMethod != null
  }


  private val viewModelTask = CompletableDeferred<ByteArray>()

  override fun onDestroy() {
    super.onDestroy()
    viewModelTask.completeExceptionally(CancellationException("User cancel"))
  }

  suspend fun start(
    title: String? = null,
    subtitle: String? = null,
    description: String? = null,
    background: (@Composable (Modifier) -> Unit)? = null,
  ): ByteArray = withMainContext {/// nav 相关的操作需要在主线程
    this.title = title
    this.subtitle = subtitle
    this.description = description
    this.background = background

    println("QAQ KeychainActivity start")
    val nav = navControllerFlow.first()
    val route = when (val method = registeredMethod) {
      null -> KeychainMode.Register.mode
      else -> "${KeychainMode.Verify.mode}/${method.method}"
    }
    nav.navigate(route) {
      popUpTo("init") { inclusive = true }
    }

    viewModelTask.await().also {
      lifecycleScope.launch {
        nav.navigate("done") {
          popUpTo("init") { inclusive = true }
        }
        delay(1000)
        finish()
      }
    }
  }

  private val navControllerFlow = MutableSharedFlow<NavController>(replay = 1)
  private var title by mutableStateOf<String?>(null)
  private var subtitle by mutableStateOf<String?>(null)
  private var description by mutableStateOf<String?>(null)
  private var background by mutableStateOf<(@Composable (Modifier) -> Unit)?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent.getStringExtra("uid")?.also {
      creates[it]?.complete(this)
    }
    setContent {
      Box(Modifier.fillMaxSize().padding(WindowInsets.safeGestures.asPaddingValues())) {
        ElevatedCard(
          elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.dimens.small),
          modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            .align(Alignment.BottomCenter),
        ) {
          val navController = rememberNavController()
          LaunchedEffect(navController) {
            navControllerFlow.emit(navController)
          }
          CardHeader(background = {
            background?.also {
              it(Modifier.fillMaxSize().zIndex(-1f))
            }
          }) {
            Column(Modifier.padding(16.dp)) {
              title?.also { CardHeaderTitle(it) }
              subtitle?.also { CardHeaderSubtitle(it) }
              description?.also { CardHeaderDescription(it) }
            }
          }
          Column(
            Modifier.padding(16.dp).animateContentSize().sizeIn(minHeight = 220.dp),
            verticalArrangement = Arrangement.Center
          ) {
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
                  CardSubTitle(
                    buildAnnotatedString {
                      val parts =
                        "⚠️ 注意：根密码不会上传到任何服务器，假如您忘记了根密码，保存在设备里的密码都将无法恢复，请务必保存好您的密码".split(
                          "根密码"
                        ).toMutableList()
                      append(parts.removeFirst())
                      parts.forEach { s ->
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                          append("根密码")
                        }
                        append(s)
                      }
                    }, style = TextStyle(color = LocalColorful.current.DeepOrange.current)
                  )
                  CardSubTitle("请选择一种生成根密码的方式：")
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
                val viewModel = remember { RegisterPasswordViewModel(viewModelTask) }
                RegisterPassword(viewModel)
              }
              composable("register/${KeychainMethod.Pattern.method}") {
                val viewModel = remember { RegisterPatternViewModel(viewModelTask) }
                RegisterPattern(viewModel)
              }
              composable("register/${KeychainMethod.Question.method}") {
                val viewModel = remember { RegisterQuestionViewModel(viewModelTask) }
                RegisterQuestion(viewModel)
              }
              composable("verify/${KeychainMethod.Password.method}") {
                val viewModel = remember { VerifyPasswordViewModel(viewModelTask) }
                VerifyPassword(viewModel)
              }
              composable("verify/${KeychainMethod.Pattern.method}") {
                val viewModel = remember { VerifyPatternViewModel(viewModelTask) }
                VerifyPattern(viewModel)
              }
              composable("verify/${KeychainMethod.Question.method}") {
                val viewModel = remember { VerifyQuestionViewModel(viewModelTask) }
                VerifyQuestion(viewModel)
              }
              composable("done") {
                KeychainVerified(Modifier.fillMaxWidth(), 128.dp)
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
