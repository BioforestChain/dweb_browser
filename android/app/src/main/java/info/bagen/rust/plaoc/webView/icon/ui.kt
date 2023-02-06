package info.bagen.rust.plaoc.webView.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import info.bagen.rust.plaoc.webView.IconByName

private var svgLoader: ImageLoader? = null


@Composable
private fun _getSvgLoader(): ImageLoader {
    if (svgLoader == null) {
        svgLoader = ImageLoader.Builder(LocalContext.current)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
    return svgLoader as ImageLoader
}

@Composable
fun DWebIcon(icon: DWebIcon, modifier: Modifier = Modifier) {
    val internal_modifier = modifier.let {
        if (icon.size != null) {
            it.size(icon.size.dp)
        } else {
            it
        }
    }
    when (icon.type) {
        DWebIcon.IconType.NamedIcon -> IconByName(
            name = icon.source,
            contentDescription = icon.description,
            modifier = internal_modifier,
        )
        DWebIcon.IconType.AssetIcon -> {
            val painter = rememberAsyncImagePainter(icon.source, imageLoader = _getSvgLoader())
            Icon(
                painter = painter,
                contentDescription = icon.description,
                tint = Color.Unspecified,
                modifier = internal_modifier,
            )
        }
    }
}
