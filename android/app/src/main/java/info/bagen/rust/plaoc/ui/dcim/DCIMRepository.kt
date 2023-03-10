package info.bagen.rust.plaoc.ui.dcim

import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.database.MediaDBManager
import info.bagen.rust.plaoc.network.ApiService
import info.bagen.rust.plaoc.ui.entity.DCIMInfo
import info.bagen.rust.plaoc.ui.entity.DCIMType
import info.bagen.rust.plaoc.ui.entity.MediaFile
import info.bagen.rust.plaoc.ui.entity.MediaType
import info.bagen.rust.plaoc.util.KEY_MEDIA_IS_LOADED
import info.bagen.rust.plaoc.util.getBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DCIMRepository(private val api: ApiService = ApiService.instance) {

  // 加载图片内容
  suspend fun loadDCIMInfo(result: (retMap: HashMap<String, ArrayList<DCIMInfo>>) -> Unit) {
    withContext(Dispatchers.IO) {
      val dcimMaps: HashMap<String, ArrayList<DCIMInfo>> = hashMapOf()
      if (App.appContext.getBoolean(KEY_MEDIA_IS_LOADED)) { // 如果后台加载完毕，使用后台的数据
        MediaDBManager.getMediaFilter().forEach { name ->
          val list = arrayListOf<DCIMInfo>()
          MediaDBManager.queryMediaData(filter = name, loadPath = true)
            .forEach { mediaInfo ->
              val dcimInfo = DCIMInfo(
                path = mediaInfo.path,
                id = mediaInfo.id,
                type = when (mediaInfo.type) {
                  MediaType.Video.name -> DCIMType.VIDEO
                  else -> DCIMType.IMAGE
                },
                time = mediaInfo.time,
                duration = mutableStateOf(mediaInfo.duration),
                bitmap = mediaInfo.thumbnail
              )
              list.add(dcimInfo)
            }
          dcimMaps[name] = list
        }
      }
      if (dcimMaps.isEmpty()) { // 如果后台没有加载完成，先自行加载
        val pathDCIM = Environment.getExternalStoragePublicDirectory("DCIM")
        traverseDCIM(pathDCIM.absolutePath, dcimMaps)
        val pathPicture = Environment.getExternalStoragePublicDirectory("Pictures")
        traverseDCIM(pathPicture.absolutePath, dcimMaps)
      }
      result(dcimMaps)
    }
  }

  /**
   * 遍历当前目录及其子目录所有文件
   */
  private fun traverseDCIM(path: String, maps: HashMap<String, ArrayList<DCIMInfo>>) {
    var defaultPicture = "Pictures"
    if (!maps.containsKey(defaultPicture)) maps[defaultPicture] =
      arrayListOf() // 默认先创建一个图片目录，用于保存根目录存在的图片
    var files = File(path).listFiles()
    files?.forEach inLoop@{ file ->
      var name = file.name
      if (name.startsWith(".")) return@inLoop // 判断第一个字符如果是. 不执行当前文件，直接continue
      if (file.isFile) {
        var dcimInfo = MediaFile.createDCIMInfo(file.absolutePath)
        when (dcimInfo.type) {
          DCIMType.VIDEO, DCIMType.IMAGE, DCIMType.GIF -> {
            maps[defaultPicture]!!.add(dcimInfo)
          }
          else -> {}
        }
      } else if (file.isDirectory) {
        var list = maps[name] ?: arrayListOf()
        file.walk().iterator().forEach subLoop@{ subFile ->
          if (subFile.name.startsWith(".")) return@subLoop
          if (subFile.isFile) {
            var dcimInfo = MediaFile.createDCIMInfo(subFile.absolutePath)
            when (dcimInfo.type) {
              DCIMType.VIDEO, DCIMType.IMAGE, DCIMType.GIF -> {
                list.add(dcimInfo)
              }
              else -> {}
            }
          }
        }
        if (list.isNotEmpty() && !maps.containsKey(name)) {
          maps[name] = list // 判断list不为空，并且不在map中，进行添加
        }
      }
    }
  }
}
