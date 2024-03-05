object Features {
  private val properties = java.util.Properties().also { properties ->
    java.io.File("local.properties").apply {
      if (exists()) {
        inputStream().use { properties.load(it) }
      }
    }
  }

  private val disabled = (properties.getOrDefault("app.disable", "") as String)
    .split(",")
    .map { it.trim().lowercase() };

  private val enabled = (properties.getOrDefault("app.experimental.enabled", "") as String)
    .split(",")
    .map { it.trim().lowercase() };

  class Bool(val enabled: Boolean) {
    val disabled = !enabled
  }

  val androidApp = Bool(!disabled.contains("android"));
  val iosApp = Bool(!disabled.contains("ios"));
  val desktopApp = Bool(enabled.contains("desktop"));
  val electronApp = Bool(enabled.contains("electron"));
  val libs = Bool(androidApp.enabled || iosApp.enabled || desktopApp.enabled)
}
