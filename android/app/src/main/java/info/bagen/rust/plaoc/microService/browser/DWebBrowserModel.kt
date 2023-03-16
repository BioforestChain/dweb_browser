package info.bagen.rust.plaoc.microService.browser

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.network.HttpClient
import info.bagen.rust.plaoc.network.base.byteBufferToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class DWebBrowserUIState(
    val show: MutableState<Boolean> = mutableStateOf(false),
    val dWebBrowserList: MutableList<DWebBrowserItem> = mutableStateListOf(),
    var currentProcessId: String? = null
)

data class DWebBrowserItem(
    var show: MutableState<Boolean> = mutableStateOf(false),
    val url: String,
    val host: String,
    val dWebBrowser: DWebBrowser,
)

sealed class DWebBrowserIntent {
    object RemoveLast : DWebBrowserIntent() // 移除最后一项
    object RemoveALL : DWebBrowserIntent() // 移除最后一项

    /**
     * @param origin 表示需要打开的地址
     * @param processId 表示当前分支号，类似夸克浏览器下面的新增按钮
     */
    class OpenDWebBrowser(val origin: String, val processId: String? = null) : DWebBrowserIntent()


}

class DWebBrowserModel : ViewModel() {
    val uiState = DWebBrowserUIState()
    private val dWebBrowserTree: HashMap<String, ArrayList<DWebBrowserItem>> = hashMapOf()

    class DWebBrowser(val viewId: String, val instance: DWebBrowserItem)

    private val dWebBrowserList = listOf<DWebBrowser>()

    fun handleIntent(action: DWebBrowserIntent) {
        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is DWebBrowserIntent.OpenDWebBrowser -> {
                    openDWebBrowser(action.origin, action.processId)
                }
                is DWebBrowserIntent.RemoveLast -> {
                    val last = uiState.dWebBrowserList.last()
                    if (last.dWebBrowser.canGoBack()) {
                        last.dWebBrowser.goBack()
                    } else {
                        if (uiState.dWebBrowserList.size <= 1) {
                            uiState.show.value = false
                            delay(500)
                        } else {
                            last.show.value = false
                            delay(500)
                        }
                        uiState.currentProcessId?.let { processId ->
                            dWebBrowserTree[processId]?.removeLast()
                        }
                        uiState.dWebBrowserList.removeLast()
                        last.dWebBrowser.destroy()
                    }
                }
                is DWebBrowserIntent.RemoveALL -> {
                    uiState.dWebBrowserList.forEach { item ->
                        item.dWebBrowser.destroy()
                    }
                    uiState.dWebBrowserList.clear()
                    dWebBrowserTree.forEach { (_, data) ->
                        data.forEach { it.dWebBrowser.destroy() }
                    }
                    dWebBrowserTree.clear()
                }
            }
        }
    }

    fun openDWebBrowser(origin: String, processId: String? = null): String {
        // 先判断下是否是json结尾，如果是并获取解析json为jmmMetadata，失败就照常打开网页，成功打开下载界面
        if (checkJmmMetadataJson(origin) { jmmMetadata, url ->
                JmmNMM.nativeFetchInstallApp(jmmMetadata, url)
            }) return "0"

        // 先产生 processId 返回值，然后再执行界面，否则在 Main 执行无法获取返回值
        val ret = Uri.parse(origin)?.host?.let { host ->
            var dWebBrowserItem: DWebBrowserItem
            runBlocking(Dispatchers.Main) {
                dWebBrowserItem = DWebBrowserItem(
                    url = origin, host = host, dWebBrowser = DWebBrowser(App.appContext, origin)
                )
            }
            val retValue: String
            val list = if (processId?.isNotEmpty() == true) {
                retValue = processId
                if (dWebBrowserTree[processId] != null) {
                    dWebBrowserTree[processId]!!.apply { add(dWebBrowserItem) }
                } else {
                    arrayListOf<DWebBrowserItem>().apply { add(dWebBrowserItem) }
                }
            } else if (uiState.currentProcessId?.isNotEmpty() == true &&
                dWebBrowserTree[uiState.currentProcessId!!] != null
            ) {
                retValue = uiState.currentProcessId!!
                dWebBrowserTree[uiState.currentProcessId!!]!!.apply { add(dWebBrowserItem) }
            } else {
                retValue = processId ?: uiState.currentProcessId ?: "0"
                arrayListOf<DWebBrowserItem>().apply { add(dWebBrowserItem) }
            }
            showDWebBrowser(retValue, list, dWebBrowserItem)
            retValue
        } ?: "0"
        return ret
    }

    private fun checkJmmMetadataJson(
        url: String, openJmmActivity: (JmmMetadata, String) -> Unit
    ): Boolean {
        Uri.parse(url).lastPathSegment?.let { lastPathSegment ->
            if (lastPathSegment.endsWith(".json")) { // 如果是json，进行请求判断并解析jmmMetadata
                try {
                    gson.fromJson(
                        byteBufferToString(HttpClient().requestPath(url).body.payload),
                        JmmMetadata::class.java
                    ).apply { openJmmActivity(this, url) }

                    return true
                } catch (e: JsonSyntaxException) {
                    Log.e("DWebBrowserModel", "checkJmmMetadataJson fail -> ${e.message}")
                }
            }
        }
        return false
    }

    private fun showDWebBrowser(
        processId: String, list: ArrayList<DWebBrowserItem>, dWebBrowserItem: DWebBrowserItem
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            if (processId == uiState.currentProcessId) {
                if (!uiState.show.value) { // 如果是加载DWebBrowser的话，那么就不加载WebView动画
                    uiState.show.value = true
                    dWebBrowserItem.show.value = true
                }
                uiState.dWebBrowserList.add(dWebBrowserItem)
            } else {
                if (uiState.show.value) { // 如果当前是显示状态，那么需要先隐藏再显示
                    uiState.show.value = false
                    delay(500)
                    uiState.show.value = true
                    dWebBrowserItem.show.value = true
                } else {
                    uiState.show.value = true
                    dWebBrowserItem.show.value = true
                }
                uiState.currentProcessId = processId
                uiState.dWebBrowserList.clear()
                uiState.dWebBrowserList.addAll(list) // 替换列表信息
                if (!dWebBrowserTree.containsKey(processId)) {
                    dWebBrowserTree[processId] = list
                }
            }
            if (!dWebBrowserItem.show.value) { // 为了显示新增的WebView动画
                delay(500)
                dWebBrowserItem.show.value = true
            }
        }
    }
}
