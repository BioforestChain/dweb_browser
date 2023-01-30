package info.bagen.libappmgr.ui.main

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.libappmgr.R
import info.bagen.libappmgr.entity.AppInfo
import info.bagen.libappmgr.entity.AppVersion
import info.bagen.libappmgr.network.ApiService
import info.bagen.libappmgr.network.base.ApiResultData
import info.bagen.libappmgr.network.base.IApiResult
import info.bagen.libappmgr.network.base.fold
import info.bagen.libappmgr.utils.FilesUtil
import info.bagen.libappmgr.utils.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

enum class SearchAction {
    Search, OpenCamera
}

sealed class RouteScreen(
    val route: String,
    @StringRes val resourceId: Int,
    val image: ImageVector
) {
    object Home : RouteScreen("Home", R.string.navitem_home, Icons.Filled.Home)
    object Contact : RouteScreen("Contact", R.string.navitem_contact, Icons.Filled.Call)
    object Message : RouteScreen("Message", R.string.navitem_message, Icons.Filled.Email)
    object Me : RouteScreen("Me", R.string.navitem_me, Icons.Filled.AccountBox)
}

class MainViewModel : ViewModel() {
    val navList = listOf(
        RouteScreen.Home,
        RouteScreen.Contact,
        RouteScreen.Message,
        RouteScreen.Me
    )

    fun getAppVersionAndSave(
        appInfo: AppInfo, apiResult: IApiResult<AppVersion>? = null
    ) {

        viewModelScope.launch {
            flow {
                emit(ApiResultData.prepare())
                try {
                    emit(ApiService.instance.getAppVersion(appInfo.autoUpdate?.url ?: ""))
                } catch (e: Exception) {
                    emit(ApiResultData.failure(e))
                }
            }.flowOn(Dispatchers.IO).collect {
                it.fold(onSuccess = { baseData ->
                    baseData.data?.let { appVersion ->
                        // 将改内容存储到 recommend-app/bfs-id-app/tmp/autoUpdate 中
                        FilesUtil.writeFileContent(
                            FilesUtil.getAppVersionSaveFile(appInfo), JsonUtil.toJson(appVersion)
                        )
                        apiResult?.onSuccess(
                            baseData.errorCode, baseData.errorMsg, baseData.data
                        )
                    }
                }, onFailure = { e ->
                    Log.d("MainViewModel", "fail->$e")
                    e?.printStackTrace()
                    apiResult?.onError(-1, "fail", e)
                }, onLoading = {}, onPrepare = {})
            }
        }
    }

}
