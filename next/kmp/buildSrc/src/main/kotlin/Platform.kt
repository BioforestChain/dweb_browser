object Platform {
  val osName = System.getProperty("os.name")
  val osArch = System.getProperty("os.arch")

  init {
    println("platform-os-name=$osName")
    println("platform-os-arch=$osArch")
  }

  val isMac = osName.startsWith("Mac")
  val isWindows = osName.startsWith("Windows")
  val isLinux = osName.startsWith("Linux")

  /// 寄存器宽度
  val is64 = listOf("amd64", "x86_64", "aarch64").contains(osArch)
  val is32 = listOf("x86", "arm").contains(osArch)

  /// 指令集
  val isX86 = setOf("x86", "i386", "amd64", "x86_64").contains(osArch)
  val isArm = listOf("aarch64", "arm").contains(osArch)
}
