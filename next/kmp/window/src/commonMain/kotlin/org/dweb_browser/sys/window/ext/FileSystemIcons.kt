package org.dweb_browser.sys.window.ext

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import dweb_browser_kmp.window.generated.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.mix_compression.zstdDecompress
import org.dweb_browser.pure.image.compose.ImageLoadResult
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi

object FileSystemIcons {

  @OptIn(ExperimentalResourceApi::class)
  val fsIconsLoader = SuspendOnce {
    val bundleSource = zstdDecompress(Res.readBytes("files/window-fs-icons/bundle.json.zstd"))
    Json.decodeFromString<FileSystemIconBundle>(bundleSource.utf8String)
  }

  @Serializable
  data class FileSystemIconBundle(val darker: FileSystemIcons, val light: FileSystemIcons) {
    fun getIcons(isDark: Boolean) = when {
      isDark -> darker
      else -> light
    }
  }

  @Composable
  fun fsIcons(isDark: Boolean = isSystemInDarkTheme()): FileSystemIcons? {
    val fsIcons = fsIconsLoader.getResultOrNull() ?: produceState<FileSystemIconBundle?>(null) {
      value = fsIconsLoader()
    }.value
    return fsIcons?.getIcons(isDark)
  }

  @Serializable
  data class FileSystemIcons(
    val iconResources: Map<String, String>,
    val fileExtnameMap: Map<String, String>,
    val fileFullnameMap: Map<String, String>,
    val folderFullnameMap: Map<String, String>,
    val defaultMap: DefaultIcons,
  ) {
    private fun getResourceIdByFilename(filename: String): String {
      val resourceId =
        fileFullnameMap[filename] ?: filename.split(".").toMutableList().let { segments ->
          /// 不能使用 removeFirst ，会和 android api level 35 的java冲突导致崩溃
          /// see: https://youtrack.jetbrains.com/issue/KT-71375/Prevent-Kotlins-removeFirst-and-removeLast-from-causing-crashes-on-Android-14-and-below-after-upgrading-to-Android-API-Level-35
          segments.removeAt(0)
          while (segments.isNotEmpty()) {
            val ext = segments.joinToString(".")
            fileExtnameMap[ext]?.also { return@let it }
            segments.removeAt(0)
          }
          return@let defaultMap.file
        }
      return resourceId
    }

    private fun getResource(resourceId: String): String {
      return iconResources.getValue(resourceId)
    }

    @Transient
    private val uriCache = SafeHashMap<String, String>()
    fun getResourceUrlByFilename(filename: String) = getResourceIdByFilename(filename).let {
      uriCache.getOrPut(it) {
        "data:image/svg+xml;base64,${getResource(it).utf8Binary.base64String}"
      }
    }
  }

  @Serializable
  data class DefaultIcons(val file: String, val folder: String)

}

@Composable
private fun MicroModule.Runtime.loadFileIconByFilename(
  filename: String,
  size: Dp,
): ImageLoadResult? {
  val isDark = runCatching { LocalWindowControllerTheme.current.isDark }.getOrElse {
    isSystemInDarkTheme()
  }
  val fsIconsRes = FileSystemIcons.fsIcons(isDark) ?: return null
  val iconRes = fsIconsRes.getResourceUrlByFilename(filename)
  return PureImageLoader.SmartLoad(iconRes, size, size)
}

@Composable
fun MicroModule.Runtime.FileIconByFilename(
  filename: String,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  FileIconByFilename(filename, size) {
    Image(it, filename, modifier)
  }
}

@Composable
fun MicroModule.Runtime.FileIconByFilename(
  filename: String,
  size: Dp,
  content: @Composable (ImageBitmap) -> Unit,
) {
  when (val res = loadFileIconByFilename(filename, size)) {
    null -> Box(Modifier.requiredSize(size))
    else -> res.with(
      onBusy = { Box(Modifier.requiredSize(size)) },
      onError = { Box(Modifier.requiredSize(size)) },
      onSuccess = { content(it) }
    )
  }
}
