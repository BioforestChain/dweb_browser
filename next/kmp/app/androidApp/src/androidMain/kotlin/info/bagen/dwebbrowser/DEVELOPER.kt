package info.bagen.dwebbrowser

import android.os.Build

enum class DEVELOPER(val deviceName: String) {
  GAUBEE("Xiaomi/M2006J10C"), //
  WaterbangXiaoMi("Xiaomi/2107119DC"), //
  HuangLin("HUAWEI/ELE-AL00"), //
  HLVirtual("Google/sdk_gphone64_x86_64"), //
  HLOppo("OPPO/PGJM10"), //
  HBXiaomi("Xiaomi/2209129SC"),
  ZGSansung("samsung/SM-A5360"),
  WaterBang("HONOR/ADT-AN00"), //
  Kingsword09("HUAWEI/BKL-AL20"), //
  KVirtual("Google/sdk_gphone64_arm64"), //
  ANONYMOUS("*"); //


  companion object {
    fun find(deviceName: String) = entries.find { it.deviceName == deviceName } ?: ANONYMOUS
    val CURRENT = find(Build.MANUFACTURER + "/" + Build.MODEL)

    init {
      println("Hi Developer! ${CURRENT.deviceName} => ${Build.MANUFACTURER}/${Build.MODEL}")
    }
  }
}