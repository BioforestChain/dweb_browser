package info.bagen.rust.plaoc.database

import android.net.Uri

class AppContract {
    companion object {
        const val AUTHORITY = "info.bagen.rust.plaoc"
        const val PATH_PERMISSION = "permission"
        const val PATH_MEDIA = "media" // 用于存储图片信息
        const val PATH_APP_INFO = "appinfo" // 用于存储图片信息
    }

    object Permissions {
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_PERMISSION")

        const val CONTENT_TYPE = "vnd.android.cursor.dir/permission"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/permission"

        const val COLUMN_ID: String = "_id"
        const val COLUMN_APP_ID: String = "app_id"
        const val COLUMN_NAME: String = "permission"
        const val COLUMN_GRANT: String = "grant"
    }

    object Medias {
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_MEDIA")

        const val CONTENT_TYPE = "vnd.android.cursor.dir/media"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/media"

        const val COLUMN_ID: String = "_id"
        const val COLUMN_PATH: String = "path" // 存储文件路径
        const val COLUMN_FILTER: String = "filter" // 图片便签
        const val COLUMN_TYPE: String = "type" // 存储文件类型
        const val COLUMN_DURATION: String = "duration" // 存储文件更新时间 long
        const val COLUMN_TIME: String = "update_time" // 存储文件更新时间 long
        const val COLUMN_THUMBNAIL: String = "thumbnail" // 缩略图
        const val COLUMN_BITMAP: String = "bitmap" // 图片原始图
    }

    object AppInfo {
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_APP_INFO")

        const val CONTENT_TYPE = "vnd.android.cursor.dir/app_info"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/app_info"

        const val COLUMN_ID: String = "_id"
        const val COLUMN_MMID: String = "mmid"
        const val COLUMN_MAIN_URL: String = "main_url"
        const val COLUMN_META_DATA: String = "meta_data"
    }
}
