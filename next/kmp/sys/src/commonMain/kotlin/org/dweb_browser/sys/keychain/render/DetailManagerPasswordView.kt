package org.dweb_browser.sys.keychain.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.base64Binary
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.base64UrlBinary
import org.dweb_browser.helper.base64UrlString
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.helper.hexBinary
import org.dweb_browser.helper.hexString
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.sys.keychain.KeychainI18nResource
import org.dweb_browser.sys.keychain.KeychainManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KeychainManager.DetailManager.KeyManager.PasswordView(
  passwordSource: ByteArray,
  rwMode: PasswordReadWriteMode,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    PrimaryTabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
      PasswordViewMode.entries.forEachIndexed { tabIndex, tab ->
        Tab(
          selected = selectedTabIndex == tabIndex,
          onClick = {
            selectedTabIndex = tabIndex
          },
        ) {
          Text(tab.label(), modifier = Modifier.padding(8.dp))
        }
      }
    }

    @Composable
    fun PasswordTextValue(
      value: ByteArray,
      onValueChange: (ByteArray, String) -> Unit,
      decoder: (ByteArray) -> String,
      encoder: (String) -> ByteArray,
    ) {
      var decoderError by remember { mutableStateOf(false) }
      var encoderError by remember { mutableStateOf(false) }
      val decodedValue = remember(value, decoder) {
        runCatching {
          decoderError = false
          decoder(value).also { onValueChange(value, it) }
        }.getOrElse {
          decoderError = true
          ""
        }
      }
      var text by remember(decodedValue) { mutableStateOf(decodedValue) }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
          text,
          { textValue ->
            text = textValue
            val encodedValue = runCatching {
              encoderError = false
              encoder(textValue)
            }.getOrElse {
              encoderError = true
              null
            }
            if (encodedValue != null) {
              onValueChange(encodedValue, text)
            }
          },
          readOnly = rwMode == PasswordReadWriteMode.Readonly,
          modifier = Modifier.fillMaxWidth(),
          isError = decoderError || encoderError
        )
        if (decoderError) {
          Text(
            KeychainI18nResource.password_decode_error(),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
          )
        } else if (encoderError) {
          Text(
            KeychainI18nResource.password_encode_error(),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
          )
        }
      }
    }

    Box(Modifier.weight(1f, false).padding(8.dp)) {
      var passwordBinaryValue by remember { mutableStateOf(passwordSource) }
      var passwordTextValue by remember { mutableStateOf("") }
      var urlMode by remember { mutableStateOf(false) }
      var hexMode by remember { mutableStateOf(true) }
      val scope = rememberCoroutineScope()

      @Composable
      fun RowScope.PasswordActions() {
        when (rwMode) {
          PasswordReadWriteMode.Readonly -> {
            val success = remember { Animatable(0f) }
            val clipboardManager = LocalClipboardManager.current
            Button(
              {
                clipboardManager.setText(AnnotatedString(passwordTextValue))
                scope.launch {
                  success.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                  delay(1000)
                  success.animateTo(0f)
                }
              },
              modifier = Modifier.hoverCursor(),
              contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.TwoTone.ContentCopy, "copy", Modifier.alpha(1f - success.value))
                Icon(Icons.TwoTone.Check, "success", Modifier.graphicsLayer {
                  alpha = success.value
                  scaleX = success.value
                  scaleY = success.value
                })
              }
              Text(KeychainI18nResource.password_copy())
            }
          }

          PasswordReadWriteMode.ReadWrite -> {
            val success = remember { Animatable(0f) }
            var saving by remember { mutableStateOf(false) }
            hasModify = !passwordBinaryValue.contentEquals(passwordSource)
            Button(
              {
                scope.launch {
                  saving = true
                  runCatching {
                    updatePassword(key, passwordBinaryValue)
                    success.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                    delay(1000)
                    success.animateTo(0f)
                  }
                  saving = false
                }
              },
              contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
              enabled = !saving && hasModify,
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.TwoTone.Save, "save", Modifier.alpha(1f - success.value))
                Icon(Icons.TwoTone.Check, "success", Modifier.graphicsLayer {
                  alpha = success.value
                  scaleX = success.value
                  scaleY = success.value
                })
              }

              Text(KeychainI18nResource.password_save())
            }
          }
        }
      }
      when (val passwordViewMode = PasswordViewMode.entries[selectedTabIndex]) {
        PasswordViewMode.Utf8 -> Column {
          PasswordTextValue(value = passwordBinaryValue,
            onValueChange = { bin, txt -> passwordBinaryValue = bin;passwordTextValue = txt },
            decoder = { it.utf8String },
            encoder = { it.utf8Binary })
          Row(Modifier.align(Alignment.End).padding(vertical = 8.dp)) {
            PasswordActions()
          }
        }

        PasswordViewMode.Base64 -> Column {
          PasswordTextValue(
            value = passwordBinaryValue,
            onValueChange = { bin, txt -> passwordBinaryValue = bin;passwordTextValue = txt },
            decoder = if (urlMode) ({ it.base64UrlString }) else ({ it.base64String }),
            encoder = if (urlMode) ({ it.base64UrlBinary }) else ({ it.base64Binary }),
          )
          Row(
            Modifier.align(Alignment.End).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            CheckboxWithLabel(checked = urlMode, onCheckedChange = { urlMode = it }) {
              Text(text = KeychainI18nResource.password_base64_url_mode())
            }
            Spacer(Modifier)
            PasswordActions()
          }
        }

        PasswordViewMode.Binary -> Column {
          PasswordTextValue(
            value = passwordBinaryValue,
            onValueChange = { bin, txt -> passwordBinaryValue = bin;passwordTextValue = txt },
            decoder = if (hexMode) ({ it.hexString }) else ({ it.joinToString(",") }),
            encoder = if (hexMode) ({ it.hexBinary }) else ({
              it.split(",").map { b -> b.toByte() }.toByteArray()
            }),
          )
          Row(
            Modifier.align(Alignment.End).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            CheckboxWithLabel(checked = hexMode, onCheckedChange = { hexMode = it }) {
              Text(text = KeychainI18nResource.password_binary_hex_mode())
            }
            Spacer(Modifier)
            PasswordActions()
          }
        }
      }
    }
  }
}

@Composable
fun CheckboxWithLabel(
  checked: Boolean,
  onCheckedChange: ((Boolean) -> Unit)? = null,
  modifier: Modifier = Modifier,
  label: @Composable RowScope.() -> Unit,
) {
  Row(
    modifier,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(
      checked = checked, onCheckedChange = onCheckedChange
    )
    label()
  }
}

enum class PasswordViewMode(val mode: String, val label: SimpleI18nResource) {
  Utf8("utf8", KeychainI18nResource.password_mode_label_utf8), Base64(
    "base64", KeychainI18nResource.password_mode_label_base64
  ),
  Binary("binary", KeychainI18nResource.password_mode_label_binary),
}

enum class PasswordReadWriteMode(val mode: String) {
  Readonly("readonly"), ReadWrite("readwrite"),
}