package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import java.io.File

class SharePlugin {

    /**
     * 分享文本
     */
    private fun shareText(
        packageName: String?, className: String?, content: String?, title: String?, subject: String?,mmid: Mmid
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
        MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
    }

    /**
     * 分享网页
     */
    private fun shareUrl(
        packageName: String?, className: String?, content: String?, title: String?, subject: String?,mmid: Mmid
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
        MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
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
            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
        } else {
            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
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
            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
        } else {
            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 分享视频
     */
    private fun shareVideo(packageName: String?, className: String?, file: File, mmid: Mmid) {
        setIntent("video/*", packageName, className, file,mmid)
    }

    private fun setIntent(type: String?, packageName: String?, className: String?, file: File, mmid: Mmid) {
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
            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(chooserIntent)
        } else {
            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
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
            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(intent)
        } else {
            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "文件不存在", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 是否安装分享app
     * @param packageName
     */
    private fun checkInstall(packageName: String,mmid: Mmid): Boolean {
        return try {
            MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.packageManager?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            Toast.makeText(MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity, "请先安装应用app", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * 跳转官方安装网址
     */
    private fun toInstallWebView(url: String?,mmid: Mmid) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
        }
        MultiWebViewNMM.getCurrentWebViewController(mmid)?.activity?.startActivity(intent)
    }
    private fun stringCheck(str: String?): Boolean {
        return str?.isNotEmpty() ?: false
    }

}