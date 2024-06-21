package org.dweb_browser.browser.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.helper.PrivacyUrl
import org.dweb_browser.helper.platform.theme.dimens

@Composable
fun SplashPrivacyDialog(
  openHome: () -> Unit, openWebView: (String) -> Unit, closeApp: () -> Unit,
) {
  var showPrivacyDeny by remember { mutableStateOf(false) }

  val transition = updateTransition(showPrivacyDeny, "")
  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier.widthIn(max = 600.dp).align(Alignment.BottomCenter),
    ) {
      transition.AnimatedVisibility(
        visible = { show -> !show },
        enter = slideInVertically(
          initialOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(350)
        ),
        exit = slideOutVertically(
          targetOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(50)
        ),
      ) {
        SplashPrivacyView(openWebView, openHome) { showPrivacyDeny = true }
      }

      transition.AnimatedVisibility(
        visible = { show -> show },
        enter = slideInVertically(
          initialOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(350)
        ),
        exit = slideOutVertically(
          targetOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(50)
        ),
      ) {
        SplashPrivacyDeny(closeApp) { showPrivacyDeny = false }
      }
    }
  }
}

@Composable
private fun SplashPrivacyView(
  openWebView: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
  BottomSheet {
    Text(
      text = BrowserI18nResource.privacy_title(),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))

    val privacyContent = BrowserI18nResource.privacy_content()
    val privacyPolicy = BrowserI18nResource.privacy_policy()
    val annotatedString = buildAnnotatedString {
      privacyContent.split(privacyPolicy).also { parts ->
        parts.forEachIndexed { index, part ->
          if (index > 0) {
            withStyle(SpanStyle(MaterialTheme.colorScheme.primary)) {
              withLink(LinkAnnotation.Clickable(privacyPolicy) {
                openWebView(PrivacyUrl)
              }) {
                append(privacyPolicy)
              }
            }
          }
          append(part)
        }
      }
    }
    // Use Text or BasicText and pass an AnnotatedString that contains a LinkAnnotation
    Text(
      text = annotatedString,
      style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(24.dp))

    BottomButton(
      dismissStr = BrowserI18nResource.privacy_button_refuse(),
      confirmStr = BrowserI18nResource.privacy_button_agree(),
      onDismiss = onDismiss,
      onConfirm = onConfirm
    )
  }
}


/**
 * 这个是不同意协议后，弹出的再次确认框
 */
@Composable
private fun SplashPrivacyDeny(onDismiss: () -> Unit, onConfirm: () -> Unit) {
  BottomSheet {
    Text(
      text = BrowserI18nResource.privacy_title(),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = BrowserI18nResource.privacy_content_deny(),
      style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(24.dp))

    BottomButton(
      dismissStr = BrowserI18nResource.privacy_button_exit(),
      confirmStr = BrowserI18nResource.privacy_button_i_know(),
      onDismiss = onDismiss,
      onConfirm = onConfirm
    )
  }
}


@Composable
private fun BottomSheet(content: @Composable ColumnScope.() -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    shadowElevation = MaterialTheme.dimens.medium,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      content = content
    )
  }
}

@Composable
private fun BottomButton(
  dismissStr: String, confirmStr: String, onDismiss: () -> Unit, onConfirm: () -> Unit,
) {
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val highEmphasisButtonSize = maxWidth * 0.385f
    Row(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
      horizontalArrangement = Arrangement.End
    ) {
      TextButton(onDismiss) {
        Text(text = dismissStr)
      }
      Spacer(modifier = Modifier.width(24.dp))
      FilledTonalButton(onConfirm, modifier = Modifier.widthIn(highEmphasisButtonSize)) {
        Text(confirmStr)
      }
    }
  }
}