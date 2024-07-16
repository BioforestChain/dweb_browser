package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.animation.headShake
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.crypto.hash.sha256Sync

@Composable
fun RegisterPassword(viewModel: RegisterPasswordViewModel, modifier: Modifier = Modifier) {
  Column {
    CardTitle("请设置您的密码")
    Column(
      Modifier.padding(8.dp).clip(
        MaterialTheme.shapes.medium.copy(
          bottomStart = CornerSize(0),
          bottomEnd = CornerSize(0)
        )
      )
    ) {
      PasswordTextField(
        viewModel.firstPassword,
        { viewModel.firstPassword = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("密码") },
        placeholder = { Text("请输入您的密码") },
        shape = RectangleShape,
      )
      PasswordTextField(
        viewModel.secondPassword,
        { viewModel.secondPassword = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("确认密码") },
        placeholder = { Text("请再次输入您的密码") },
        shape = RectangleShape,
      )
      TextField(
        viewModel.tip,
        { viewModel.tip = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("密码提示") },
        placeholder = { Text("请输入密码提示词辅助记忆") },
        shape = RectangleShape,
      )
      if (viewModel.firstPassword.isNotEmpty()) {
        val score =
          remember(viewModel.firstPassword) { PasswordSafeLevel.calcScore(viewModel.firstPassword) }
        val level = remember(score) {
          PasswordSafeLevel.entries.find { it.level(score) } ?: PasswordSafeLevel.Weak
        }
        val color = level.color(LocalColorful.current)

        LinearProgressIndicator(
          progress = { score / 100f },
          modifier = Modifier.fillMaxWidth(),
          color = color,
        )
        level.brand(color)
      }
    }
    CardActions(Modifier.align(Alignment.End)) {
      val scope = rememberCoroutineScope()
      FilledTonalButton({
        scope.launch {
          viewModel.confirm()
        }
      }, enabled = viewModel.canConfirm && !viewModel.registering) {
        Text("确定")
      }
    }
  }
}

@Composable
private fun PasswordTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  prefix: @Composable (() -> Unit)? = null,
  suffix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null,
  isError: Boolean = false,
  shape: Shape = TextFieldDefaults.shape,
  colors: TextFieldColors = TextFieldDefaults.colors(),
) {
  var viewPassword by remember { mutableStateOf(false) }
  TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    visualTransformation = if (viewPassword) VisualTransformation.None else PasswordVisualTransformation(),
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = {
      IconButton({ viewPassword = !viewPassword }) {
        Icon(
          if (viewPassword) Icons.TwoTone.VisibilityOff else Icons.TwoTone.Visibility,
          contentDescription = if (viewPassword) "click to hide password" else "click to view password"
        )
      }
    },
    prefix = prefix,
    suffix = suffix,
    supportingText = supportingText,
    isError = isError,
    shape = shape,
    colors = colors,
  )
}


class RegisterPasswordViewModel(override val task: CompletableDeferred<ByteArray>) :
  RegisterViewModelTask(KeychainMethod.Password) {
  var firstPassword by mutableStateOf("")
  var secondPassword by mutableStateOf("")
  var tip by mutableStateOf("")
  val canConfirm get() = firstPassword.isNotEmpty() && firstPassword == secondPassword
  suspend fun confirm() {
    finish()
  }

  override fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray {
    keyTipCallback(tip.trim().utf8Binary)
    return sha256Sync(firstPassword.utf8Binary)
  }
}

@Composable
fun VerifyPassword(viewModel: VerifyPasswordViewModel, modifier: Modifier = Modifier) {
  Column {
    CardTitle("验证密码")
    var isError by remember { mutableStateOf(false) }
    Column(Modifier.padding(8.dp).headShake(isError) { isError = false }) {
      PasswordTextField(
        viewModel.password,
        { viewModel.password = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("密码") },
        placeholder = { Text("请输入您的密码") },
        isError = isError
      )
      if (viewModel.tip.isNotEmpty()) Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        CardSubTitle("密码提示")
        CardDescription(viewModel.tip)
      }
    }
    CardActions(Modifier.align(Alignment.End)) {
      val scope = rememberCoroutineScope()
      Button(
        onClick = {
          scope.launch {
            isError = !viewModel.confirm()
          }
        },
        enabled = viewModel.canConfirm && !viewModel.verifying,
      ) {
        Text("确定")
      }
    }
  }
}

class VerifyPasswordViewModel(
  override val task: CompletableDeferred<ByteArray>,
) : VerifyViewModelTask(KeychainMethod.Password) {

  var password by mutableStateOf("")
  lateinit var tip: String
    private set

  override fun keyTipCallback(keyTip: ByteArray?) {
    tip = keyTip?.utf8String ?: ""
  }

  override fun doFinish(): ByteArray {
    return sha256Sync(password.utf8Binary)
  }

  val canConfirm get() = password.isNotEmpty()

  suspend fun confirm(): Boolean {
    return finish()
  }
}