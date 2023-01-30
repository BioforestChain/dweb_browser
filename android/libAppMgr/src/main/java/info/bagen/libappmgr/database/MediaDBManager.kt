package info.bagen.libappmgr.database

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.database.Cursor
import info.bagen.libappmgr.system.media.MediaInfo
import info.bagen.libappmgr.system.media.MediaType
import info.bagen.libappmgr.system.media.createContentValues
import info.bagen.libappmgr.utils.AppContextUtil
import info.bagen.libappmgr.utils.createMediaInfo
import java.io.File

object MediaDBManager {
    private val PROJECTION = arrayOf(
        AppContract.Medias.COLUMN_ID,
        AppContract.Medias.COLUMN_PATH,
        AppContract.Medias.COLUMN_TYPE,
        AppContract.Medias.COLUMN_DURATION,
        AppContract.Medias.COLUMN_TIME,
        AppContract.Medias.COLUMN_THUMBNAIL//, AppContract.Medias.COLUMN_BITMAP
    )

    /**
     * 获取数据库中的数据
     * @param id 数据库中的主键ID
     * @param type 区分查找类型
     * @return 返回查找结果
     */
    @SuppressLint("Range")
    fun queryMediaData(
        id: Int, type: MediaType = MediaType.All, isOrigin: Boolean = false
    ): MediaInfo? {
        // 查询数据库，然后获取path，转为base64
        val selection = when (type) {
            MediaType.All -> "${AppContract.Medias.COLUMN_ID} = ?"
            else -> "${AppContract.Medias.COLUMN_ID} = ? AND ${AppContract.Medias.COLUMN_TYPE} = ?"
        }
        val selectionArgs = when (type) {
            MediaType.All -> arrayOf("$id")
            else -> arrayOf("$id", "$type")
        }
        val sortOrder = "${AppContract.Medias.COLUMN_TIME}, ${AppContract.Medias.COLUMN_ID}"

        var mediaInfo: MediaInfo? = null
        val cursor = AppContextUtil.sInstance!!.contentResolver.query(
            AppContract.Medias.CONTENT_URI, PROJECTION, selection, selectionArgs, sortOrder
        )
        cursor?.let { c ->
            if (c.moveToFirst()) {
                mediaInfo = getMediaInfo(c, isOrigin)
            }
            c.close()
        }
        return mediaInfo
    }

    /**
     * 获取数据库中的数据
     * @param type 区分查找类型
     * @param from 和 to 需要联合使用，表示当前查找的起始位置
     * @return 返回查找结果
     */
    fun queryMediaData(
        type: MediaType = MediaType.All,
        filter: String? = null,
        from: Int = -1,
        to: Int = -1,
        isOrigin: Boolean = false,
        loadPath: Boolean = false
    ): ArrayList<MediaInfo> {
        // 查询数据库，然后获取path，转为base64
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        if (type != MediaType.All && filter != null) {
            selection =
                "${AppContract.Medias.COLUMN_TYPE} = ? AND ${AppContract.Medias.COLUMN_FILTER} = ?"
            selectionArgs = arrayOf(type.name, filter)
        } else if (type != MediaType.All) {
            selection = "${AppContract.Medias.COLUMN_TYPE} = ? "
            selectionArgs = arrayOf(type.name)
        } else if (filter != null) {
            selection = "${AppContract.Medias.COLUMN_FILTER} = ?"
            selectionArgs = arrayOf(filter)
        }
        val sortOrder = if (from >= 0 && to > 0 && from < to) {
            "${AppContract.Medias.COLUMN_TIME}, ${AppContract.Medias.COLUMN_ID} limit $from,$to"
        } else {
            "${AppContract.Medias.COLUMN_TIME}, ${AppContract.Medias.COLUMN_ID}"
        }
        val list = arrayListOf<MediaInfo>()
        val cursor = AppContextUtil.sInstance!!.contentResolver.query(
            AppContract.Medias.CONTENT_URI, PROJECTION, selection, selectionArgs, sortOrder
        )
        cursor?.let { c ->
            c.moveToPrevious()
            while (c.moveToNext()) {
                getMediaInfo(c, isOrigin, loadPath)?.let { list.add(it) }
            }
            c.close()
        }
        return list
    }

