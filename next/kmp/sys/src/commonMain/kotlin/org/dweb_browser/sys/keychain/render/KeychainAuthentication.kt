package org.dweb_browser.sys.keychain.render

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ContactSupport
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.twotone.Fingerprint
import androidx.compose.material.icons.twotone.Password
import androidx.compose.material.icons.twotone.Pattern
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.platform.DeviceKeyValueStore
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.helper.platform.theme.dimens
import org.dweb_browser.helper.utf8String
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.StableSmartLoad
import org.dweb_browser.sys.keychain.KeychainNMM
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict

internal expect fun getDeviceName(): String

/**
 * 获取已经注册的认证方案
 */
internal expect fun getRegisteredMethod(): KeychainMethod?
internal expect fun getSupportMethods(): List<KeychainMethod>
internal fun getCustomRegisteredMethod() =
  keychainMetadataStore.getItem(KeychainAuthentication.ROOT_KEY_METHOD)?.utf8String
    .let { KeychainMethod.ALL[it] }

internal val keychainMetadataStore = DeviceKeyValueStore(KeychainAuthentication.KEYCHAIN_METADATA)

class KeychainAuthentication(
  internal var onAuthRequestDismiss: () -> Unit = {},
  val lifecycleScope: CoroutineScope,
) : ViewModel() {
  companion object {
    internal const val KEYCHAIN_METADATA = "Dweb-Keychain-Metadata"
    internal const val ROOT_KEY_METHOD = "root-key-method"
    internal const val ROOT_KEY_TIP = "root-key-tip"
    internal const val ROOT_KEY_VERIFY = "root-key-verify"
    internal const val ROOT_KEY_VERSION = "root-key-version"
    private val supportMethods = getSupportMethods()
  }

  internal val viewModelTask = CompletableDeferred<ByteArray>().also { task ->
    task.invokeOnCompletion { throwable ->
      lifecycleScope.launch {
        if (throwable == null) {
          val nav = navControllerFlow.first()
          nav.navigate("done") {
            popUpTo("init") { inclusive = true }
          }
          delay(1000)
        }
        onAuthRequestDismiss()
      }
    }
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
    val route = when (val method = getRegisteredMethod()) {
      null -> KeychainMode.Register.mode
      else -> "${KeychainMode.Verify.mode}/${method.method}"
    }
    nav.navigate(route) {
      popUpTo("init") { inclusive = true }
    }

    viewModelTask.await()
  }

  suspend fun start(
    runtime: KeychainNMM.KeyChainRuntime,
    title: String? = null,
    subtitle: String? = null,
    description: String? = null,
  ) = start(
    title, subtitle, description,
    background = {
      BoxWithConstraints(
        modifier = Modifier.fillMaxSize().clip(RectangleShape)
          .background(LocalColorful.current.Amber.current),
        contentAlignment = Alignment.CenterEnd,
      ) {
        remember { runtime.icons.toStrict().pickLargest() }?.also { icon ->
          val size = min(maxWidth, maxHeight)
          PureImageLoader.StableSmartLoad(icon.src, size, size, hook = runtime.blobFetchHook)
            .with { img ->
              AnimatedVisibility(
                visibleState = remember {
                  MutableTransitionState(false).apply {
                    // Start the animation immediately.
                    targetState = true
                  }
                },
                enter = slideInHorizontally() + scaleIn()
              ) {
                Image(img, null, modifier = Modifier.graphicsLayer {
                  rotationY = -15f
                  translationX = maxWidth.value / 4
                })
              }
            }
        }
      }
    },
  )


  private val navControllerFlow = MutableSharedFlow<NavController>(replay = 1)
  private var title by mutableStateOf<String?>(null)
  private var subtitle by mutableStateOf<String?>(null)
  private var description by mutableStateOf<String?>(null)
  private var background by mutableStateOf<(@Composable (Modifier) -> Unit)?>(null)

  @Composable
  fun ContentRender(closeBoolean: Boolean, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    LaunchedEffect(navController) {
      navControllerFlow.emit(navController)
    }
    Column(modifier) {
      CardHeader(background = {
        if (closeBoolean) {
          IconButton(
            { onAuthRequestDismiss() },
            Modifier.padding(4.dp).size(32.dp).zIndex(10f).align(Alignment.TopEnd)
          ) {
            Icon(Icons.Rounded.Cancel, "cancel")
          }
        }
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
        val scope = rememberCoroutineScope()
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
              CardTitle("在 ${getDeviceName()} 设备上使用钥匙串管理，需要为您的设备初始化一个根密码，从而保护您的数据安全。")
              CardSubTitle(
                buildAnnotatedString {
                  val parts =
                    "⚠️ 注意：根密码不会上传到任何服务器，假如您忘记了根密码，保存在设备里的密码都将无法恢复，请务必保存好您的密码".split(
                      "根密码"
                    ).toMutableList()
                  /// 不能使用 removeFirst ，会和 android api level 35 的java冲突导致崩溃
                  /// see: https://youtrack.jetbrains.com/issue/KT-71375/Prevent-Kotlins-removeFirst-and-removeLast-from-causing-crashes-on-Android-14-and-below-after-upgrading-to-Android-API-Level-35
                  append(parts.removeAt(0))
                  parts.forEach { s ->
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                      append("根密码")
                    }
                    append(s)
                  }
                }, style = TextStyle(color = LocalColorful.current.DeepOrange.current)
              )
              CardSubTitle("请选择一种生成根密码的方式：")
              var registerMethod by remember { mutableStateOf(supportMethods.first()) }
              Column(Modifier.selectableGroup()) {
                supportMethods.forEach { method ->
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
            val viewModel = remember { VerifyPatternViewModel(scope, viewModelTask) }
            VerifyPattern(viewModel)
          }
          composable("verify/${KeychainMethod.Question.method}") {
            val viewModel = remember { VerifyQuestionViewModel(viewModelTask) }
            VerifyQuestion(viewModel)
          }
          composable("verify/${KeychainMethod.Biometrics.method}") {
            val viewModel = remember { VerifyBiometricsViewModel(viewModelTask) }
            VerifyBiometrics(viewModel)
          }
          composable("done") {
            KeychainVerified(Modifier.fillMaxWidth(), 128.dp)
          }
        }
      }
    }
  }

  @Composable
  fun Render(isDark: Boolean = isSystemInDarkTheme(), modifier: Modifier = Modifier) {
    DwebBrowserAppTheme(isDark) {
      Surface(color = Color.Transparent, modifier = modifier) {
        Box(Modifier.fillMaxSize().padding(WindowInsets.safeGestures.asPaddingValues())) {
          ElevatedCard(
            elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.dimens.small),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
              .align(Alignment.BottomCenter),
          ) {
            ContentRender(closeBoolean = true)
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
  Password("password", "文本密码", Icons.TwoTone.Password),
  Pattern("pattern", "图像密码", Icons.TwoTone.Pattern),
  Question("question", "自问答", Icons.AutoMirrored.TwoTone.ContactSupport),
  Biometrics("biometrics", "生物识别", Icons.TwoTone.Fingerprint),
  ;

  companion object {
    val ALL = entries.associateBy { it.method }
  }
}