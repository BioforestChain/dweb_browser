package org.dweb_browser.sys.keychain.render


import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Fingerprint
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.animation.headShake
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.helper.withIoContext
import org.dweb_browser.sys.biometrics.BiometricsManage
import org.dweb_browser.sys.keychain.KeychainI18nResource


@Composable
fun VerifyBiometrics(viewModel: VerifyBiometricsViewModel, modifier: Modifier = Modifier) {
  Column(
    Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    val iconAni = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
      iconAni.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
    }
    var result by remember { mutableStateOf<Boolean?>(null) }
    val iconTint = remember { Animatable(Color.Transparent) }
    val colorNull = LocalContentColor.current.copy(alpha = .5f)
    val colorSuccess = LocalColorful.current.Green.current
    val colorError = MaterialTheme.colorScheme.error
    LaunchedEffect(result, colorNull, colorSuccess, colorError) {
      iconTint.animateTo(
        when (result) {
          true -> colorSuccess
          false -> colorError
          null -> colorNull
        }, spring()
      )
    }
    Icon(
      Icons.TwoTone.Fingerprint,
      null,
      Modifier.size(64.dp).headShake(result == false) { result = null }.graphicsLayer {
        transformOrigin = TransformOrigin.Center
        scaleX = iconAni.value
        scaleY = iconAni.value
        alpha = iconAni.value
      },
      tint = iconTint.value
    )
    CardActions(Modifier.align(Alignment.End)) {
      val scope = rememberCoroutineScope()
      TextButton(
        onClick = {
          viewModel.refuse(CancellationException("user refused"))
        },
        enabled = !viewModel.verifying,
        colors = ButtonDefaults.textButtonColors()
          .copy(contentColor = MaterialTheme.colorScheme.error)
      ) {
        Text(CommonI18n.refuse())
      }
      TextButton(
        onClick = {
          scope.launch {
            result = viewModel.confirm()
          }
        },
        enabled = !viewModel.verifying
      ) {
        Text(KeychainI18nResource.biometrics_verify_submit())
      }
    }
  }
}

class VerifyBiometricsViewModel(
  override val task: CompletableDeferred<ByteArray>,
) : VerifyViewModelTask(KeychainMethod.Biometrics) {

  override fun keyTipCallback(keyTip: ByteArray?) {}
  override fun doFinish(): ByteArray {
    return byteArrayOf()
  }

  override suspend fun verifyUser(keyRawData: ByteArray): Boolean {
//    BiometricsManage.startLocalAuthentication()
    return withIoContext { BiometricsManage.biometricsAuthInGlobal("认证身份").success }
  }

  suspend fun confirm(): Boolean {
    return finish()
  }
}