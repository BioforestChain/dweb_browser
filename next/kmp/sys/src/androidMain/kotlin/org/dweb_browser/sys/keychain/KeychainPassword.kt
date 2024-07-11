package org.dweb_browser.sys.keychain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.crypto.hash.jvmSha256

@Composable
fun RegisterPassword(viewModel: RegisterPasswordViewModel, modifier: Modifier = Modifier) {
  Column {
    PasswordTextField(
      viewModel.password,
      { viewModel.password = it },
      label = { Text("密码") },
      placeholder = { Text("请输入您的密码") },
    )
    PasswordTextField(
      viewModel.password,
      { viewModel.password = it },
      label = { Text("确认密码") },
      placeholder = { Text("请再次输入您的密码") },
    )
    TextField(
      viewModel.tip,
      { viewModel.tip = it },
      label = { Text("密码提示") },
      placeholder = { Text("请输入密码提示词辅助记忆") },
    )
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
) {
  var viewPassword by remember { mutableStateOf(false) }
  TextField(
    value = value,
    onValueChange = onValueChange,
    visualTransformation = if (viewPassword) VisualTransformation.None else PasswordVisualTransformation(),
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = {
      IconButton({ viewPassword = !viewPassword }) {
        Icon(
          if (viewPassword) Icons.TwoTone.Visibility else Icons.TwoTone.VisibilityOff,
          contentDescription = if (viewPassword) "click to hide password" else "click to view password"
        )
      }
    },
    prefix = prefix,
    suffix = suffix,
    supportingText = supportingText,
  )
}


class RegisterPasswordViewModel(override val task: CompletableDeferred<ByteArray>) :
  RegisterViewModelTask() {
  override val method = KeychainMethod.Password
  var password by mutableStateOf("")
  var tip by mutableStateOf("")
  override fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray {
    keyTipCallback(tip.trim().utf8Binary)
    return jvmSha256(password.utf8Binary)
  }
}

@Composable
fun VerifyPassword(viewModel: VerifyPasswordViewModel, modifier: Modifier = Modifier) {
  Column {
    PasswordTextField(
      viewModel.password,
      { viewModel.password = it },
      label = { Text("密码") },
      placeholder = { Text("请输入您的密码") },
    )
    if (viewModel.tip.isNotEmpty()) Column(
      Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("密码提示", style = MaterialTheme.typography.titleSmall)
      Text(viewModel.tip, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

class VerifyPasswordViewModel(
  override val task: CompletableDeferred<ByteArray>,
) : VerifyViewModelTask() {
  override val method = KeychainMethod.Password

  var password by mutableStateOf("")
  lateinit var tip: String
    private set

  override fun keyTipCallback(keyTip: ByteArray?) {
    tip = keyTip?.utf8String ?: ""
  }

  override fun doFinish(): ByteArray {
    return jvmSha256(password.utf8Binary)
  }
}