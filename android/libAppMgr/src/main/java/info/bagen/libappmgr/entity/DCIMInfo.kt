package info.bagen.libappmgr.entity

import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.android.exoplayer2.SimpleExoPlayer
import info.bagen.libappmgr.system.media.MediaInfo
import info.bagen.libappmgr.system.media.getDurationOfMinute
import info.bagen.libappmgr.system.media.toByteArray
import java.io.File
import java.util.*

data class DCIMInfo(
    val path: String,
    val id: Int = 0,
    val type: DCIMType = DCIMType.IMAGE,
    val name: String = "",
    val time: Int = 0, // 用于表示文件时间
    val checked: MutableState<Boolean> = mutableStateOf(false),
    val index: MutableState<Int> = mutableStateOf(0),
    var duration: MutableState<Int> = mutableStateOf(0), // 如果是视频，需要加载时长
    var bitmap: ByteArray? = null, // 如果是视频，用于显示某一帧的图片
    var overlay: MutableState<Boolean> = mutableStateOf(false), // 主要是为了在预览模式下，显示的图片不直接删除，而是改为白色覆盖
)

data class DCIMSpinner(
    val name: String,
    var count: Int = 0,
    var path: Any = "",
    val checked: MutableState<Boolean> = mutableStateOf(false)
)

enum class DCIMType {
    IMAGE, SVG, GIF, VIDEO, OTHER
}

// 视频状态
enum class PlayerState {
    Play, Pause, END, Playing
}

data class ExoPlayerData(
    var exoPlayer: SimpleExoPlayer,
    var playerState: MutableState<PlayerState>
)

fun MediaInfo.createDCIMInfo(): DCIMInfo {
    return DCIMInfo(path = this.path)
}

/**
 * 更新视频的时间
 */
fun DCIMInfo.updateDuration() {
    if (this.type == DCIMType.VIDEO) {
        var mmr: MediaMetadataRetriever? = null
        try {
            mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            bitmap = mmr.frameAtTime?.toByteArray()
            duration.value = mmr.getDurationOfMinute()
            // mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            // title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            // album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            // artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            // bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            // date = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        } catch (e: Exception) {
            Log.d("DCIMInfo", "fail->$e")
        } finally {
            mmr?.release()
        }
    }
}

object MediaFile {
    // comma separated list of all file extensions supported by the media scanner
    var sFileExtensions: String? = null

    // Audio file types
    val FILE_TYPE_MP3 = 1
    val FILE_TYPE_M4A = 2
    val FILE_TYPE_WAV = 3
    val FILE_TYPE_AMR = 4
    val FILE_TYPE_AWB = 5
    val FILE_TYPE_WMA = 6
    val FILE_TYPE_OGG = 7
    private val FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3
    private val LAST_AUDIO_FILE_TYPE = FILE_TYPE_OGG

    // MIDI file types
    val FILE_TYPE_MID = 11
    val FILE_TYPE_SMF = 12
    val FILE_TYPE_IMY = 13
    private val FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID
    private val LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY

    // Video file types
    val FILE_TYPE_MP4 = 21
    val FILE_TYPE_M4V = 22
    val FILE_TYPE_3GPP = 23
    val FILE_TYPE_3GPP2 = 24

    //val FILE_TYPE_WMV = 25
    //val FILE_TYPE_RMVB = 26
    //val FILE_TYPE_FLV = 27
    //val FILE_TYPE_MKV = 28
    //val FILE_TYPE_AVI = 29
    private val FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4
    private val LAST_VIDEO_FILE_TYPE = FILE_TYPE_3GPP2

    // Image file types
    val FILE_TYPE_JPEG = 31
    val FILE_TYPE_GIF = 32
    val FILE_TYPE_PNG = 33
    val FILE_TYPE_BMP = 34
    val FILE_TYPE_WBMP = 35
    private val FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG
    private val LAST_IMAGE_FILE_TYPE = FILE_TYPE_WBMP

    val FILE_TYPE_SVG = 41

    // Playlist file types
    val FILE_TYPE_M3U = 51
    val FILE_TYPE_PLS = 52
    val FILE_TYPE_WPL = 53
    private val FIRST_PLAYLIST_FILE_TYPE = FILE_TYPE_M3U
    private val LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_WPL

    data class MediaFileType(var fileType: Int, var mimeType: String)

    private val sFileTypeMap = HashMap<String, MediaFileType>()
    private val sMimeTypeMap = HashMap<String, Int>()

