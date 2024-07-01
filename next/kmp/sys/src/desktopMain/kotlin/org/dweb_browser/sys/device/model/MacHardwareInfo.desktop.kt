package org.dweb_browser.sys.device.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.JsonLoose
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class SPHardwareData(
  @SerialName("SPHardwareDataType")
  val spHardwareDataType: List<SPHardwareDataItem>
)

@Serializable
data class SPHardwareDataItem(
  @SerialName("chip_type")
  val chip: String,
  @SerialName("machine_name")
  val modelName: String,
  @SerialName("number_processors")
  val numberProcessors: String,
  @SerialName("physical_memory")
  val physicalMemory: String
)

// 高效核心 节能核心
data class MacHardwareInfoData(
  val modelName: String,
  val chip: String,
  val ram: String,
  var cpuCoresNumber: String? = null,
  var cpuPerformanceCoresNumber: String? = null,
  var cpuEfficiencyCoresNumber: String? = null
)

object MacHardwareInfo {
  private fun execCommand(): String {
    val process =
      Runtime.getRuntime().exec("system_profiler SPHardwareDataType -detailLevel mini -json")
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    return reader.readText().trim()
  }

  fun getHardwareInfo(): MacHardwareInfoData? {
    val output = execCommand()
    val spHardwareData = JsonLoose.decodeFromString<SPHardwareData>(output)
    println("QAQ spHardwareData=${spHardwareData.spHardwareDataType.size}")
    if (spHardwareData.spHardwareDataType.isNotEmpty()) {
      val spHardwareDataItem = spHardwareData.spHardwareDataType[0]
      val numberProcessors =
        spHardwareDataItem.numberProcessors.replace("[a-zA-Z\\s]+".toRegex(), "").split(':')

      val hardwareInfoData = MacHardwareInfoData(
        modelName = spHardwareDataItem.modelName,
        chip = spHardwareDataItem.chip,
        ram = spHardwareDataItem.physicalMemory
      )
      if (numberProcessors.size == 3) {
        hardwareInfoData.cpuCoresNumber = numberProcessors.component1()
        hardwareInfoData.cpuPerformanceCoresNumber = numberProcessors.component2()
        hardwareInfoData.cpuEfficiencyCoresNumber = numberProcessors.component3()
      }

      return hardwareInfoData
    }

    return null
  }
}