    @SuppressLint("Range")
    private fun getMediaInfo(
        cursor: Cursor, isOrigin: Boolean, loadPath: Boolean = false
    ): MediaInfo? {
        return try {
            val id = cursor.getInt(cursor.getColumnIndex(AppContract.Medias.COLUMN_ID))
            val path = if (loadPath) {
                cursor.getString(cursor.getColumnIndex(AppContract.Medias.COLUMN_PATH))
            } else {
                ""
            }
            val type = cursor.getString(cursor.getColumnIndex(AppContract.Medias.COLUMN_TYPE))
            val duration = cursor.getInt(cursor.getColumnIndex(AppContract.Medias.COLUMN_DURATION))
            val time = cursor.getInt(cursor.getColumnIndex(AppContract.Medias.COLUMN_TIME))
            val thumbnail =
                cursor.getBlob(cursor.getColumnIndex(AppContract.Medias.COLUMN_THUMBNAIL))
            val bitmap = if (isOrigin) {
                cursor.getBlob(cursor.getColumnIndex(AppContract.Medias.COLUMN_BITMAP))
            } else {
                null
            }
            MediaInfo(id, path, type, duration, time, thumbnail, bitmap)
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("Range")
    fun getMediaFilter(): ArrayList<String> {
        val cursor = AppContextUtil.sInstance!!.contentResolver.query(
            AppContract.Medias.CONTENT_URI,
            arrayOf(AppContract.Medias.COLUMN_FILTER),
            "1=1 GROUP BY ${AppContract.Medias.COLUMN_FILTER}",
            null,
            null
        )
        val list = arrayListOf<String>()
        cursor?.let { c ->
            c.moveToPrevious()
            while (c.moveToNext()) {
                list.add(c.getString(c.getColumnIndex(AppContract.Medias.COLUMN_FILTER)))
            }
            c.close()
        }
        return list
    }

    @SuppressLint("Range")
    fun getThumbnail(id: Int): ByteArray? {
        val cursor = AppContextUtil.sInstance!!.contentResolver.query(
            AppContract.Medias.CONTENT_URI,
            arrayOf(AppContract.Medias.COLUMN_THUMBNAIL),
            "${AppContract.Medias.COLUMN_ID} = ?",
            arrayOf("$id"),
            null
        )
        cursor?.let { c ->
            if (c.moveToFirst()) {
                return c.getBlob(c.getColumnIndex(AppContract.Medias.COLUMN_THUMBNAIL))
            }
            c.close()
        }
        return null
    }

    fun saveMediaInfoList(saveMaps: HashMap<String, ArrayList<MediaInfo>>) {
        val maps = getExistsPathMaps()
        var count = 0
        val ops = arrayListOf<ContentProviderOperation>()
        saveMaps.forEach {
            val filter = it.key
            it.value.forEach { mediaInfo ->
                val mapMediaInfo = maps[mediaInfo.path]
                if (mapMediaInfo == null) {
                    val cpo = ContentProviderOperation.newInsert(AppContract.Medias.CONTENT_URI)
                        .withValue(AppContract.Medias.COLUMN_PATH, mediaInfo.path)
                        .withValue(AppContract.Medias.COLUMN_FILTER, filter)
                        .withValue(AppContract.Medias.COLUMN_TYPE, mediaInfo.type)
                        .withValue(AppContract.Medias.COLUMN_DURATION, mediaInfo.duration)
                        .withValue(AppContract.Medias.COLUMN_TIME, mediaInfo.time)
                    mediaInfo.thumbnail?.let { b ->
                        cpo.withValue(
                            AppContract.Medias.COLUMN_THUMBNAIL,
                            b
                        )
                    }
                    mediaInfo.bitmap?.let { b ->
                        cpo.withValue(
                            AppContract.Medias.COLUMN_BITMAP,
                            b
                        )
                    }
                    ops.add(cpo.build())
                    count++
                } else if (!mapMediaInfo.equal(mediaInfo)) {
                    val cpo = ContentProviderOperation.newUpdate(AppContract.Medias.CONTENT_URI)
                        .withValue(AppContract.Medias.COLUMN_PATH, mediaInfo.path)
                        .withValue(AppContract.Medias.COLUMN_FILTER, filter)
                        .withValue(AppContract.Medias.COLUMN_TYPE, mediaInfo.type)
                        .withValue(AppContract.Medias.COLUMN_DURATION, mediaInfo.duration)
                        .withValue(AppContract.Medias.COLUMN_TIME, mediaInfo.time)
                        .withSelection(
                            "${AppContract.Medias.COLUMN_PATH}=?",
                            arrayOf(mediaInfo.path)
                        )
                    mediaInfo.thumbnail?.let { array ->
                        cpo.withValue(
                            AppContract.Medias.COLUMN_THUMBNAIL, array
                        )
                    }
                    mediaInfo.bitmap?.let { array ->
                        cpo.withValue(
                            AppContract.Medias.COLUMN_BITMAP,
                            array
                        )
                    }
                    ops.add(cpo.build())
                    count++
                }

                if (ops.size > 0 && ops.size % 100 == 0) {
                    AppContextUtil.sInstance!!.contentResolver.applyBatch(
                        AppContract.AUTHORITY,
                        ops
                    )
                    ops.clear()
                }
            }
        }

        if (ops.size > 0) {
            AppContextUtil.sInstance!!.contentResolver.applyBatch(AppContract.AUTHORITY, ops)
            ops.clear()
        }
    }

    @SuppressLint("Range")
    private fun getExistsPathMaps(): HashMap<String, MediaInfo> {
        val selection = null
        val selectionArgs = null
        val sortOrder = null
        val maps = hashMapOf<String, MediaInfo>()
        maps.clear()
        val cursor = AppContextUtil.sInstance!!.contentResolver.query(
            AppContract.Medias.CONTENT_URI, PROJECTION, selection, selectionArgs, sortOrder
        )
        cursor?.let { c ->
            if (c.moveToFirst()) {
                do {
                    val path = c.getString(c.getColumnIndex(AppContract.Medias.COLUMN_PATH))
                    val id = c.getInt(c.getColumnIndex(AppContract.Medias.COLUMN_ID))
                    val time = c.getInt(c.getColumnIndex(AppContract.Medias.COLUMN_TIME))
                    maps[path] = MediaInfo(id, path, time = time)
                } while (c.moveToNext())
            }
            c.close()
        }
        return maps
    }

    private fun MediaInfo.equal(mediaInfo: MediaInfo): Boolean {
        if (this.path == mediaInfo.path && this.time == mediaInfo.time) return true
        return false
    }

    fun insertMediaInfoByPath(path: String) {
        val mediaInfo = File(path).createMediaInfo()
        mediaInfo?.let { mi ->
            val uri = AppContextUtil.sInstance!!.contentResolver.insert(
                AppContract.Medias.CONTENT_URI, mi.createContentValues()
            )
        }
    }

    fun updateMediaInfoByPath(path: String) {
        val mediaInfo = File(path).createMediaInfo()
        mediaInfo?.let { mi ->
            val selection = "${AppContract.Medias.COLUMN_PATH} = ?"
            val selectionArgs = arrayOf("$path")
            val count = AppContextUtil.sInstance!!.contentResolver.update(
                AppContract.Medias.CONTENT_URI, mi.createContentValues(), selection, selectionArgs
            )
        }
    }

    fun deleteMediaInfoByPath(path: String) {
        val selection = "${AppContract.Medias.COLUMN_PATH} = ?"
        val selectionArgs = arrayOf("$path")
        val count = AppContextUtil.sInstance!!.contentResolver.delete(
            AppContract.Medias.CONTENT_URI, selection, selectionArgs
        )
    }
}
