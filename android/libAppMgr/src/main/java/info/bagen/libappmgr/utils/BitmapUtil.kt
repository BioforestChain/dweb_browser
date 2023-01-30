package info.bagen.libappmgr.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Video.Thumbnails.MINI_KIND

object BitmapUtil {

    /**
     * 获取手机里视频缩略图
     */
    @SuppressLint("Range")
    fun getVideoThumbnail(cr: ContentResolver, uri: Uri): Bitmap? {
        var options = BitmapFactory.Options()
        options.inDither = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        var cursor = cr.query(uri, arrayOf(MediaStore.Video.Media._ID), null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                var videoId =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID)) //image id in image table.s
                return MediaStore.Video.Thumbnails.getThumbnail(
                    cr, videoId.toLong(),
                    MediaStore.Images.Thumbnails.MICRO_KIND, options
                )
            }
            cursor.close()
            cursor = null
        }
        return null
    }

    /**
     * 根据指定的图像路径和大小来获取缩略图
     * 此方法有两点好处：
     * 1. 使用较小的内存空间，第一次获取的bitmap实际上为null，只是为了读取宽度和高度，
     * 第二次读取的bitmap是根据比例压缩过的图像，第三次读取的bitmap是所要的缩略图。
     * 2. 缩略图对于原图像来讲没有拉伸，这里使用了2.2版本的新工具ThumbnailUtils，使
     * 用这个工具生成的图像不会被拉伸。
     * @param imagePath 图像的路径
     * @param width 指定输出图像的宽度
     * @param height 指定输出图像的高度
     * @return 生成的缩略图
     */
    fun getImageThumbnail(imagePath: String, width: Int, height: Int): Bitmap? {
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        // 获取这个图片的宽和高，注意此处的bitmap为null
        BitmapFactory.decodeFile(imagePath, options)
        options.inJustDecodeBounds = false // 设置为false
        // 计算缩放比
        var w = options.outWidth
        var h = options.outHeight
        var beWidth = w / width
        var beHeight = h / height
        var be = if (beWidth < beHeight) {
            beWidth
        } else {
            beHeight
        }
        if (be <= 0) be = 1
        options.inSampleSize = be
        // 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
        var bitmap = BitmapFactory.decodeFile(imagePath, options)
        // 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
        bitmap =
            ThumbnailUtils.extractThumbnail(
                bitmap,
                width,
                height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT
            )
        return bitmap
    }

    /**
     * 获取视频的缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     * @param videoPath 视频的路径
     * @param width 指定输出视频缩略图的宽度
     * @param height 指定输出视频缩略图的高度度
     * @param kind 参照MediaStore.Images.Thumbnails类中的常量MINI_KIND和MICRO_KIND。
     * 其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
     * @return 指定大小的视频缩略图
     */
    fun getVideoThumbnail(
        videoPath: String, width: Int, height: Int, kind: Int = MINI_KIND
    ): Bitmap? {
        // 获取视频的缩略图
        return ThumbnailUtils.extractThumbnail(
            ThumbnailUtils.createVideoThumbnail(videoPath, kind),
            width,
            height,
            ThumbnailUtils.OPTIONS_RECYCLE_INPUT
        )
    }

    /**
     * 获取视频的缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     * @param videoPath 视频的路径
     * @param size 指定输出视频缩略图的宽度和高度
     * @param kind 参照MediaStore.Images.Thumbnails类中的常量MINI_KIND和MICRO_KIND。
     * 其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
     * @return 指定大小的视频缩略图
     */
    fun getVideoThumbnail(videoPath: String, size: Int, kind: Int = MINI_KIND): Bitmap? {
        // 获取视频的缩略图
        return ThumbnailUtils.extractThumbnail(
            ThumbnailUtils.createVideoThumbnail(videoPath, kind),
            size,
            size,
            ThumbnailUtils.OPTIONS_RECYCLE_INPUT
        )
    }
}
