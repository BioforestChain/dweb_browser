# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 类名保持，只要在相应的包名下的
-keep class info.bagen.dwebbrowser.** { *; }
-keep class org.dweb_browser.** { *; }
-keep interface org.dweb_browser.** { *; }
-keep class biometrics.** { *; }
-keep interface biometrics.** { *; }
-keep class keychainstore.** { *; }
-keep interface keychainstore.** { *; }
-keep class mix_compression.** { *; }
-keep interface mix_compression.** { *; }
-keep class multipart.** { *; }
-keep interface multipart.** { *; }
-keep class resvg_render.** { *; }
-keep interface resvg_render.** { *; }
-keep class gradlew.** { *; }
-keep interface gradlew.** { *; }
-keep class reverse_proxy.** { *; }
-keep interface reverse_proxy.** { *; }
-keep class hardware_info.** { *; }
-keep interface hardware_info.** { *; }
-keep class ziplib.** { *; }
-keep interface ziplib.** { *; }

# com.sun.jna
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-dontwarn java.awt.*
-keep class reverse_proxy.** { *; }
-keepclassmembers class * extends reverse_proxy.** { public *; }
-keep class ziplib.** { *; }
-keepclassmembers class * extends ziplib.** { public *; }


# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
# 一定要保持 values 函数，否则编译出来代码是异常的，目前第三方代码还没有完全改成 Enum.entries，还是有在用 Enum.values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn javax.imageio.spi.ImageInputStreamSpi
-dontwarn javax.imageio.spi.ImageOutputStreamSpi
-dontwarn javax.imageio.spi.ImageReaderSpi
-dontwarn javax.imageio.spi.ImageWriterSpi
-dontwarn androidx.test.platform.app.InstrumentationRegistry

-dontwarn androidx.window.extensions.WindowExtensions
-dontwarn androidx.window.extensions.WindowExtensionsProvider
-dontwarn androidx.window.extensions.area.ExtensionWindowAreaPresentation
-dontwarn androidx.window.extensions.layout.DisplayFeature
-dontwarn androidx.window.extensions.layout.FoldingFeature
-dontwarn androidx.window.extensions.layout.WindowLayoutComponent
-dontwarn androidx.window.extensions.layout.WindowLayoutInfo
-dontwarn androidx.window.sidecar.SidecarDeviceState
-dontwarn androidx.window.sidecar.SidecarDisplayFeature
-dontwarn androidx.window.sidecar.SidecarInterface$SidecarCallback
-dontwarn androidx.window.sidecar.SidecarInterface
-dontwarn androidx.window.sidecar.SidecarProvider
-dontwarn androidx.window.sidecar.SidecarWindowLayoutInfo