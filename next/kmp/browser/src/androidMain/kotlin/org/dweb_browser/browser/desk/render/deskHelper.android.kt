package org.dweb_browser.browser.desk.render

import android.annotation.SuppressLint
import android.os.Build

@SuppressLint("AnnotateVersionCheck")
actual fun canSupportModifierBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
