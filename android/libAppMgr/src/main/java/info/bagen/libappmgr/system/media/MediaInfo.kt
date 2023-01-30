package info.bagen.libappmgr.system.media

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import info.bagen.libappmgr.database.AppContract
import info.bagen.libappmgr.utils.BitmapUtil
import java.io.ByteArrayOutputStream

enum class MediaType(name: String) {
    All("All"), Image("Image"), Video("Video"), Svg("Svg"), Gif("gif")
}

data class MediaInfo(
    val id: Int = 0, // 用户做唯一性标识
    val path: String, // 表示文件存放位置
    var type: String = "", // 用于表示当前media类型，Video,Image
    // var mimeType: String = "", // 用于表示当前media类型，video(3gp,mp4,mkv..), image(png,jpg,jpeg,png,svg,gif)
    var duration: Int = 0, // 用于表示视频的时长，默认是0 s
    var time: Int = 0, // 用于表示文件更新时间
    var thumbnail: ByteArray? = null, // 用于显示图片的缩略图，视频的某一祯图片
    var bitmap: ByteArray? = null, // 用于原图数据
)

fun MediaInfo.loadThumbnail() {
    try {
        if (type == MediaType.Video.name) {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            thumbnail = mmr.frameAtTime?.toByteArray()
            duration = mmr.getDurationOfMinute()
            mmr.release()
        } else {
            thumbnail = BitmapUtil.getImageThumbnail(path, 200, 200)?.toByteArray()
        }
    } catch (e: Exception) {
        Log.e("MediaManager", "MediaInfo.loadThumbnail -> path=$path (fail->$e)")
    }
}

fun MediaInfo.createContentValues(): ContentValues {
    val contentValues = ContentValues()
    contentValues.put(AppContract.Medias.COLUMN_PATH, path)
    contentValues.put(AppContract.Medias.COLUMN_TYPE, type)
    contentValues.put(AppContract.Medias.COLUMN_DURATION, duration)
    contentValues.put(AppContract.Medias.COLUMN_TIME, time)
    contentValues.put(AppContract.Medias.COLUMN_THUMBNAIL, thumbnail)
    contentValues.put(AppContract.Medias.COLUMN_BITMAP, bitmap)
    return contentValues
}

fun Bitmap.toByteArray(): ByteArray? {
    var baos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 50, baos)
    return baos.toByteArray()
}

fun MediaMetadataRetriever.getDurationOfMinute(): Int {
    return try {
        extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
            Integer.parseInt(it) / 1000
        } ?: 0
    } catch (e: Exception) {
        0
    }
}
