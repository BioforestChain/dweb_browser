package info.bagen.libappmgr.ui.dcim

import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import info.bagen.libappmgr.data.PreferencesHelper
import info.bagen.libappmgr.database.MediaDBManager
import info.bagen.libappmgr.entity.DCIMInfo
import info.bagen.libappmgr.entity.DCIMType
import info.bagen.libappmgr.entity.MediaFile
import info.bagen.libappmgr.network.ApiService
import info.bagen.libappmgr.system.media.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DCIMRepository(private val api: ApiService = ApiService.instance) {

    // 加载图片内容
    suspend fun loadDCIMInfo(result: (retMap: HashMap<String, ArrayList<DCIMInfo>>) -> Unit) {
        withContext(Dispatchers.IO) {
            val dcimMaps: HashMap<String, ArrayList<DCIMInfo>> = hashMapOf()
            if (PreferencesHelper.isMediaLoading()) { // 如果后台加载完毕，使用后台的数据
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
