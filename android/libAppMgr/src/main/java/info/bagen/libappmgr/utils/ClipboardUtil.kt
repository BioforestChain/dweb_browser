package info.bagen.libappmgr.utils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import info.bagen.libappmgr.entity.AppVersion
import info.bagen.libappmgr.network.ApiService
import info.bagen.libappmgr.network.base.BaseData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ClipboardUtil {
    private var currentDate: String = ""

    // 写内容到剪切板
    @SuppressLint("ServiceCast")
    fun writeToClipboard(context: Context, content: String, label: String = "OcrText") {
        // 获取剪贴板管理器
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // 创建普通字符型ClipData
        val clipData = ClipData.newPlainText(label, content)
        // 将ClipData内容放到系统剪贴板里。
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(context, "复制成功!", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ServiceCast")
    fun readFromClipboard(context: Context): String? {
        // 获取剪贴板管理器
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // 获取剪贴板的剪贴数据集
        val clipData = clipboardManager.primaryClip;

        if (clipData != null && clipData!!.itemCount > 0 && clipData!!.getItemAt(0).text != null) {
            return clipData!!.getItemAt(0).text.toString()
        }
        return null
    }

    suspend fun readAndParsingClipboard(context: Context) {
        val content = readFromClipboard(context)
        if (content == currentDate) {
            return
        } else if (content != null && content.startsWith("http")) {
            currentDate = content
            // 网络请求最新版本
            withContext(Dispatchers.IO) {
                val apiResultData = ApiService.instance.getAppVersion(content)
                if (apiResultData.isSuccess) {
                    val bb = apiResultData.value as BaseData<AppVersion>
                    Log.d("lin.huang", "readAndParsingClipboard -> ${bb.data?.version}")
                } else {
                    Log.d("lin.huang", "readAndParsingClipboard -> load fail->$apiResultData")
                }
            }
        }
    }
}
