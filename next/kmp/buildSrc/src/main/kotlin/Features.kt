object Features {

  private val properties = java.util.Properties().also { properties ->
    java.io.File(System.getProperty("dweb-browser.root.dir"), "local.properties").apply {
      if (exists()) {
        inputStream().use { properties.load(it) }
      }
    }
  }

  private val disabled = properties.getProperty("app.disable", "")
    .split(",")
    .map { it.trim().lowercase() };

  private val enabled = properties.getProperty("app.experimental.enabled", "")
    .split(",")
    .map { it.trim().lowercase() };


  init {
    println("features-disabled=${disabled.joinToString(",")}")
    println("features-experimental.enabled=${enabled.joinToString(",")}")
  }

  class Bool(val enabled: Boolean) {
    val disabled = !enabled
  }

  val androidApp = Bool(!disabled.contains("android"));
  val iosApp = Bool(Platform.isMac && !disabled.contains("ios"));
  val desktopApp = Bool(!disabled.contains("desktop"));
  val electronApp = Bool(enabled.contains("electron"));
  val libs = Bool(androidApp.enabled || iosApp.enabled || desktopApp.enabled)
}
