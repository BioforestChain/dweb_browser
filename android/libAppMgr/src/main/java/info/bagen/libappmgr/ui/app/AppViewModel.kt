package info.bagen.libappmgr.ui.app

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.libappmgr.entity.AppInfo
import info.bagen.libappmgr.entity.AppVersion
import info.bagen.libappmgr.entity.DAppInfoUI
import info.bagen.libappmgr.ui.download.DownLoadIntent
import info.bagen.libappmgr.ui.download.DownLoadState
import info.bagen.libappmgr.ui.download.DownLoadViewModel
import info.bagen.libappmgr.ui.view.DialogInfo
import info.bagen.libappmgr.ui.view.DialogType
import info.bagen.libappmgr.utils.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AppViewUIState(
    val useMaskView: MutableState<Boolean> = mutableStateOf(true), // 表示使用app遮罩的方式下载
    val appDialogInfo: MutableState<AppDialogInfo> = mutableStateOf(AppDialogInfo()),
    val appViewStateList: MutableList<AppViewState> = mutableStateListOf(),
    // val downloadAppList: MutableList<AppViewState> = mutableStateListOf(),
    var curAppViewState: AppViewState? = null,
)

data class AppViewState(
    val showBadge: MutableState<Boolean> = mutableStateOf(false),
    val showPopView: MutableState<Boolean> = mutableStateOf(false),
    val iconPath: MutableState<String> = mutableStateOf(""),
    val name: MutableState<String> = mutableStateOf(""),
    val maskViewState: MaskProgressState = MaskProgressState(),
    var appInfo: AppInfo? = null, // 为了下载，考虑下载是没有这个信息
    var dAppInfoUI: DAppInfoUI? = null,
    var bfsDownloadPath: String? = null,
)

data class MaskProgressState(
    val show: MutableState<Boolean> = mutableStateOf(false),
    val downLoadState: MutableState<DownLoadState> = mutableStateOf(DownLoadState.IDLE),
    var path: String = "",
    var downLoadViewModel: DownLoadViewModel = DownLoadViewModel()
)

private fun AppInfo.createAppViewState(
    showBadge: Boolean? = null,
    iconPath: String? = null,
    name: String? = null,
): AppViewState {
    return AppViewState(
        showBadge = showBadge?.let { mutableStateOf(it) } ?: mutableStateOf(false),
        iconPath = iconPath?.let { mutableStateOf(it) } ?: mutableStateOf(this.iconPath),
        name = name?.let { mutableStateOf(it) } ?: mutableStateOf(this.name),
        appInfo = this,
    )
}

enum class AppDialogType {
    NewVersion, ReDownLoad, DownLoading, DownLoadCompleted, OpenDApp, Other
}

enum class NewAppUnzipType {
    INSTALL, OVERRIDE, LOW_VERSION
}

data class AppDialogInfo(
    val dialogInfo: DialogInfo = DialogInfo(),
    val lastType: AppDialogType = AppDialogType.Other,
    val data: Any? = null
)

sealed class AppViewIntent {
    object LoadAppInfoList : AppViewIntent()
    class LoadAppNewVersion(val appViewState: AppViewState) : AppViewIntent()

    class MaskDownloadCallback(
        val downLoadState: DownLoadState, val dialogInfo: DialogInfo, val appViewState: AppViewState
    ) : AppViewIntent()

    class ShowAppViewBadge(val appViewState: AppViewState, val show: Boolean) : AppViewIntent()

    class DialogDownloadCallback(
        val downLoadState: DownLoadState, val dialogInfo: DialogInfo, val appViewState: AppViewState
    ) : AppViewIntent()

    object DialogHide : AppViewIntent()
    object DialogConfirm : AppViewIntent()

