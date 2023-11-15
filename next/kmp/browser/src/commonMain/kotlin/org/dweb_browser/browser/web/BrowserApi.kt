package org.dweb_browser.browser.web

import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.ImageResource

expect fun ImageBitmap.toImageResource(): ImageResource?
expect fun getImageResourceRootPath(): String