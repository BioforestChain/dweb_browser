package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.BuildConfig
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.sys.plugin.fileSystem.EFileDirectory
import info.bagen.rust.plaoc.microService.sys.plugin.share.ShareActivity.Companion.RESULT_SHARE_CODE
import info.bagen.rust.plaoc.microService.sys.plugin.share.ShareController.Companion.controller
import org.http4k.core.Method
import org.http4k.core.MultipartFormBody
import org.http4k.lens.Query
import org.http4k.lens.composite
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.io.File


data class ShareOptions(
    val title: String?,
    val text: String?,
    val url: String?,
)

fun debugShare(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Share", tag, msg, err)

class ShareNMM : NativeMicroModule("share.sys.dweb") {

    private val plugin = CacheFilePlugin()

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val shareOption = Query.composite { spec ->
            ShareOptions(
                title = string().optional("title")(spec),
                text = string().optional("text")(spec),
                url = string().optional("url")(spec)
            )
        }
        apiRouting = routes(
            /** 分享*/
            "/share" bind Method.POST to defineHandler { request, ipc ->
                val ext = shareOption(request)
                val receivedForm = MultipartFormBody.from(request)
                val fileByteArray = receivedForm.files("files")
                // 启动activity
                controller.startShareActivity()
                controller.waitActivityCreated()
                val files = mutableListOf<String>()
                val result = PromiseOut<String>()
                // 写入缓存
                fileByteArray.map { file ->
                    val url = plugin.writeFile(
                        file.filename,
                        EFileDirectory.Cache.location,
                        file.content,
                        false
                    )
                    files.add(url)
                }
                debugShare("open_share", "share===>${ipc.remote.mmid}  ${files}")

                share(ipc.remote.mmid, ext.title, ext.text, ext.url, files, result)
                // 等待结果回调
                controller.activity?.getShareData { it ->
                    debugShare("share", "result => $it")
                    result.resolve(it)
                }

                return@defineHandler result.waitPromise()
            },
        )
    }


    /**
     * 打开分享界面
     * @param title Set a title for any message. This will be the subject if sharing to email
     * @param text Set some text to share
     * @param url Set a URL to share, can be http, https or file:// URL
     * @param files Array of file:// URLs of the files to be shared. Only supported on iOS and Android.
     */
    fun share(
        mmid: Mmid,
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
                shareFiles(mmid, filesArray, this, po)
            }

            title?.let { putExtra(Intent.EXTRA_SUBJECT, it) }

            if (files != null && files.isNotEmpty()) {
                shareFiles(mmid, files, this, po)
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

        controller.activity?.startActivityForResult(
            chooserIntent,
            RESULT_SHARE_CODE
        )
    }

    private fun shareFiles(
        mmid: Mmid, files: List<String>, intent: Intent, po: PromiseOut<String>
    ) {
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


    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}
