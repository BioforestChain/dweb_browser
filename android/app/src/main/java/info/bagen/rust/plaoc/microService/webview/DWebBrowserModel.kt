package info.bagen.rust.plaoc.microService.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.rust.plaoc.App
import kotlinx.coroutines.launch

data class DWebBrowserUIState(
    val dWebBrowserList: MutableList<DWebBrowserItem> = mutableListOf(),
    var currentProcessId: String? = null
)

data class DWebBrowserItem(
    var show: MutableState<Boolean> = mutableStateOf(true),
    val url: String,
    val host: String,
    val dWebBrowser: DWebBrowser,
)

sealed class DWebBrowserIntent {
    object RemoveLast : DWebBrowserIntent() // 移除最后一项
    object RemoveALL : DWebBrowserIntent() // 移除最后一项\

    /**
     * @param origin 表示需要打开的地址
     * @param processId 表示当前分支号，类似夸克浏览器下面的新增按钮
     */
    class OpenDWebBrowser(val origin: String, val processId: String?) : DWebBrowserIntent()
}

class DWebBrowserModel : ViewModel() {
    val uiState = DWebBrowserUIState()
    private val dWebBrowserTree: HashMap<String, ArrayList<DWebBrowserItem>> = hashMapOf()
    private var mProcessId: Int = 0

    fun handleIntent(action: DWebBrowserIntent) {
        viewModelScope.launch {
            when (action) {
                is DWebBrowserIntent.OpenDWebBrowser -> {
                    Log.d(
                        "DWebBrowserModel",
                        "OpenDWebBrowser url=${action.origin},${action.processId}"
                    )
                    openDWebBrowser(action.origin, action.processId)
                }
                is DWebBrowserIntent.RemoveLast -> {
                    val temp = uiState.dWebBrowserList.removeLast()
                    temp.dWebBrowser.destroy()
                }
                is DWebBrowserIntent.RemoveALL -> {
                    uiState.dWebBrowserList.forEach { item ->
                        item.dWebBrowser.destroy()
                    }
                    uiState.dWebBrowserList.clear()
                }
            }
        }
    }

    fun openDWebBrowser(origin: String, processId: String?) : String {
        Uri.parse(origin)?.host?.let { host ->
            val dWebBrowser = DWebBrowserItem(
                url = origin, host = host, dWebBrowser = DWebBrowser(App.appContext, origin)
            )

            if (processId?.isNotEmpty() == true) {
                dWebBrowserTree[processId]?.let { list ->
                    list.add(dWebBrowser)
                    uiState.currentProcessId = processId
                    uiState.dWebBrowserList.clear()
                    uiState.dWebBrowserList.addAll(list) // 替换列表信息
                    return processId
                }
            } else if (uiState.currentProcessId?.isNotEmpty() == true) {
                dWebBrowserTree[uiState.currentProcessId!!]?.let { list ->
                    list.add(dWebBrowser)
                    uiState.dWebBrowserList.add(dWebBrowser)
                    return uiState.currentProcessId!!
                }
            }
            mProcessId = uiState.currentProcessId?.toInt() ?: mProcessId
            uiState.currentProcessId = mProcessId.toString()
            val list = arrayListOf<DWebBrowserItem>()
            dWebBrowserTree[mProcessId.toString()] = list
            uiState.dWebBrowserList.clear()
            uiState.dWebBrowserList.addAll(list)
            return mProcessId.toString()
        }
        return "0"
    }
}