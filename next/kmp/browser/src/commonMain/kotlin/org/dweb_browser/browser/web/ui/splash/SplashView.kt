package org.dweb_browser.browser.web.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.helper.PrivacyUrl

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashPrivacyDialog(
  openHome: () -> Unit, openWebView: (String) -> Unit, closeApp: () -> Unit
) {
  val showPrivacyDeny = remember { mutableStateOf(false) }

  val transition = updateTransition(showPrivacyDeny.value, "")
  Box(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .background(Color.Black.copy(0.5f))
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
    ) {
      transition.AnimatedVisibility(
        visible = { show -> !show }, enter = slideInVertically(
          initialOffsetY = { fullHeight -> 2 * fullHeight }, animationSpec = tween(1000)
        ), exit = slideOutVertically(
          targetOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(50)
        )
      ) {
        SplashPrivacyView(openWebView, openHome) { showPrivacyDeny.value = true }
      }

      transition.AnimatedVisibility(
        visible = { show -> show }, enter = slideInVertically(
          initialOffsetY = { fullHeight -> 2 * fullHeight }, animationSpec = tween(1000)
        ), exit = slideOutVertically(
          targetOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(50)
        )
      ) {
        SplashPrivacyDeny(closeApp) { showPrivacyDeny.value = false }
      }
    }
  }
}

@Composable
private fun SplashPrivacyView(
  openWebView: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
  val privacyContent1 = BrowserI18nResource.privacy_content_1()
  val privacyContent2 = BrowserI18nResource.privacy_content_2()
  val privacyPolicy = BrowserI18nResource.privacy_policy()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp)
  ) {
    Text(
      text = BrowserI18nResource.privacy_title(),
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp
    )
    Spacer(modifier = Modifier.height(12.dp))

    val annotatedString = buildAnnotatedString {
      append(privacyContent1)
      pushStringAnnotation("ysxy", PrivacyUrl)
      withStyle(
        SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)
      ) {
        append(privacyPolicy)
      }
      pop()
      append(privacyContent2)
    }
    ClickableText(
      text = annotatedString,
      style = TextStyle(fontSize = 16.sp),
      onClick = { position ->
        //通过下面的tag标识找到对应的位置
        annotatedString.getStringAnnotations("ysxy", start = position, end = position).firstOrNull()
          ?.let { annotation -> openWebView(annotation.item) }
      })
    Spacer(modifier = Modifier.height(24.dp))

    BottomButton(
      dismissStr = BrowserI18nResource.privacy_button_refuse(),
      confirmStr = BrowserI18nResource.privacy_button_agree(),
      onDismiss = onDismiss,
      onConfirm = onConfirm
    )
  }
}

@Composable
private fun BottomButton(
  dismissStr: String, confirmStr: String, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Text(text = dismissStr,
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable { onDismiss() }
        .padding(top = 12.dp, bottom = 12.dp),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.width(24.dp))
    Text(text = confirmStr,
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable { onConfirm() }
        .padding(top = 12.dp, bottom = 12.dp),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      color = Color(14, 80, 252))
  }
}

/**
 * 这个是不同意协议后，弹出的再次确认框
 */
@Composable
private fun SplashPrivacyDeny(onDismiss: () -> Unit, onConfirm: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp)
  ) {
    Text(
      text = BrowserI18nResource.privacy_title(),
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp
    )
    Spacer(modifier = Modifier.height(12.dp))

    Text(text = BrowserI18nResource.privacy_content_deny())
    Spacer(modifier = Modifier.height(24.dp))

    BottomButton(
      dismissStr = BrowserI18nResource.privacy_button_exit(),
      confirmStr = BrowserI18nResource.privacy_button_i_know(),
      onDismiss = onDismiss,
      onConfirm = onConfirm
    )
  }
}

