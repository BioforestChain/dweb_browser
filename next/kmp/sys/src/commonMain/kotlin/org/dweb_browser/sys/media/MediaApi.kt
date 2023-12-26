package org.dweb_browser.sys.media

import org.dweb_browser.helper.platform.MultiPartFile

expect suspend fun savePictures(saveLocation: String, files: List<MultiPartFile>)