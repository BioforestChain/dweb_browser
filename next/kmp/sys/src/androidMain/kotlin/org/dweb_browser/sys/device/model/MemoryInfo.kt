package org.dweb_browser.sys.device.model

import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.RandomAccessFile

@Serializable
data class MemoryData(
  var total: String = "",
  var usage: String = "",
  var free: String = "",
  var buffers: String = ""
)

@Serializable
data class StorageSize(
  var hasExternalSDCard: Boolean = false,
  var internalTotalSize: String = "",
  var internalFreeSize: String = "",
  var internalUsageSize: String = "",
  var externalTotalSize: String = "",
  var externalFreelSize: String = "",
)

/**
 * cat /proc/meminfo 获取内存信息
 * cat /proc/cpuinfo 获取cpu信息
 * cat /proc/stat 计算CPU使用率
 *
 */
class MemoryInfo {

  fun getMemoryInfo(): String {
    return Json.encodeToString(memoryData)
  }

  fun getStorageInfo(): String {
    return Json.encodeToString(storageSize)
  }

  val storageSize: StorageSize
    get() {
      val storageSize = StorageSize()
      storageSize.internalTotalSize = totalInternalStorageSize
      storageSize.internalFreeSize = availableInternalStorageSize
      storageSize.internalUsageSize = usageInternalStorageSize
      storageSize.hasExternalSDCard = hasExternalSDCard
      storageSize.externalTotalSize = totalExternalMemorySize
      storageSize.externalFreelSize = availableExternalMemorySize
      return storageSize
    }

  val memoryData: MemoryData
    get() {
      /*var totalMemory: Long = 0
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        val mi = ActivityManager.MemoryInfo()
        val activityManager =
          App.appContext.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        return mi.totalMem
      }*/
      var memoryData = MemoryData()
      var total: Long = 0L
      var free: Long = 0L
      return try {
        val reader = RandomAccessFile("/proc/meminfo", "r")
        for (i in 1..4) {
          var data = reader.readLine()
          if (data.contains("MemTotal:")) {
            total = data.replace("\\D+".toRegex(), "").toLong()
            memoryData.total = unitConversionFromKB(data)
          } else if (data.contains("MemFree:")) {
            free = data.replace("\\D+".toRegex(), "").toLong()
            memoryData.free = unitConversionFromKB(data)
          } else if (data.contains("Buffers:")) {
            memoryData.buffers = unitConversionFromKB(data)
          }
        }
        memoryData.usage = unitConversionFromKB(total - free)
        reader.close()
        memoryData
      } catch (e: IOException) {
        e.printStackTrace()
        memoryData
      }
    }

  private fun unitConversionFromKB(value: String): String {
    val load = value.replace("\\D+".toRegex(), "") //提取纯数字
    return unitConversionFromKB(load.toLong())
  }

  private fun unitConversionFromKB(size: Long): String {
    return if (size < 1024) {
      "$size KB"
    } else if (size < 1024 * 1024) {
      String.format("%.2f MB", (size / 1024.0))
    } else if (size < 1024 * 1024 * 1024) {
      String.format("%.2f GB", (size / 1024 / 1024.0))
    } else {
      String.format("%.2f TB", (size / 1024 / 1024 / 1024.0))
    }
  }

  /**
   * 下面是获取存储空间信息
   */
  val totalInternalStorageSize: String
    get() {
      val path = Environment.getDataDirectory()
      val stat = StatFs(path.path)
      val blockSize: Long
      val totalBlocks: Long
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        blockSize = stat.blockSizeLong
        totalBlocks = stat.blockCountLong
      } else {
        blockSize = stat.blockSize.toLong()
        totalBlocks = stat.blockCount.toLong()
      }
      return unitConversionFromKB(totalBlocks * blockSize / 1024)
    }

  val availableInternalStorageSize: String
    get() {
      val path = Environment.getDataDirectory()
      val stat = StatFs(path.path)
      val blockSize: Long
      val availableBlocks: Long
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        blockSize = stat.blockSizeLong
        availableBlocks = stat.availableBlocksLong
      } else {
        blockSize = stat.blockSize.toLong()
        availableBlocks = stat.availableBlocks.toLong()
      }
      return unitConversionFromKB(availableBlocks * blockSize / 1024)
    }

  val usageInternalStorageSize: String
    get() {
      val path = Environment.getDataDirectory()
      val stat = StatFs(path.path)
      val blockSize: Long
      val availableBlocks: Long
      val totalBlocks: Long
      val usageBlocks: Long
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        blockSize = stat.blockSizeLong
        availableBlocks = stat.availableBlocksLong
        totalBlocks = stat.blockCountLong
        usageBlocks = totalBlocks - availableBlocks
      } else {
        blockSize = stat.blockSize.toLong()
        availableBlocks = stat.availableBlocks.toLong()
        totalBlocks = stat.blockCount.toLong()
        usageBlocks = totalBlocks - availableBlocks
      }
      return unitConversionFromKB(usageBlocks * blockSize / 1024)
    }

  val hasExternalSDCard: Boolean get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

  val availableExternalMemorySize: String
    get() {
      if (hasExternalSDCard) {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        val blockSize: Long
        val availableBlocks: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          blockSize = stat.blockSizeLong
          availableBlocks = stat.availableBlocksLong
        } else {
          blockSize = stat.blockSize.toLong()
          availableBlocks = stat.availableBlocks.toLong()
        }
        return unitConversionFromKB(availableBlocks * blockSize / 1024)
      }
      return "0"
    }

  val totalExternalMemorySize: String
    get() {
      if (hasExternalSDCard) {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        val blockSize: Long
        val totalBlocks: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          blockSize = stat.blockSizeLong
          totalBlocks = stat.blockCountLong
        } else {
          blockSize = stat.blockSize.toLong()
          totalBlocks = stat.blockCount.toLong()
        }
        return unitConversionFromKB(totalBlocks * blockSize / 1024)
      }
      return "0"
    }
}
