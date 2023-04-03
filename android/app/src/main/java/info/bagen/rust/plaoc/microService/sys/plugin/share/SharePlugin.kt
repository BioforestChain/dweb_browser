package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.BuildConfig
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import java.io.File

object SharePlugin {

    /**
     * 打开分享界面
     * @param title Set a title for any message. This will be the subject if sharing to email
     * @param text Set some text to share
     * @param url Set a URL to share, can be http, https or file:// URL
     * @param files Array of file:// URLs of the files to be shared. Only supported on iOS and Android.
     */
    fun share(
        controller: ShareController,
        title: String? = null,
        text: String? = null,
        url: String? = null,
        files: List<String>? = null,
        po: PromiseOut<String>,
    ) {
        if (text == null && url == null && (files == null || files.isEmpty())) {
            po.resolve("Must provide a URL or Message or files")
            return
        }
        if (url != null && !isFileUrl(url) && !isHttpUrl(url)) {
            po.resolve("Unsupported url")
            return
        }

        val intent = Intent().apply {
            action = if (files != null && files.isNotEmpty()) {
                Intent.ACTION_SEND_MULTIPLE
            } else {
                Intent.ACTION_SEND
            }

            if (text != null) {
                val sendText = if (url != null && isHttpUrl(url)) {
                    "$text $url"
                } else {
                    text
                }
                putExtra(Intent.EXTRA_TEXT, sendText)
                setTypeAndNormalize("text/plain")
            }

            if (url != null && isHttpUrl(url) && text == null) {
                putExtra(Intent.EXTRA_TEXT, url)
                setTypeAndNormalize("text/plain")
            } else if (url != null && isFileUrl(url)) {
                val filesArray = mutableListOf<String>()
                filesArray.add(url)
                shareFiles(filesArray, this, po)
            }

            title?.let { putExtra(Intent.EXTRA_SUBJECT, it) }

            if (files != null && files.isNotEmpty()) {
                shareFiles(files, this, po)
            }
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        val pi = PendingIntent.getBroadcast(
            App.appContext,
            0,
            Intent(Intent.EXTRA_CHOSEN_COMPONENT),
            flags
        )
        val chooserIntent = Intent.createChooser(intent, title, pi.intentSender).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        controller.let {
            it.shareLauncher?.launch(chooserIntent)
        }
    }

    private fun shareFiles(files: List<String>, intent: Intent, po: PromiseOut<String>) {
        val arrayListFiles = arrayListOf<Uri>()
        try {
            files.forEach { file ->
                if (isFileUrl(file)) {
                    var type = getMimeType(file)
                    if (type == null || files.size > 1) {
                        type = "*/*"
                    }
                    intent.type = type


                    val fileUrl = App.appContext.let {
                        println("path=> ${File(Uri.parse(file).path)}")
                        FileProvider.getUriForFile(
                            it,
                            "${BuildConfig.APPLICATION_ID}.file.opener.provider",
                            File(Uri.parse(file).path)
                        )
                    }
                    arrayListFiles.add(fileUrl!!)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && files.size == 1) {
                        intent.setDataAndType(fileUrl, type)
                        intent.putExtra(Intent.EXTRA_STREAM, fileUrl)
                    }
                } else {
                    debugShare("shareFiles", "only file urls are supported")
                    po.resolve("only file urls are supported")
                    return
                }
            }
            if (arrayListFiles.size > 1) {
                intent.putExtra(Intent.EXTRA_STREAM, arrayListFiles)
            }
            // 添加权限
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        } catch (e: Throwable) {
            po.resolve(e.message ?: "share file error")
        }
    }

    private fun isFileUrl(url: String): Boolean {
        return url.startsWith("file:")
    }

    private fun isHttpUrl(url: String): Boolean {
        return url.startsWith("http")
    }