    private fun addFileType(extension: String, fileType: Int, mimeType: String) {
        sFileTypeMap[extension] = MediaFileType(fileType, mimeType)
        sMimeTypeMap[mimeType] = fileType
    }

    init {
        addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg")
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4")
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav")
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr")
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb")
        addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma")
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg")

        addFileType("MID", FILE_TYPE_MID, "audio/midi")
        addFileType("XMF", FILE_TYPE_MID, "audio/midi")
        addFileType("RTTTL", FILE_TYPE_MID, "audio/midi")
        addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi")
        addFileType("IMY", FILE_TYPE_IMY, "audio/imelody")

        addFileType("MP4", FILE_TYPE_MP4, "video/mp4")
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4")
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp")
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp")
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2")
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2")
        //addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv")
        //addFileType("RMVB", FILE_TYPE_RMVB, "video/rmvb")
        //addFileType("FLV", FILE_TYPE_FLV, "video/flv")
        //addFileType("MKV", FILE_TYPE_MKV, "video/mkv")
        //addFileType("AVI", FILE_TYPE_AVI, "video/avi")

        addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg")
        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg")
        addFileType("GIF", FILE_TYPE_GIF, "image/gif")
        addFileType("PNG", FILE_TYPE_PNG, "image/png")
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp")
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp")

        addFileType("SVG", FILE_TYPE_SVG, "image/svg+xml")

        addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl")
        addFileType("PLS", FILE_TYPE_PLS, "audio/x-scpls")
        addFileType("WPL", FILE_TYPE_WPL, "application/vnd.ms-wpl")

        // compute file extensions list for native Media Scanner
        var builder = StringBuilder()
        var iterator = sFileTypeMap.iterator()

        while (iterator.hasNext()) {
            if (builder.isNotEmpty()) {
                builder.append(',')
            }
            builder.append(iterator.next())
        }
        sFileExtensions = builder.toString()
    }

    private fun isAudioFileType(fileType: Int): Boolean {
        return (fileType in FIRST_AUDIO_FILE_TYPE..LAST_AUDIO_FILE_TYPE ||
                fileType in FIRST_MIDI_FILE_TYPE..LAST_MIDI_FILE_TYPE)
    }

    private fun isVideoFileType(fileType: Int): Boolean {
        return (fileType in FIRST_VIDEO_FILE_TYPE..LAST_VIDEO_FILE_TYPE)
    }

    private fun isImageFileType(fileType: Int): Boolean {
        return (fileType in FIRST_IMAGE_FILE_TYPE..LAST_IMAGE_FILE_TYPE)
    }

    private fun isPlayListFileType(fileType: Int): Boolean {
        return (fileType in FIRST_PLAYLIST_FILE_TYPE..LAST_PLAYLIST_FILE_TYPE)
    }

    private fun getFileType(path: String): MediaFileType? {
        val lastDot = path.lastIndexOf(".")
        return if (lastDot < 0) null
        else sFileTypeMap[path.substring(lastDot + 1).uppercase(Locale.getDefault())]
    }

    //根据视频文件路径判断文件类型
    private fun isVideoFileType(path: String): Boolean {  //自己增加
        val type = getFileType(path)
        return if (null != type) {
            isVideoFileType(type.fileType)
        } else false
    }

    //根据音频文件路径判断文件类型
    private fun isAudioFileType(path: String): Boolean {  //自己增加
        val type = getFileType(path)
        return if (null != type) {
            isAudioFileType(type.fileType)
        } else false
    }

    //根据mime类型查看文件类型
    private fun getFileTypeForMimeType(mimeType: String): Int {
        val value = sMimeTypeMap[mimeType]
        return (value?.toInt() ?: 0)
    }

    private fun getDCIMType(path: String): DCIMType {
        val type = getFileType(path)
        type?.let {
            return when (it.fileType) {
                FILE_TYPE_GIF -> DCIMType.GIF
                in FIRST_VIDEO_FILE_TYPE..LAST_VIDEO_FILE_TYPE -> DCIMType.VIDEO
                in FIRST_IMAGE_FILE_TYPE..LAST_IMAGE_FILE_TYPE -> DCIMType.IMAGE
                FILE_TYPE_SVG -> DCIMType.SVG
                else -> {
                    DCIMType.OTHER
                }
            }
        } ?: return DCIMType.OTHER
    }

    fun createDCIMInfo(path: String): DCIMInfo {
        return DCIMInfo(
            path = path,
            type = getDCIMType(path),
            name = path.substring(path.lastIndexOf("/") + 1),
            time = (File(path).lastModified() / 1000).toInt()
        )
    }
}

