package org.dweb_browser.sys.keychain.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.min
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.StableSmartLoad
import org.dweb_browser.sys.keychain.deviceKeyStore
import org.dweb_browser.sys.keychain.render.KeychainActivity
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.render.blobFetchHook

abstract class EncryptKey {
  companion object {

    /**
     * 获取根密钥
     */
    suspend fun getRootKey(
      params: UseKeyParams,
    ): EncryptKey {
      val secretKeyRawBytes = KeychainActivity.create(params.runtime).start(
        title = params.title,
        subtitle = params.subtitle,
        description = params.description,
        background = {
          BoxWithConstraints(
            modifier = Modifier.fillMaxSize().clip(RectangleShape)
              .background(LocalColorful.current.Amber.current),
            contentAlignment = Alignment.CenterEnd,
          ) {
            remember { params.runtime.icons.toStrict().pickLargest() }?.also { icon ->
              val size = min(maxWidth, maxHeight)
              PureImageLoader.StableSmartLoad(icon.src, size, size, params.runtime.blobFetchHook)
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
      val version = deviceKeyStore.getItem("root-key-version") ?: run {
        deviceKeyStore.setItem("root-key-version", RootKeyV1.VERSION)
      }
      return when (version) {
        RootKeyV1.VERSION -> RootKeyV1(secretKeyRawBytes)
        else -> {
          WARNING("invalid root-key-version: $version")
          params.runtime.showToast("您的钥匙串数据可能已经遭到损坏")
          RootKeyV1(secretKeyRawBytes)
        }
      }
    }
  }

  abstract suspend fun encryptData(
    params: UseKeyParams,
    sourceData: ByteArray,
  ): ByteArray

  abstract suspend fun decryptData(
    params: UseKeyParams,
    encryptedBytes: ByteArray,
  ): ByteArray
}

data class UseKeyParams(
  val runtime: MicroModule.Runtime,
  val remoteMmid: String,
  val title: String? = null,
  val subtitle: String? = null,
  val description: String? = null,
)
typealias RecoveryKey = suspend (params: UseKeyParams) -> EncryptKey?
typealias GenerateKey = suspend (params: UseKeyParams) -> EncryptKey

@OptIn(ExperimentalSerializationApi::class)
val EncryptCbor = Cbor {
  serializersModule = SerializersModule {
    polymorphic(EncryptKey::class) {
      subclass(EncryptKeyV1::class)
    }
  }
}