    private fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }


    /**
     * 分享文本
     */
    private fun shareText(
        packageName: String?,
        className: String?,
        content: String?,
        title: String?,
        subject: String?,
        mmid: Mmid
    ) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"

            if (stringCheck(className) && stringCheck(packageName)) {
                val componentName = ComponentName(packageName!!, className!!)
                component = componentName
            } else if (stringCheck(packageName)) {
                setPackage(packageName)
            }

            content?.let { putExtra(Intent.EXTRA_TEXT, it) }
            title?.let { putExtra(Intent.EXTRA_TITLE, it) }
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        val chooserIntent = Intent.createChooser(intent, "分享到：")
//        MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
    }

    /**
     * 分享网页
     */
    private fun shareUrl(
        packageName: String?,
        className: String?,
        content: String?,
        title: String?,
        subject: String?,
        mmid: Mmid
    ) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"

            if (stringCheck(className) && stringCheck(packageName)) {
                val componentName = ComponentName(packageName!!, className!!)
                component = componentName
            } else if (stringCheck(packageName)) {
                setPackage(packageName)
            }

            content?.let { putExtra(Intent.EXTRA_TEXT, it) }
            title?.let { putExtra(Intent.EXTRA_TITLE, it) }
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        val chooserIntent = Intent.createChooser(intent, "分享到：")
//        MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
    }

    /**
     * 分享图片
     */
    private fun shareImg(packageName: String?, className: String?, file: File, mmid: Mmid) {
        if (file.exists()) {
            val uri: Uri = Uri.fromFile(file)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                if (stringCheck(packageName) && stringCheck(className)) {
                    component = ComponentName(packageName!!, className!!)
                } else if (stringCheck(packageName)) {
                    setPackage(packageName)
                }
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            val chooserIntent = Intent.createChooser(intent, "分享到:")
//            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
        } else {
//            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 分享音乐
     */
    private fun shareAudio(packageName: String?, className: String?, file: File, mmid: Mmid) {
        if (file.exists()) {
            val uri: Uri = Uri.fromFile(file)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "audio/*"
                if (stringCheck(packageName) && stringCheck(className)) {
                    component = ComponentName(packageName!!, className!!)
                } else if (stringCheck(packageName)) {
                    setPackage(packageName)
                }
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            val chooserIntent = Intent.createChooser(intent, "分享到:")
//            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
        } else {
//            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 分享视频
     */
    private fun shareVideo(packageName: String?, className: String?, file: File, mmid: Mmid) {
        setIntent("video/*", packageName, className, file, mmid)
    }

    private fun setIntent(
        type: String?,
        packageName: String?,
        className: String?,
        file: File,
        mmid: Mmid
    ) {
        if (file.exists()) {
            val uri: Uri = Uri.fromFile(file)
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = type
            if (stringCheck(packageName) && stringCheck(className)) {
                intent.component = ComponentName(packageName!!, className!!)
            } else if (stringCheck(packageName)) {
                intent.setPackage(packageName)
            }
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            val chooserIntent = Intent.createChooser(intent, "分享到:")
//            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
        } else {
//            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 分享多张图片和文字至朋友圈
     * @param title
     * @param packageName
     * @param className
     * @param file 图片文件
     */
    private fun shareImgToWXCircle(
        title: String?, packageName: String?, className: String?, file: File, mmid: Mmid
    ) {
        if (file.exists()) {
            val uri: Uri = Uri.fromFile(file)
            val intent = Intent()
            val comp = ComponentName(packageName!!, className!!)
            intent.component = comp
            intent.action = Intent.ACTION_SEND
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra("Kdescription", title)
//            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(intent)
        } else {
//            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 是否安装分享app
     * @param packageName
     */
    private fun checkInstall(packageName: String, mmid: Mmid): Boolean {
        return try {
//            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.packageManager?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
//            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "请先安装应用app", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * 跳转官方安装网址
     */
    private fun toInstallWebView(url: String?, mmid: Mmid) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
        }
//        MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(intent)
    }

    private fun stringCheck(str: String?): Boolean {
        return str?.isNotEmpty() ?: false
    }

}