    class PopAppMenu(val appViewState: AppViewState, val show: Boolean) : AppViewIntent()
    class ShareAppMenu(val appViewState: AppViewState) : AppViewIntent()
    class UninstallApp(val appViewState: AppViewState) : AppViewIntent()
    class BFSInstallApp(val path: String) : AppViewIntent()
    class RemoveDownloadApp(val appViewState: AppViewState) : AppViewIntent()
    class OverrideDownloadApp(
        val appViewState: AppViewState, val appInfo: AppInfo, val downloadFile: String
    ) : AppViewIntent()

    class UpdateDownloadApp(
        val appViewState: AppViewState, val appInfo: AppInfo, val downloadFile: String
    ) : AppViewIntent()
}


class AppViewModel(private val repository: AppRepository = AppRepository()) : ViewModel() {
    // val channel = Channel<DownLoadIntent>(2) // 表示最多两个buffer，超过后挂起
    val uiState = AppViewUIState()

    /*init { // 这边是初始化的时候执行的内容
      viewModelScope.launch {
        channel.consumeAsFlow().collect { TODO("这功能是持续监听channel信道的内容，有收到就做处理") }
      }
    }*/

    fun handleIntent(action: AppViewIntent) {
        viewModelScope.launch {
            /* channel.send(action) // 进行发送操作，可以根据传参进行发送 */
            when (action) {
                is AppViewIntent.LoadAppInfoList -> loadAppInfoList()
                is AppViewIntent.LoadAppNewVersion -> loadAppNewVersion(action.appViewState)
                is AppViewIntent.MaskDownloadCallback -> maskDownloadCallback(
                    action.downLoadState, action.dialogInfo, action.appViewState
                )
                is AppViewIntent.ShowAppViewBadge -> {
                    action.appViewState.showBadge.value = action.show
                }
                is AppViewIntent.DialogHide -> {
                    uiState.appDialogInfo.value = uiState.appDialogInfo.value.copy(
                        dialogInfo = DialogInfo(type = DialogType.HIDE)
                    )
                }
                is AppViewIntent.DialogDownloadCallback -> dialogDownloadCallback(
                    action.downLoadState, action.dialogInfo, action.appViewState
                )
                is AppViewIntent.DialogConfirm -> dialogConfirm()
                is AppViewIntent.PopAppMenu -> {
                    action.appViewState.showPopView.value = action.show
                }
                is AppViewIntent.ShareAppMenu -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TITLE, action.appViewState.name.value)
                        putExtra(
                            Intent.EXTRA_TEXT,
                            action.appViewState.appInfo?.autoUpdate?.url ?: ""
                        )
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    AppContextUtil.sInstance?.startActivity(shareIntent)
                }
                is AppViewIntent.UninstallApp -> {
                    // 卸载就是删除当前目录，然后重新捞取
                    uiState.curAppViewState = null
                    action.appViewState.showBadge.value = false
                    val path = FilesUtil.getAppRootDirectory(action.appViewState.appInfo!!)
                    FilesUtil.deleteQuietly(path)
                    action.appViewState.appInfo?.let { appInfo ->
                        if (appInfo.isRecommendApp) {
                            appInfo.appDirType = APP_DIR_TYPE.RecommendApp
                            appInfo.dAppUrl = null
                        } else {
                            uiState.appViewStateList.remove(action.appViewState)
                        }
                    }
                }
                is AppViewIntent.BFSInstallApp -> { // 通过 JS 调用的下载操作
                    if (action.path.isNotEmpty()) {
                        newAppDownloadAndInstall(action.path)
                    }
                }
                is AppViewIntent.RemoveDownloadApp -> { // 删除当前的下载的应用，弹出提示框，版本太低
                    uiState.appViewStateList.remove(action.appViewState)
                    val dialogInfo = DialogInfo(
                        DialogType.CUSTOM,
                        title = "异常提示",
                        text = "安装失败! 下载的应用版本太低，无法安装!",
                        confirmText = "重新下载"
                    )
                    handleIntent(
                        AppViewIntent.MaskDownloadCallback(
                            DownLoadState.FAILURE, dialogInfo, action.appViewState
                        )
                    )
                }
                is AppViewIntent.OverrideDownloadApp -> { // 解压并且跟已存在的app绑定，显示小红点，最后移除下载
                    overrideDownloadApp(action.appViewState, action.appInfo, action.downloadFile)
                }
                is AppViewIntent.UpdateDownloadApp -> { // 如果当前下载app是正常的app，那么就需要自动解压，并且
                    action.appViewState.appInfo = action.appInfo
                }
            }
        }
    }

    private suspend fun loadAppInfoList() {
        val list = arrayListOf<AppViewState>()
        repository.loadAppInfoList().forEach { appInfo ->
            val appViewState = appInfo.createAppViewState()
            appViewState.dAppInfoUI = repository.loadDAppUrl(appInfo) // 补充跳转到地址
            list.add(appViewState)
        }
        uiState.appViewStateList.clear()
        uiState.appViewStateList.addAll(list)
    }

    private suspend fun loadAppNewVersion(appViewState: AppViewState) {
        uiState.curAppViewState = appViewState
        // 调用下载接口，然后弹出对话框
        appViewState.appInfo?.apply {
            FilesUtil.getLastUpdateContent(this)?.let { path ->
                JsonUtil.fromJson(AppVersion::class.java, path)?.let { appVersion ->
                    dialogShowNewVersion(appVersion)
                }
            }
            autoUpdate?.let { autoUpdateInfo ->
                repository.loadAppNewVersion(autoUpdateInfo.url).collectLatest { appVersion ->
                    dialogShowNewVersion(appVersion)
                }
            }
        }
    }

    private suspend fun dialogShowNewVersion(appVersion: AppVersion?) {
        val url = appVersion?.files?.get(0)?.url ?: ""
        uiState.appDialogInfo.value = uiState.appDialogInfo.value.copy(
            dialogInfo = DialogInfo(
                type = DialogType.CUSTOM,
                title = "版本更新",
                confirmText = "下载",
                cancelText = "取消",
                text = "版本信息：${appVersion?.version}\n更新内容：${appVersion?.releaseNotes}"
            ), lastType = AppDialogType.NewVersion, data = url
        )
        uiState.curAppViewState?.let {
            it.maskViewState.path = url
        }
    }

    private suspend fun dialogConfirm() {
        when (uiState.appDialogInfo.value.lastType) {
            AppDialogType.NewVersion, AppDialogType.ReDownLoad -> {
                if (uiState.useMaskView.value) { // 在app上面显示遮罩
                    uiState.curAppViewState?.let {
                        it.maskViewState.show.value = true
                        it.maskViewState.downLoadViewModel.handleIntent(
                            DownLoadIntent.LoadDownLoadStateAndDownLoad(it.maskViewState.path)
                        )
                    }
                    uiState.appDialogInfo.value = uiState.appDialogInfo.value.copy(
                        dialogInfo = DialogInfo(DialogType.HIDE)
                    )
                } else { // 打开下载对话框
                    uiState.appDialogInfo.value = uiState.appDialogInfo.value.copy(
                        lastType = AppDialogType.DownLoading
                    )
                }
            }
            AppDialogType.DownLoadCompleted -> {
                uiState.appDialogInfo.value = uiState.appDialogInfo.value.copy(
                    lastType = AppDialogType.OpenDApp
                )
            }
            else -> {}
        }
    }

    private suspend fun dialogDownloadCallback(
        downLoadState: DownLoadState, dialogInfo: DialogInfo, appViewState: AppViewState
    ) {
        val appDialogType: AppDialogType = when (downLoadState) {
            DownLoadState.COMPLETED -> {
                appViewState.showBadge.value = true
                appViewState.appInfo?.let { appInfo ->
                    repository.loadDAppUrl(appInfo)?.let { dAppInfoUI -> // 跳转需要的地址
                        appViewState.dAppInfoUI = dAppInfoUI
                        appViewState.iconPath.value = dAppInfoUI.icon
                    }
                }
                AppDialogType.DownLoadCompleted
            }
            DownLoadState.FAILURE -> AppDialogType.ReDownLoad
            else -> AppDialogType.Other
        }
        uiState.appDialogInfo.value = uiState.appDialogInfo.value.copy(
            dialogInfo = dialogInfo, lastType = appDialogType
        )
    }

    private suspend fun maskDownloadCallback(
        downLoadState: DownLoadState, dialogInfo: DialogInfo, appViewState: AppViewState
    ) {
        when (downLoadState) {
            DownLoadState.COMPLETED -> {
                appViewState.showBadge.value = true
                appViewState.appInfo?.apply {
                    appDirType = APP_DIR_TYPE.SystemApp // 需要提前将路径改为 system-app
                    repository.loadDAppUrl(this)?.let { dAppInfoUI -> // 跳转需要的地址
                        appViewState.dAppInfoUI = dAppInfoUI
                        appViewState.name.value = dAppInfoUI.name
                        appViewState.iconPath.value = dAppInfoUI.icon
                        appViewState.appInfo!!.dAppUrl = dAppInfoUI.dAppUrl
                    }
                }
            }
            DownLoadState.FAILURE -> {
                Toast.makeText(AppContextUtil.sInstance!!, dialogInfo.text, Toast.LENGTH_SHORT)
                    .show()
            }
            DownLoadState.CLOSE -> {
                appViewState.maskViewState.show.value = false
            }
            else -> {}
        }
        appViewState.maskViewState.downLoadState.value = downLoadState
    }

    private suspend fun newAppDownloadAndInstall(path: String) {
        // 添加新应用， 需要判断当前应用是否已存在
        var found = false
        uiState.appViewStateList.forEach {
            if (it.maskViewState.path == path) {
                found = true
            }
        }
        if (!found) {
            AppContextUtil.showShortToastMessage("开始下载")
            val appViewState = AppViewState(
                name = mutableStateOf("开始下载"),
                bfsDownloadPath = path,
                maskViewState = MaskProgressState(
                    show = mutableStateOf(true),
                    downLoadState = mutableStateOf(DownLoadState.LOADING),
                    path = path,
                    downLoadViewModel = DownLoadViewModel().apply {
                        handleIntent(DownLoadIntent.LoadDownLoadStateAndDownLoad(path)) // 直接下载
                    })
            )
            uiState.appViewStateList.add(appViewState)
            uiState.curAppViewState = appViewState
        } else {
            AppContextUtil.showShortToastMessage("正在下载中，不需要重新下载...")
        }
    }

    private fun overrideDownloadApp(
        appViewState: AppViewState, appInfo: AppInfo, downloadFile: String
    ) {
        uiState.appViewStateList.remove(appViewState)
        val unzip = ZipUtil.decompress(downloadFile, FilesUtil.getAppUnzipPath())
        if (unzip) {
            uiState.appViewStateList.forEach { item ->
                if (item.appInfo?.bfsAppId == appInfo.bfsAppId) {
                    handleIntent(
                        AppViewIntent.MaskDownloadCallback(
                            DownLoadState.COMPLETED,
                            DialogInfo(),
                            item
                        )
                    )
                }
            }
        } else {
            uiState.appViewStateList.forEach { item ->
                if (item.appInfo?.bfsAppId == appInfo.bfsAppId) {
                    val dialogInfo = DialogInfo(
                        DialogType.CUSTOM,
                        title = "异常提示",
                        text = "安装失败! 下载的应用无法正常安装，请联系管理员!",
                        confirmText = "重新下载"
                    )
                    handleIntent(
                        AppViewIntent.MaskDownloadCallback(DownLoadState.FAILURE, dialogInfo, item)
                    )
                }
            }
        }
    }
}
