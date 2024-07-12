package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Verified
import androidx.compose.material.icons.twotone.WarningAmber
import androidx.compose.material.icons.twotone.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.platform.theme.Colorful

enum class PasswordSafeLevel(
  val level: (Float) -> Boolean,
  val color: @Composable (Colorful) -> Color,
  val brand: @Composable (Color) -> Unit,
) {
  Weak(
    level = { it < 25f },
    color = { it.Red.current },
    brand = { color ->
      Row {
        Icon(Icons.TwoTone.WarningAmber, null, Modifier.size(16.dp), color)
        Text("弱密码", color = color)
      }
    },
  ),
  Medium(
    level = { 25 <= it && it < 50f },
    color = { it.Orange.current },
    brand = { color ->
      Row {
        Icon(Icons.TwoTone.Check, null, Modifier.size(16.dp), color)
        Text("中等强度密码", color = color)
      }
    },
  ),
  Strong(
    level = { 50 <= it && it < 70f },
    color = { it.Green.current },
    brand = { color ->
      Row {
        Icon(Icons.TwoTone.Verified, null, Modifier.size(16.dp), color)
        Text("安全的密码", color = color)
      }
    },
  ),
  VeryStrong(
    level = { it >= 70f },
    color = { it.Green.current },
    brand = { color ->
      Row {
        Icon(Icons.TwoTone.WorkspacePremium, null, Modifier.size(16.dp), color)
        Text("非常强的密码", color = color)
      }
    },
  ),
  ;

  companion object {
    fun calcScore(password: String?): Float {
      if (password !is String) {
        return 0f
      }

      var hasUpperCase = false
      var hasLowerCase = false
      var hasNumbers = false
      var hasSpecialCharacters = false
      var score = 0f

      score += when {
        password.length < 4 -> 5
        password.length < 8 -> 10
        else -> 25
      }

      if (password.any { it.isUpperCase() }) {
        score += 10
        hasUpperCase = true
      }
      if (password.any { it.isLowerCase() }) {
        score += 10
        hasLowerCase = true
      }

      val numMatch = password.filter { it.isDigit() }
      if (numMatch.isNotEmpty()) {
        score += if (numMatch.length > 1) 20 else 10
        hasNumbers = true
      }

      val specialCharRegex = Regex("""["#$%&'()*+,-./:;<=>?@\[\]^_`{|}~]""")
      val matches = specialCharRegex.findAll(password).count()
      if (matches > 0) {
        score += if (matches == 1) 10 else 25
        hasSpecialCharacters = true
      }

      if (hasUpperCase && hasLowerCase && hasNumbers && hasSpecialCharacters) {
        score += 10
      } else if (hasSpecialCharacters && hasNumbers && (hasUpperCase || hasLowerCase)) {
        score += 5
      } else if (hasNumbers && (hasUpperCase || hasLowerCase)) {
        score += 2
      }

      return score
    }
  }
}